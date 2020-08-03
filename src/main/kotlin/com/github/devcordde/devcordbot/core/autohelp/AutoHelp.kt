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
import com.github.devcordde.devcordbot.database.DatabaseDevCordUser
import com.github.devcordde.devcordbot.dsl.EmbedConvention
import com.github.devcordde.devcordbot.dsl.editMessage
import com.github.devcordde.devcordbot.dsl.sendMessage
import com.github.devcordde.devcordbot.event.DevCordGuildMessageReceivedEvent
import com.github.devcordde.devcordbot.event.EventSubscriber
import com.github.devcordde.devcordbot.util.HastebinUtil
import com.github.devcordde.devcordbot.util.await
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import org.jetbrains.exposed.sql.transactions.transaction
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

    private val executor = Executors.newFixedThreadPool(10).asCoroutineDispatcher()
    private val fetcher = ContentFetcher(bot.httpClient, bot.github, executor)
    private val brain = Brain(bot.googler)
    private val guesser = LanguageGusser(knownLanguages)
    private val beautifier = CodeBeautifier(bot.httpClient)

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
                    || bypassWord in input)
        ) return

        val inputs = fetcher.fetchMessageContents(event.message)
        val codeblocks =
            Constants.CODE_BLOCK_REGEX.findAll(input).map {
                val language = it.groupValues.getOrNull(1)
                language to it.groupValues[2]
            }.toList()
        val messageContents = if (codeblocks.isEmpty()) {
            listOf(null to input)
        } else codeblocks

        val tooLongBlocks =
            messageContents.mapNotNull {
                if (it.second.lines().size < maxLines) {
                    null
                } else {
                    val language = guesser.guessLanguage(it.second)
                    language?.let { result ->
                        (it.first ?: result.language) to it.second
                    }
                }
            }

        if (tooLongBlocks.isNotEmpty()) {
            val message = event.channel.sendMessage(buildTooLongEmbed()).await()
            val hasteUrls = tooLongBlocks.map {
                val rawUrl = beautifier.formatCode(it.second).thenCompose { beautified ->
                    HastebinUtil.postErrorToHastebin(beautified, bot.httpClient)
                }.await()
                if (!it.first.isNullOrBlank()) {
                    rawUrl + ".${it.first}"
                } else rawUrl
            }
            message.editMessage(buildTooLongEmbed(hasteUrls.joinToString())).queue()
        }

        if (userLevel >= levelLimit) return
        val newConversation = { brain.findConversation(event) }
        val container = ConversationContainer(newConversation)

        for (future in (inputs + CompletableFuture.completedFuture(messageContents.map { it.second }))) {
            val userInput = future.await()
            userInput.toList().forEach {
                if (it != null) {
                    JVM_EXCEPTION_PATTERN.findAll(it).toList().forEach { match ->
                        val root = match.groupValues.drop(1)
                        val rootException = parseException(root)
                        val cause = root.drop(5).filterNot(String::isNullOrBlank)
                        val finalException = if (cause.isNotEmpty()) {
                            val causeException = parseException(cause)
                            rootException.copy(cause = causeException)
                        } else rootException

                        if (container.conversation.answer.exceptionSet) {
                            brain.abandon(container.conversation)
//                            container.conversation = brain.findConversation(event)
                        }
                        brain.determinedException(container.conversation, finalException)
                    }

                    JAVA_CLASS_PATTERN.findAll(it).forEach { match ->
                        val (_, pakage, _, name) = match.groupValues
                        brain.determinedClass(container.conversation, Class(pakage, name, it))
                    }
                }
            }
        }
    }

    private fun parseException(values: List<String>): StackTrace {
        val (name, message) = values
        val elementsRaw = values[2]
        val elements = STACK_TRACE_ELEMENT_PATTERN.findAll(elementsRaw).map { elementMatch ->
            val (_, pakage, method, className, line) = elementMatch.groupValues
            StackTraceElement(pakage, method, className, line.toInt())
        }.toList()

        return StackTrace(name, message, elements)
    }

    private fun buildTooLongEmbed(url: String? = null): EmbedConvention {
        return Embeds.warn(
            "Huch, ist das viel?",
            """Bitte sende lange Codeteile nicht über den Chat oder als Datei, sondern benutze stattdessen ein haste-Tool. Mehr dazu findest du bei `sudo tag haste`.
                                        |Faustregel: Alles, was mehr als $maxLines Zeilen hat.
                                        |Hier, ich mache das schnell für dich: ${url ?: Emotes.LOADING}
                                    """.trimMargin()
        )
    }

    companion object {
        /**
         * https://regex101.com/r/vgz86r/17
         */
        val JVM_EXCEPTION_PATTERN: Regex =
            """(?m)^(?:Exception in thread ".*")?.*?(.+?(?<=Exception|Error:))(?:\: )?(.*)((?:\R+^\s*.*)?(?:\R+^.*at .*)+)(\R)*(Caused by: (.+?(?<=Exception|Error:))(?:\: )?(.*)?((?:\R+^\s*.*)?(?:\R+^.*at .*)+))?""".toRegex()

        /**
         * https://regex101.com/r/xYGH0m/2
         */
        val STACK_TRACE_ELEMENT_PATTERN: Regex =
            "at ((?:(?:\\w)+\\.?)+)\\.((?:\\w|<|>)+)\\((\\w+).java:([0-9]+)\\)".toRegex()

        /**
         * Java class pattern (should match kotlin (maybe))
         * https://regex101.com/r/uksxY5/3
         */
        val JAVA_CLASS_PATTERN: Regex =
            """(?m)package ((?:\w+\.?)+);[\s\S]*(public|private|protected) class (\w*) \{([\s\S]*)}""".toRegex()

    }
}

private data class ConversationContainer(
    private val newConversation: () -> Conversation,
    private var setConversation: Conversation? = null
) {
    var conversation: Conversation
        get() = setConversation ?: newConversation().also { setConversation = it }
        set(value) {
            setConversation = value
        }
}
