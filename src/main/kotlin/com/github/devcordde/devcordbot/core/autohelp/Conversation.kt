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
import com.github.devcordde.devcordbot.constants.Emotes
import com.github.devcordde.devcordbot.dsl.EmbedConvention
import com.github.devcordde.devcordbot.dsl.editMessage
import com.github.devcordde.devcordbot.dsl.sendMessage
import com.github.devcordde.devcordbot.util.limit
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import java.time.OffsetDateTime

/**
 * Interface for possible autp-help user input
 */
interface AutoHelpItem

/**
 * Representation of a parsed Stack trace element.
 *
 * @property pakage the package of the class of the element
 * @property method the name of the method that was called
 * @property className the name of the class
 * @property lineNumber the
 */
data class StackTraceElement(val pakage: String, val method: String, val className: String, val lineNumber: Int)

/**
 * Representation of a Stacktrace.
 *
 * @property exceptionName the name of the exception
 * @property message the message
 * @property elements a list of [StackTraceElement]s
 * @property cause a probably cause for this stacktrace
 */
data class StackTrace(
    val exceptionName: String,
    val message: String,
    val elements: List<StackTraceElement>,
    val cause: StackTrace? = null
) : AutoHelpItem

/**
 * Representation of a class.
 *
 * @property pakage the package of the class
 * @property name the name of the class
 * @property rawContent the raw content of the class
 */
data class Class(val pakage: String, val name: String, val rawContent: String) : AutoHelpItem

/**
 * Representation of a "Conversation" for autohelp.
 *
 * @property owner the user starting the conversation
 * @property trigger the initial message
 * @property stacktraces a list of [StackTrace]s provided by the user
 * @property classes a list of [Class]es provided by the user7
 * @property helpMessage the helpmessage sent by the bot
 * @property lastInteraction the [OffsetDateTime] of the last interaction in this conversation
 * @property answer the [ConversationAnswer]
 */
data class Conversation(
    val owner: Member,
    val trigger: Message,
    val stacktraces: MutableList<StackTrace>,
    val classes: MutableList<Class>,
    var helpMessage: Message?,
    var lastInteraction: OffsetDateTime,
    val answer: ConversationAnswer = ConversationAnswer()
) {
    /**
     * The text Channel the message is in.
     */
    val textChannel: TextChannel
        get() = trigger.textChannel

    /**
     * Creates message if needed.
     */
    private val safeHelpMessage: Message
        get() {
            if (helpMessage == null) {
                helpMessage = trigger.channel.sendMessage(
                    Embeds.loading(
                        "Ich denke nach!",
                        "Ich versuche zu helfen bitte warte etwas."
                    )
                ).complete()
            }
            return helpMessage!!
        }

    /**
     * Updates the Discord answer message.
     */
    fun update(): Unit =
        safeHelpMessage.editMessage(answer.toEmbed()).queue()
}

/**
 * Mutable object to store information on how to answer an error.
 *
 * @property exception the exception daat
 * @property causeContent the raw string of the line causing the issue
 * @property useless whether this answer does contain any useful info or not
 * @property isComplete whether all information was found or not
 *  @property npeHint hint to resolve an NPE
 */
class ConversationAnswer {
    lateinit var exception: ExceptionAnswer
    lateinit var causeContent: String
    lateinit var npeHint: String

    val useless: Boolean
        get() = !::exception.isInitialized && !::causeContent.isInitialized

    val isComplete: Boolean
        get() = ::exception.isInitialized && ::causeContent.isInitialized

    internal val exceptionSet: Boolean
        get() = ::exception.isInitialized

    internal val causeSet: Boolean
        get() = ::causeContent.isInitialized

    internal val npeHintSet: Boolean
        get() = ::npeHint.isInitialized

    /**
     * Info about the exception.
     *
     * @property exceptionName the name of the exception
     * @property exceptionMessage the message of the exception
     * @property causee exception that is caused by this
     * @property explanation the tag explaining the exception
     * @property causeClass the class in which the exception is caused
     * @property causeLine the line in which the exception is caused
     * @property exceptionDoc a URL to the exceptions javadoc
     * @property sealed whether this is the final exception data or not
     */
    data class ExceptionAnswer(
        val exceptionName: String,
        val exceptionMessage: String?,
        val causee: String?,
        var explanation: String?,
        val causeClass: String?,
        val causeLine: Int?,
        var exceptionDoc: String? = null,
        var sealed: Boolean = false
    )

    /**
     * Converts the answer into a user-friendly embed.
     */
    fun toEmbed(): EmbedConvention = Embeds.info("AutoHelp - ${exception.exceptionName}", exception.explanation) {

        if (exceptionSet) {
            addField("Exception", exception.exceptionName)
            if (!exception.exceptionMessage.isNullOrBlank() && exception.exceptionMessage != null && exception.exceptionMessage != "null") {
                addField("Beschreibung", exception.exceptionMessage)
            }
            addField("Exception Doc", exception.exceptionDoc ?: Emotes.LOADING)
            if (exception.causeClass != null) {
                addField(
                    "Ursache",
                    "Der Fehler befindet sich vermutlich in der Datei `${exception.causeClass?.limit(100)}.java` in Zeile `${exception.causeLine}`"
                )
            }

            if (exception.causee != null) {
                addField("Verursacht", exception.causee)
            }
        }

        if (causeSet) {
            addField(
                "Ursache - Code",
                causeContent
            )
        } else {
            addField(
                "Ursache - Code",
                "Ich konnte keinen Code finden, bitte schicke die komplette Klasse in der der Fehler auftritt (Am besten via hastebin)"
            )
        }

        if (npeHintSet) {
            addField("NPE Hinweis", npeHint)
        }

        footer("AutoHelp V1 BETA - Bitte Bugs auf GitHub.com/devcordde/DevcordBot melden.")
    }
}
