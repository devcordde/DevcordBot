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

import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.database.Tag
import com.github.devcordde.devcordbot.database.Tags
import com.github.devcordde.devcordbot.dsl.EmbedConvention
import com.github.devcordde.devcordbot.dsl.editMessage
import com.github.devcordde.devcordbot.event.DevCordGuildMessageReceivedEvent
import com.github.devcordde.devcordbot.util.Googler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import okhttp3.OkHttpClient
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.OffsetDateTime

/**
 * I mean not actully a brain but the name sounds cool.
 */
class Brain(
    knownLanguages: List<String>,
    private val httpClient: OkHttpClient,
    private val maxLines: Int,
    googler: Googler
) {

    private val shortTimeMemory = mutableListOf<Conversation>()
    private val javadocFinder = JavaDocFinder(googler)
    private val guesser = LanguageGusser(knownLanguages)
    private val beautifier = CodeBeautifier(httpClient)

    init {
        cleanShortTimeMemory()
    }

    internal fun findConversation(event: DevCordGuildMessageReceivedEvent): Conversation {
        val found = shortTimeMemory.firstOrNull { it.owner == event.member && it.textChannel == event.channel }
        if (found == null) {

            val conversation = Conversation(
                event.member!!,
                event.message,
                mutableListOf(),
                mutableListOf(),
                null,
                OffsetDateTime.now()
            )

            shortTimeMemory.add(conversation)

            return conversation
        }
        return found
    }

    internal fun determinedException(conversation: Conversation, exception: StackTrace) {
        if (conversation.answer.exception?.sealed == true) return
        conversation.stacktraces.add(exception)
        conversation.lastInteraction = OffsetDateTime.now()
        think(conversation)
    }

    internal fun determinedClass(conversation: Conversation, clazz: Class) {
        conversation.classes.add(clazz)
        conversation.lastInteraction = OffsetDateTime.now()
        think(conversation)
    }

    private fun think(conversation: Conversation) {
        conversation.findException()
        conversation.findCause()
        if (conversation.answer.useless) return
        conversation.safeHelpMessage.editMessage(conversation.answer.toEmbed()).queue()
        if (conversation.answer.isComplete) {
            shortTimeMemory.remove(conversation)
        }
        tryToFindJavadoc(conversation)
    }

    private fun tryToFindJavadoc(conversation: Conversation) {
        GlobalScope.launch {
            val exception = conversation.answer.exception
            if (exception != null) {
                val javadoc = javadocFinder.findJavadocForClass(exception.exceptionName)
                conversation.answer.exception?.exceptionDoc = javadoc
                conversation.safeHelpMessage.editMessage(conversation.answer.toEmbed()).queue()
            }
        }
    }

    private fun Conversation.findException() {
        if (stacktraces.isEmpty()) return
        for (stacktrace in stacktraces) {
            val possibleAnswer = handleCommonException(stacktrace) ?: continue
            val stacktraceElement = stacktrace.elements.first()
            answer.exception = ConversationAnswer.ExceptionAnswer(
                stacktrace.exceptionName,
                possibleAnswer,
                stacktraceElement.className,
                stacktraceElement.lineNumber
            )
        }

        if (answer.exception == null) {
            val stacktrace = stacktraces.first()
            val stacktraceElement = stacktrace.elements.first()
            answer.exception = ConversationAnswer.ExceptionAnswer(
                stacktrace.exceptionName,
                null,
                stacktraceElement.className,
                stacktraceElement.lineNumber
            )
        }
    }

    private fun Conversation.findCause() {
        val exception = answer.exception ?: return
        for ((_, name, rawContent) in classes) {
            if (name != exception.causeClass) continue
            answer.causeContent = rawContent.lines()
                .getOrNull(exception.causeLine - 1)?.let { "`${it.trim()}`" }
                ?: "Der Inhalt konnte nicht gefunden werden bitte stelle sicher die ganze Klasse zu senden"
        }
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

    private fun handleCommonException(match: StackTrace) =
        handleCommonException(match.exceptionName, match.message)

    private fun handleCommonException(
        exception: String,
        message: String
    ): String? {
        val exceptionName = exception.substring(exception.lastIndexOf('.') + 1).toLowerCase().trim()
        val tag = when {
            exceptionName == "nullpointerexception" -> "nullpointerexception"
            exceptionName == "unsupportedclassversionerror" -> "class-version"
            exceptionName == "ClassCastException" -> "casting"
            message == "Plugin already initialized!" -> "plugin-already-initialized"
            exceptionName == "invaliddescriptionexception" -> "plugin.yml"
            exceptionName == "invalidpluginexception" && "cannot find main class" in message.toLowerCase() -> "main-class-not-found"
            else -> null
        } ?: return null
        val tagContent = transaction { Tag.find { Tags.name eq tag }.firstOrNull() } ?: return null
        return tagContent.content
    }

    private fun cleanShortTimeMemory() {
        GlobalScope.launch {
            while (true) {
                delay(Duration.ofSeconds(30))
                shortTimeMemory.removeAll {
                    Duration.between(it.lastInteraction, OffsetDateTime.now()) > Duration.ofSeconds(30)
                }
            }
        }
    }
}
