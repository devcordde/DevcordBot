/*
 * Copyright 2020 Daniel Scherf & Michael Rittmeister
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
import com.github.seliba.devcordbot.util.executeAsync
import mu.KotlinLogging
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import okhttp3.Request
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture

private const val MAX_LINES = 10
private val WHITELIST = listOf(486916258965618690, 649253900473597952, 671772614993379328)
private val KNOWN_LANGUAGES = arrayOf("java", "python", "go", "kotlin", "cs")

/**
 * Automatic analzyer for common pitfalls.
 */
class CommonPitfallListener(private val bot: DevCordBot) {
    private val logger = KotlinLogging.logger {}
    private val languageGuesser = Highlighter(UselessRendererFactoryThing())

    /**
     * Listens for new messages.
     */
    @EventSubscriber
    fun onMessage(event: GuildMessageReceivedEvent) {
        val input = event.message.contentRaw
        if (event.author.isBot || (!bot.debugMode && event.channel.parent?.idLong !in WHITELIST) || Constants.prefix.find(
                input
            ) != null
        ) return
        val hastebinMatch = HASTEBIN_PATTERN.find(input)
        val firstAttachment = event.message.attachments.firstOrNull()
        val actualInput =
            if (firstAttachment != null && !firstAttachment.isImage && !firstAttachment.isVideo) {
                firstAttachment.retrieveInputStream().thenApply {
                    BufferedReader(InputStreamReader(it)).use { reader ->
                        reader.lineSequence().joinToString(System.lineSeparator()) to false
                    }
                }
            } else
                if (hastebinMatch != null) {
                    val hastebinSite = hastebinMatch.groupValues[1]
                    val pasteId = hastebinMatch.groupValues[2]
                    val domain = if (hastebinSite == ".com") "hastebin.com" else "hasteb.in"
                    val rawPaste = "https://$domain/raw/$pasteId"
                    fetchContent(rawPaste)
                } else {
                    val pastebinMatch = PASTEBIN_PATTERN.find(input)
                    if (pastebinMatch != null) {
                        val pasteId = pastebinMatch.groupValues[1]
                        val rawPaste = "https://pastebin.com/raw/$pasteId"
                        fetchContent(rawPaste)
                    } else {
                        CompletableFuture.completedFuture(input to false)
                    }
                }
        actualInput.thenAccept { analyzeInput(it, event) }
            .exceptionally { logger.error(it) { "An error occurred while analyzing message" };null }
    }

    private fun analyzeInput(input: Pair<String?, Boolean>, event: GuildMessageReceivedEvent) {
        val (inputString, wasPaste) = input
        require(inputString != null)
        val inputBlockMatch = Constants.CODE_BLOCK_REGEX.matchEntire(inputString)
        val cleanInput = if (inputBlockMatch != null) inputBlockMatch.groupValues[2].trim() else inputString
        if (!wasPaste && isCode(cleanInput)) {
            val hastebinUrlFuture = HastebinUtil.postErrorToHastebin(cleanInput, bot.httpClient)
            if (inputString.lines().size > MAX_LINES) {
                event.channel.sendMessage(
                    buildTooLongEmbed(Emotes.LOADING)
                )
                    .submit()
                    .thenCombine(hastebinUrlFuture) { message, url ->
                        message.editMessage(buildTooLongEmbed(url)).queue()
                    }
            }
        }
        val exceptionMatch = JVM_EXCEPTION_PATTERN.find(cleanInput)
        if (exceptionMatch != null) {
            handleCommonException(exceptionMatch, event)
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

    private fun guessLanguage(potentialCode: String) = languageGuesser.highlightAuto(potentialCode, KNOWN_LANGUAGES)

    private fun handleCommonException(match: MatchResult, event: GuildMessageReceivedEvent) {
        val exception = with(match.groupValues[1]) { substring(lastIndexOf('.') + 1) }
        val exceptionName = exception.toLowerCase()
        val tag = when {
            exceptionName == "nullpointerexception" -> "nullpointerexception"
            exceptionName == "unsupportedclassversionerror" -> "class-version"
            match.groupValues[2] == "Plugin already initialized!" -> "plugin-already-initialized"
            else -> null
        } ?: return
        val tagContent = transaction { Tag.find { Tags.name eq tag }.first().content }
        @Suppress("ReplaceNotNullAssertionWithElvisReturn") // We know that all the tags exist
        event.channel.sendMessage(tagContent).queue()
    }

    private fun isCode(potentialCode: String) = guessLanguage(potentialCode).language != null

    private fun fetchContent(url: String): CompletableFuture<Pair<String?, Boolean>> {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        return bot.httpClient.newCall(request).executeAsync().thenApply { response ->
            response.body.use {
                it?.string() to true
            }
        }
    }

    companion object {
        // https://regex101.com/r/vgz86r/5
        private val JVM_EXCEPTION_PATTERN =
            """(?m)^(?:Exception in thread ".*")?.*?(.+?(?<=Exception|Error))(?:\: )?(.*)(?:\R+^\s*.*)?(?:\R+^\s*at .*)+""".toRegex()

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
