/*
 * Copyright 2020 Daniel Scherf & Michael Rittmeister & Julian König
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.seliba.devcordbot.core

import com.codewaves.codehighlight.core.Highlighter
import com.codewaves.codehighlight.core.StyleRendererFactory
import com.codewaves.codehighlight.renderer.HtmlRenderer
import com.github.seliba.devcordbot.constants.Constants
import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.constants.Emotes
import com.github.seliba.devcordbot.database.Tag
import com.github.seliba.devcordbot.database.Tags
import com.github.seliba.devcordbot.dsl.EmbedConvention
import com.github.seliba.devcordbot.dsl.editMessage
import com.github.seliba.devcordbot.dsl.sendMessage
import com.github.seliba.devcordbot.event.EventSubscriber
import com.github.seliba.devcordbot.util.HastebinUtil
import com.github.seliba.devcordbot.util.await
import com.github.seliba.devcordbot.util.executeAsync
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import okhttp3.Request
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture

private const val MAX_LINES = 15

/**
 * Automatic analzyer for common pitfalls.
 */
class CommonPitfallListener(
    private val bot: DevCordBot,
    private val whitelist: List<String>,
    private val blacklist: List<String>,
    knownLanguages: List<String>
) {
    private val languageGuesser = Highlighter(UselessRendererFactoryThing())
    private val knownLanguages = knownLanguages.toTypedArray()

    /**
     * Listens for new messages.
     */
    @EventSubscriber
    suspend fun onMessage(event: GuildMessageReceivedEvent) {
        val input = event.message.contentRaw
        if (event.author.isBot ||
            (!bot.debugMode && (event.channel.parent?.id !in whitelist ||
                    event.channel.id in blacklist)) ||
            Constants.prefix.containsMatchIn(
                input
            )
        ) return

        // Search for all kinds of sources that show common pitfalls (except github gist since I also need to do stuff for my job to earn money lol)
        val hastebinMatches = HASTEBIN_PATTERN.findAll(input)
        val pastebinMatches = PASTEBIN_PATTERN.findAll(input)
        val attachments = event.message.attachments

        // Fetch value of sources
        val hastebinInputs = hastebinMatches.map {
            runBlocking {
                fetchHastebin(it)
            }
        }
        val pastebinInputs = pastebinMatches.map {
            runBlocking {
                fetchPastebin(it)
            }
        }

        // Collect remote sources (BEST PRACTICE!)
        val remoteInputs = (hastebinInputs + pastebinInputs).toList()
        val attachmentInputs = attachments.map {
            fetchAttachment(it)
        }

        // If there is no paste source analyze the input directly
        if (remoteInputs.isEmpty() and attachmentInputs.isEmpty()) {
            analyzeInput(input, false, event)
            return
        }

        // ragequit on first match
        val remotePassed = remoteInputs.any {
            analyzeInput(it, true, event)
        }

        // fallback to attachments
        if (!remotePassed) {
            // ragequit on first match
            attachmentInputs.firstOrNull {
                analyzeInput(it, false, event)
            }
        }
    }

    private suspend fun fetchAttachment(attachment: Message.Attachment): String {
        val stream = attachment.retrieveInputStream().await()
        return BufferedReader(InputStreamReader(stream)).use { reader ->
            reader.lineSequence().joinToString(System.lineSeparator())
        }
    }

    private suspend fun fetchHastebin(match: MatchResult): String? {
        val hastebinSite = match.groupValues[1]
        val pasteId = match.groupValues[2]
        val domain = if (hastebinSite == ".com") "hastebin.com" else "hasteb.in"
        val rawPaste = "https://$domain/raw/$pasteId"
        return fetchContent(rawPaste).await()
    }

    private suspend fun fetchPastebin(match: MatchResult): String? {
        val pasteId = match.groupValues[1]
        val rawPaste = "https://pastebin.com/raw/$pasteId"
        return fetchContent(rawPaste).await()
    }

    private suspend fun analyzeInput(
        inputString: String?,
        wasPaste: Boolean,
        event: GuildMessageReceivedEvent
    ): Boolean {
        require(inputString != null)
        val inputBlockMatch = Constants.CODE_BLOCK_REGEX.matchEntire(inputString)
        val cleanInput = if (inputBlockMatch != null) inputBlockMatch.groupValues[2].trim() else inputString
        if (!wasPaste && isCode(cleanInput)) {
            if (inputString.lines().size > MAX_LINES) {
                val message = event.channel.sendMessage(buildTooLongEmbed(Emotes.LOADING)).await()
                val hastebinUrl = HastebinUtil.postErrorToHastebin(cleanInput, bot.httpClient).await()
                message.editMessage(buildTooLongEmbed(hastebinUrl)).queue()
            }
        }

        return JVM_EXCEPTION_PATTERN.findAll(cleanInput).any {
            handleCommonException(it, event)
        }
    }

    private fun buildTooLongEmbed(url: String): EmbedConvention {
        return Embeds.warn(
            "Huch ist das viel?",
            """Bitte sende, lange Codeteile nicht über den Chat oder als File, benutze stattdessen, ein haste Tool. Mehr dazu findest du, bei `sudo tag haste`.
                                        |Faustregel: Alles, was mehr als $MAX_LINES Zeilen hat.
                                        |Hier ich mache das schnell für dich: $url
                                    """.trimMargin()
        )
    }

    private fun guessLanguage(potentialCode: String) = languageGuesser.highlightAuto(potentialCode, knownLanguages)

    private fun handleCommonException(match: MatchResult, event: GuildMessageReceivedEvent): Boolean {
        val exception = with(match.groupValues[1]) { substring(lastIndexOf('.') + 1) }
        val exceptionName = exception.toLowerCase()
        val tag = when {
            exceptionName == "nullpointerexception" -> "nullpointerexception"
            exceptionName == "unsupportedclassversionerror" -> "class-version"
            match.groupValues[2] == "Plugin already initialized!" -> "plugin-already-initialized"
            exceptionName == "invaliddescriptionexception" -> "plugin.yml"
            else -> null
        } ?: return false
        val tagContent = transaction { Tag.find { Tags.name eq tag }.firstOrNull() } ?: return false
        event.channel.sendMessage(tagContent.content).queue()
        return true
    }

    private fun isCode(potentialCode: String) = guessLanguage(potentialCode).language != null

    private fun fetchContent(url: String): CompletableFuture<String?> {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        return bot.httpClient.newCall(request).executeAsync().thenApply { response ->
            response.body.use {
                it?.string()
            }
        }
    }

    companion object {
        // https://regex101.com/r/vgz86r/8
        private val JVM_EXCEPTION_PATTERN =
            """(?m)^(?:Exception in thread ".*")?.*?(.+?(?<=Exception|Error))(?:\: )(.*)(?:\R+^\s*.*)?(?:\R+^\s*at .*)+""".toRegex()

        // https://regex101.com/r/u0QAR6/2
        private val HASTEBIN_PATTERN =
            "(?:https?:\\/\\/(?:www\\.)?)?hasteb((?:in\\.com|\\.in))\\/(?:raw\\/)?(.+?(?=\\.|\$)\\/?)".toRegex()

        // https://regex101.com/r/N8NBDz/1
        private val PASTEBIN_PATTERN = "(?:https?:\\/\\/(?:www\\.)?)?pastebin\\.com\\/(?:raw\\/)?(.*)".toRegex()
    }
}

// We don't want to highlight anything
@Suppress("FunctionName") // It should act like a class
private fun UselessRendererFactoryThing() = StyleRendererFactory { HtmlRenderer("") }
