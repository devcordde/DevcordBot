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

package com.github.seliba.devcordbot.core.autohelp

import com.github.seliba.devcordbot.constants.Constants
import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.constants.Emotes
import com.github.seliba.devcordbot.core.DevCordBot
import com.github.seliba.devcordbot.database.Tag
import com.github.seliba.devcordbot.database.Tags
import com.github.seliba.devcordbot.dsl.EmbedConvention
import com.github.seliba.devcordbot.dsl.editMessage
import com.github.seliba.devcordbot.dsl.sendMessage
import com.github.seliba.devcordbot.event.EventSubscriber
import com.github.seliba.devcordbot.util.HastebinUtil
import com.github.seliba.devcordbot.util.await
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class AutoHelp(
    private val bot: DevCordBot,
    private val whitelist: List<String>,
    private val blacklist: List<String>,
    knownLanguages: List<String>
) {

    private val guesser = LanguageGusser(knownLanguages)
    private val fetcher = ContentFetcher(bot.httpClient)
    private val executor = Executors.newFixedThreadPool(10).asCoroutineDispatcher()

    @EventSubscriber
    suspend fun onMessage(event: GuildMessageReceivedEvent) {
        val input = event.message.contentRaw
        if (event.author.isBot ||
            (!bot.debugMode && (event.channel.parent?.id !in whitelist ||
                    event.channel.id in blacklist)) ||
            BYPASS_WORD in input
        ) return

        // Asynchronously fetch potential content
        val hastebinMatches = findInput(HASTEBIN_PATTERN, input, ::fetchHastebin)
        val pastebinMatches = findInput(PASTEBIN_PATTERN, input, ::fetchPastebin)
        val attachments = fetchAttachments(event.message)

        // Quit on first match
        if (analyzeInputs(hastebinMatches, event)) {
            pastebinMatches.cancel(true)
            attachments.cancel(true)
            return
        }
        if (analyzeInputs(pastebinMatches, event)) {
            attachments.cancel(true)
            return
        }
        // Also send too long message

        if (analyzeInputs(attachments, event, false)) return
        analyzeInput(input, false, event)
    }

    private suspend fun analyzeInputs(
        matches: CompletableFuture<List<String?>>,
        event: GuildMessageReceivedEvent,
        paste: Boolean = true
    ): Boolean {
        return matches.await().any {
            analyzeInput(it, paste, event)
        }
    }

    private fun findInput(
        pattern: Regex,
        input: String,
        fetcher: (MatchResult) -> CompletableFuture<String?>
    ): CompletableFuture<List<String?>> {
        return GlobalScope.future(executor) {
            pattern.findAll(input).toList().map {
                fetcher(it).await()
            }.also {
                println("DONE")
            }
        }
    }

    private suspend fun fetchAttachments(message: Message): CompletableFuture<List<String?>> {
        return GlobalScope.future(executor) {
            message.attachments.map {
                fetchAttachment(it)
            }
        }
    }

    private suspend fun fetchAttachment(attachment: Message.Attachment): String {
        val stream = attachment.retrieveInputStream().await()
        return BufferedReader(InputStreamReader(stream)).use { reader ->
            reader.lineSequence().joinToString(System.lineSeparator())
        }

    }

    private fun fetchHastebin(match: MatchResult): CompletableFuture<String?> {
        val hastebinSite = match.groupValues[1]
        val pasteId = match.groupValues[2]
        val domain = if (hastebinSite == ".com") "hastebin.com" else "hasteb.in"
        val rawPaste = "https://$domain/raw/$pasteId"
        return fetcher.fetchContent(rawPaste)
    }

    private fun fetchPastebin(match: MatchResult): CompletableFuture<String?> {
        val pasteId = match.groupValues[1]
        val rawPaste = "https://pastebin.com/raw/$pasteId"
        return fetcher.fetchContent(rawPaste)
    }

    private suspend fun analyzeInput(
        inputString: String?,
        wasPaste: Boolean,
        event: GuildMessageReceivedEvent
    ): Boolean {
        require(inputString != null)

        // It's kind of unlikely to paste code blocks on hastebin so only check for codeblocks if it's not pasted
        val inputBlockMatch by lazy { Constants.CODE_BLOCK_REGEX.matchEntire(inputString) }
        val cleanInput =
            if (!wasPaste && inputBlockMatch != null) inputBlockMatch!!.groupValues[2].trim() else inputString

        if (!wasPaste && guesser.isCode(cleanInput)) {
            if (inputString.lines().size > MAX_LINES &&
                !Constants.prefix.containsMatchIn(inputString)
            ) {
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

    companion object {
        // https://regex101.com/r/vgz86r/8
        private val JVM_EXCEPTION_PATTERN =
            """(?m)^(?:Exception in thread ".*")?.*?(.+?(?<=Exception|Error))(?:\: )(.*)(?:\R+^\s*.*)?(?:\R+^\s*at .*)+""".toRegex()

        // https://regex101.com/r/u0QAR6/2
        private val HASTEBIN_PATTERN =
            "(?:https?:\\/\\/(?:www\\.)?)?hasteb((?:in\\.com|\\.in))\\/(?:raw\\/)?(.+?(?=\\.|\$)\\/?)".toRegex()

        // https://regex101.com/r/N8NBDz/1
        private val PASTEBIN_PATTERN = "(?:https?:\\/\\/(?:www\\.)?)?pastebin\\.com\\/(?:raw\\/)?(.*)".toRegex()

        private const val BYPASS_WORD = "_Ü?"

        private const val MAX_LINES = 15
    }
}
