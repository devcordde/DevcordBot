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
import com.github.devcordde.devcordbot.database.Tag
import com.github.devcordde.devcordbot.database.Tags
import com.github.devcordde.devcordbot.event.DevCordGuildMessageReceivedEvent
import com.github.devcordde.devcordbot.util.Googler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.OffsetDateTime

/**
 * I mean not actully a brain but the name sounds cool.
 */
class Brain(
    googler: Googler
) {

    private val shortTimeMemory = mutableListOf<Conversation>()
    private val javadocFinder = JavaDocFinder(googler)

    init {
        cleanShortTimeMemory()
    }

    internal fun abandon(conversation: Conversation) {
        shortTimeMemory.remove(conversation)
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
        if (conversation.answer.exceptionSet && conversation.answer.exception.sealed) return
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
        if (!conversation.answer.exceptionSet) {
            conversation.findException()
        }
        if (!conversation.answer.causeSet) {
            conversation.findCause()
        }
        if (conversation.answer.useless) return
        if (!conversation.answer.npeHintSet) {
            conversation.analyzeNpe()
        }
        conversation.update()
        if (conversation.answer.isComplete) {
            abandon(conversation)
        }
        if (conversation.answer.exceptionSet && conversation.answer.exception.exceptionDoc == null) {
            tryToFindJavadoc(conversation)
        }
    }

    private fun Conversation.analyzeNpe() {
        if (!answer.causeSet or !answer.exceptionSet) return
        val causeContent = answer.causeContent.drop(1).dropLast(1)
        if (answer.exception.exceptionName.equals(
                "java.lang.nullpointerexception",
                ignoreCase = true
            )
        ) {
            val elements = causeContent.split('.').dropLast(1)
            answer.npeHint = answer.exception.causeLine?.let { buildNpeHint(elements, it) } ?: return
        }
    }

    private fun buildNpeHint(elements: List<String>, lineNumber: Int): String =
        """Eines dieser Dinge ${
            elements.joinToString(
                prefix = "(`",
                separator = "`, `",
                postfix = "`)"
            )
        } scheint null zu sein.
        |Bitte stelle sicher, dass alle davon einen Wert haben oder füge ein `Null-Check` hinzu.
        |**Tipp**: Füge einen Breakpoint zur Zeile `$lineNumber` hinzu um zu checken was genau `null` ist oder benutze die [Helpful NPEs](https://openjdk.java.net/jeps/358) von Java 14+.
    """.trimMargin()

    private fun tryToFindJavadoc(conversation: Conversation) {
        GlobalScope.launch {
            val exception = conversation.answer.exception
            val doc = javadocFinder.findJavadocForClass(exception.exceptionName)
            if (doc != null) {
                exception.explanation = exception.explanation ?: doc.description
                exception.exceptionDoc = doc.uri
            } else {
                exception.exceptionDoc = "Es wurde kein Doc gefunden"
            }
            conversation.update()
        }
    }

    private fun Conversation.findException() {
        fun List<StackTraceElement>.firstByUser(): StackTraceElement? = firstOrNull {
            !it.pakage.matches(Constants.KNOWN_PACKAGES)
        }

        fun buildAnswer(finalStackTrace: StackTrace, stackTrace: StackTrace, possibleAnswer: String?) {
            val causedException = finalStackTrace.cause ?: finalStackTrace
            val stacktraceElement = causedException.elements.firstByUser()
            answer.exception = ConversationAnswer.ExceptionAnswer(
                causedException.exceptionName,
                causedException.message,
                stackTrace.cause?.let {
                    if (stackTrace.message.isOrSpellsNullOrBlank()) {
                        stackTrace.exceptionName
                    } else {
                        "${stackTrace.exceptionName}: ${stackTrace.message}"
                    }
                },
                possibleAnswer,
                stacktraceElement?.className,
                stacktraceElement?.lineNumber
            )
        }

        if (stacktraces.isEmpty()) return
        for (stacktrace in stacktraces) {
            var finalStackTrace: StackTrace = stacktrace
            var possibleAnswer = handleCommonException(stacktrace)
            if (possibleAnswer == null) {
                if (stacktrace.cause != null) {
                    finalStackTrace = stacktrace.cause
                    possibleAnswer = handleCommonException(finalStackTrace)
                } else continue
            }
            if (possibleAnswer == null) continue

            buildAnswer(finalStackTrace, stacktrace, possibleAnswer)
        }

        if (!answer.exceptionSet) { // fallback to first
            val exception = stacktraces.first()
            val finalException = exception.cause ?: exception
            buildAnswer(finalException, exception, null)
        }
    }

    private fun Conversation.findCause() {
        if (!answer.exceptionSet) return
        val exception = answer.exception
        if (exception.causeLine == null) return
        for ((_, name, rawContent) in classes) {
            if (name != exception.causeClass) continue
            answer.causeContent = rawContent.lines()
                .getOrNull(exception.causeLine - 1)?.let { "`${it.trim()}`" }
                ?: "Der Inhalt konnte nicht gefunden werden bitte stelle sicher die ganze Klasse zu senden"
        }
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
            exceptionName == "arrayindexoutofboundsexception" -> "ArrayIndexOutOfBoundsException"
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

private fun String.isOrSpellsNullOrBlank() = isNullOrBlank() || this == "null"
