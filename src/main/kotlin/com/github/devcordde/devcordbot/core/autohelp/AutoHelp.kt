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

package com.github.devcordde.devcordbot.core.autohelp

import com.github.devcordde.devcordbot.constants.Constants
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.constants.Emotes
import com.github.devcordde.devcordbot.core.DevCordBot
import com.github.devcordde.devcordbot.database.*
import com.github.devcordde.devcordbot.dsl.EmbedConvention
import com.github.devcordde.devcordbot.dsl.editMessage
import com.github.devcordde.devcordbot.dsl.sendMessage
import com.github.devcordde.devcordbot.event.DevCordGuildMessageReceivedEvent
import com.github.devcordde.devcordbot.event.EventSubscriber
import com.github.devcordde.devcordbot.util.HastebinUtil
import com.github.devcordde.devcordbot.util.await
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

private val levelLimit = dotenv()["AUTO_HELP_LEVEL_LIMIT"]?.toInt() ?: 75

/**
 * AutoHelp.
 */
class AutoHelp(
    private val bot: DevCordBot,
    private val whitelist: List<String>,
    private val blacklist: List<String>,
    knownLanguages: List<String>,
    private val bypassWord: String,
    private val maxLines: Int
) {

    private val guesser = LanguageGusser(knownLanguages)
    private val fetcher = ContentFetcher(bot.httpClient)
    private val beautifier = CodeBeautifier(bot.httpClient)
    private val executor = Executors.newFixedThreadPool(10).asCoroutineDispatcher()

    /**
     * Trigger AutoHelp on Message.
     */
    @EventSubscriber
    suspend fun onMessage(event: DevCordGuildMessageReceivedEvent) {
        val input = event.message.contentRaw
        val userLevel by lazy { transaction { DatabaseDevCordUser.findOrCreateById(event.author.idLong).level } }

        if (
            !bot.debugMode &&
            (event.author.isBot
                    || (event.channel.parent?.id !in whitelist || event.channel.id in blacklist)
                    || userLevel > levelLimit
                    || bypassWord in input)
        ) return

        // Asynchronously fetch potential content
        val attachments = fetchAttachments(event.message)
        if (input.isNotBlank()) {
            val hastebinMatches = findInput(HASTEBIN_PATTERN, input, ::fetchHastebin)
            val pastebinMatches = findInput(PASTEBIN_PATTERN, input, ::fetchPastebin)
            val ghostbinMatches = findInput(GHOSTBIN_PATTERN, input, ::fetchGhostbin)
            val githubMatches = findInputs(GITHUB_GIST_PATTERN, input, ::fetchGithub)

            // Quit on first match
            if (analyzeInputs(hastebinMatches, event)) {
                pastebinMatches.cancel(true)
                ghostbinMatches.cancel(true)
                githubMatches.cancel(true)
                attachments.cancel(true)
                return
            }

            if (analyzeInputs(pastebinMatches, event)) {
                ghostbinMatches.cancel(true)
                githubMatches.cancel(true)
                attachments.cancel(true)
                return
            }

            if (analyzeInputs(githubMatches, event)) {
                ghostbinMatches.cancel(true)
                attachments.cancel(true)
                return
            }

            if (analyzeInputs(ghostbinMatches, event)) {
                attachments.cancel(true)
                return
            }

            if (analyzeInput(input, false, event)) {
                return
            }
        }
        analyzeInputs(attachments, event, false)
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
            }
        }
    }

    private fun findInputs(
        pattern: Regex,
        input: String,
        fetcher: (MatchResult, String) -> CompletableFuture<List<String?>>
    ): CompletableFuture<List<String?>> {
        return GlobalScope.future(executor) {
            pattern.findAll(input).toList().map {
                fetcher(it, input).await()
            }.flatten()
        }
    }

    private suspend fun fetchAttachments(message: Message): CompletableFuture<List<String?>> {
        return GlobalScope.future(executor) {
            message.attachments.filter { !it.isVideo && !it.isImage }.map {
                fetchAttachment(it)
            }
        }
    }

    private suspend fun fetchAttachment(attachment: Message.Attachment): String {
        val stream = attachment.retrieveInputStream().await()
        return BufferedReader(InputStreamReader(stream)).use { reader ->
            reader.readText()
        }
    }

    private fun fetchHastebin(match: MatchResult): CompletableFuture<String?> {
        val domain = match.groupValues[1]
        val pasteId = match.groupValues[2]
        val rawPaste = "https://$domain/raw/$pasteId"
        return fetcher.fetchContent(rawPaste)
    }

    private fun fetchPastebin(match: MatchResult): CompletableFuture<String?> {
        val pasteId = match.groupValues[1]
        val rawPaste = "https://pastebin.com/raw/$pasteId"
        return fetcher.fetchContent(rawPaste)
    }

    private fun fetchGhostbin(match: MatchResult): CompletableFuture<String?> {
        val pasteId = match.groupValues[1]
        val rawPaste = "https://ghostbin.co/paste/$pasteId/raw"
        return fetcher.fetchContent(rawPaste)
    }

    private fun fetchGithub(match: MatchResult, url: String): CompletableFuture<List<String?>> {
        val host = match.groupValues[1]
        val gistId = match.groupValues[3]
        if (host == "gist.githubusercontent.com") {
            return fetcher.fetchContent(url).thenApply { listOf(it) }
        }
        return bot.github.retrieveGistFiles(gistId).thenApplyAsync { fileUrls ->
            fileUrls.map { fetcher.fetchContent(it).join() }
        }
    }

    private suspend fun analyzeInput(
        inputString: String?,
        wasPaste: Boolean,
        event: GuildMessageReceivedEvent
    ): Boolean {
        require(inputString != null)

        // It's kind of unlikely to paste code blocks on hastebin so only check for codeblocks if it's not pasted
        val inputBlockMatch by lazy { Constants.CODE_BLOCK_REGEX.matchEntire(inputString) }
        val cleanInput = inputBlockMatch?.groupValues?.get(2)?.trim() ?: inputString

        val language = guesser.guessLanguage(cleanInput)
        if (!wasPaste && language != null && inputString.lines().size > maxLines && !Constants.prefix.containsMatchIn(
                inputString
            )
        ) {
            val code = if (language.language.equals("java", ignoreCase = true)) {
                beautifier.formatCode(cleanInput).await()
            } else cleanInput

            val message = event.channel.sendMessage(buildTooLongEmbed(Emotes.LOADING)).await()
            val hastebinUrl = HastebinUtil.postErrorToHastebin(code, bot.httpClient).await()
            message.editMessage(buildTooLongEmbed(hastebinUrl)).queue()

        }

        val exceptions = JVM_EXCEPTION_PATTERN.findAll(cleanInput)
        if (exceptions.count() != 0) {
            return exceptions.any {
                handleCommonException(it, event)
            }
        }

        if (JVM_EXCEPTION_NAME_PATTERN.findAll(cleanInput).count() != 0) {
            event.channel.sendMessage(missingStacktrace).queue()
            return true
        }
        return false
    }

    private fun buildTooLongEmbed(url: String): EmbedConvention {
        return Embeds.warn(
            "Huch, ist das viel?",
            """Bitte sende lange Codeteile nicht über den Chat oder als Datei, sondern benutze stattdessen ein haste-Tool. Mehr dazu findest du bei `sudo tag haste`.
                                        |Faustregel: Alles, was mehr als $maxLines Zeilen hat.
                                        |Hier, ich mache das schnell für dich: $url
                                    """.trimMargin()
        )
    }

    private fun handleCommonException(match: MatchResult, event: GuildMessageReceivedEvent): Boolean {
        val exceptionName = match.groupValues[1]
        val message = match.groupValues[2].trim()
        if (!handleCommonException(exceptionName, message, event)) {
            val exceptionInMessage = JVM_EXCEPTION_NAME_PATTERN.matchEntire(message) ?: return false
            val newName = exceptionInMessage.groupValues[1]
            val newMessage = exceptionInMessage.groupValues[2]
            return handleCommonException(newName, newMessage, event)
        }
        return false
    }

    private fun handleCommonException(
        exception: String,
        message: String,
        event: GuildMessageReceivedEvent
    ): Boolean {
        val exceptionName = exception.substring(exception.lastIndexOf('.') + 1).trim()
        val searchName = exceptionName.toLowerCase()
        val tagName = when {
            searchName == "nullpointerexception" -> {
                val helpfulNpe = JAVA14_HELPFUL_NPE_PATTERN.matchEntire(message)
                if (helpfulNpe == null) {
                    "nullpointerexception"
                } else {
                    NPEAnalyzer.handleHelpfulNpe(helpfulNpe, event)
                    return true
                }
            }
            searchName == "unsupportedclassversionerror" -> "class-version"
            searchName == "ClassCastException" -> "casting"
            message == "Plugin already initialized!" -> "plugin-already-initialized"
            searchName == "invaliddescriptionexception" -> "plugin.yml"
            searchName == "invalidpluginexception" && "cannot find main class" in message.toLowerCase() -> "main-class-not-found"
            else -> null
        }
        val tag = transaction {
            if (tagName == null) {
                TagAlias.find { TagAliases.name eq searchName }.firstOrNull()?.tag
                    ?: Tag.find { Tags.name eq searchName }.firstOrNull()
            } else {
                Tag.find { Tags.name eq tagName }.firstOrNull()
            }
        } ?: return false
        if (!tag.autoHelp) return false
        event.channel.sendMessage(buildHelpEmbed(tag, exceptionName, event.jda)).queue()
        return true
    }

    private fun buildHelpEmbed(tag: Tag, exceptionName: String, jda: JDA) = Embeds.info(
        "AutoHelp - $exceptionName",
        tag.content
    ) {
        author {
            val user = jda.getUserById(tag.author) ?: jda.selfUser
            name = user.name
            iconUrl = user.avatarUrl ?: user.defaultAvatarUrl
        }

        footer("AutoHelp V1 BETA - Bitte Bugs auf GitHub.com/devcordde/DevcordBot melden.")
    }


    companion object {
        // https://regex101.com/r/vgz86r/14
        private val JVM_EXCEPTION_PATTERN =
            """(?m)^(?:Exception in thread ".*")?.*?(.+?(?<=Exception|Error:))(?:\: )?(?:.*)(\R+^\s*(?!(.*)at).*)?(?:\R+^.*at .*)+""".toRegex()

        // https://regex101.com/r/SL7g4g/2
        private val JAVA14_HELPFUL_NPE_PATTERN =
            """Cannot (assign|read|load from|store to|throw|invoke|enter|exit) (?:(field|method|.* array|synchronized block|the array length|exception) )?(?:"(.*)" )?because "(.*)" is null""".toRegex()

        // https://regex101.com/r/HtaGF8/3
        private val JVM_EXCEPTION_NAME_PATTERN =
            """(?:Exception in thread ".*")?.*?(.+?(?<=Exception|Error))(?:\: )(.+)(?:\R+^\s*.*)?""".toRegex()

        // https://regex101.com/r/u0QAR6/6
        private val HASTEBIN_PATTERN =
            "(?:https?:\\/\\/)?(?:(?:www\\.)?)?(hastebin\\.com|hasteb\\.in|paste\\.helpch\\.at)\\/(?:raw\\/)?(.+?(?=\\.|\$|\\/|#))".toRegex()

        // https://regex101.com/r/N8NBDz/2
        private val PASTEBIN_PATTERN =
            "(?:https?:\\/\\/(?:www\\.)?)?pastebin\\.com\\/(?:raw\\/)?(.+?(?=\\.|\$|\\/|#))".toRegex()

        // https://regex101.com/r/CyjiKt/2
        private val GHOSTBIN_PATTERN =
            "(?:https?:\\/\\/(?:www\\.)?)?(?:ghostbin\\.co)\\/(?:paste\\/)?(.+?(?=\\.|\$|\\/))(?:\\/raw)?".toRegex()

        // https://regex101.com/r/AlVYjn/2
        private val GITHUB_GIST_PATTERN =
            "(?:https?:\\/\\/)?(gist\\.github\\.com|gist.githubusercontent.com)\\/(.+?(?=\\.|\$|\\/))\\/(.+?(?=\\.|\$|\\/|#))".toRegex()

        private val missingStacktrace = Embeds.warn(
            "Unvollständige Fehlermeldung",
            """Glückwunsch du hast es geschafft die Fehlermeldung zu finden was schonmal gut ist.
                    Was noch besser wäre ist wenn du nun auch die vollständige Fehlermeldung hier reinschickst.
                    Das bedeuted, dass du sowohl den Stacktrace (das ganze `at package.method(File.java:Zeile)`) und den eventuellen `Caused by:` Teil mitschickst, dann kann man dir hier auch schneller helfen.
                    """.trimMargin()
        )
    }
}
