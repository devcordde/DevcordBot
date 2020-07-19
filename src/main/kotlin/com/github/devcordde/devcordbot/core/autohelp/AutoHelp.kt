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

import com.github.devcordde.devcordbot.core.DevCordBot
import com.github.devcordde.devcordbot.database.DatabaseDevCordUser
import com.github.devcordde.devcordbot.event.DevCordGuildMessageReceivedEvent
import com.github.devcordde.devcordbot.event.EventSubscriber
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import org.jetbrains.exposed.sql.transactions.transaction
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
    private val brain = Brain(knownLanguages, bot.httpClient, maxLines, bot.googler)

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

        val inputs = fetcher.fetchMessageContents(event.message)
        val conversation by lazy { brain.findConversation(event) }

        for (future in inputs) {
            val userInput = future.await()
            userInput.forEach {
                if (it != null) {
                    JVM_EXCEPTION_PATTERN.findAll(it).forEach { match ->
                        val (_, name, message) = match.groupValues
                        val elementsRaw = match.groupValues[3]
                        val elements = STACK_TRACE_ELEMENT_PATTERN.findAll(elementsRaw).map { elementMatch ->
                            val (_, pakage, method, className, line) = elementMatch.groupValues
                            StackTraceElement(pakage, method, className, line.toInt())
                        }.toList()
                        brain.determinedException(conversation, StackTrace(name, message, elements))
                    }

                    JAVA_CLASS_PATTERN.findAll(it).forEach { match ->
                        val (_, pakage, _, name) = match.groupValues

                        brain.determinedClass(conversation, Class(pakage, name, it))
                    }
                }
            }
        }
    }

    companion object {
        // https://regex101.com/r/vgz86r/16
        val JVM_EXCEPTION_PATTERN =
            """(?m)^(?:Exception in thread ".*")?.*?(.+?(?<=Exception|Error:))(?:\: )?(.*)((?:\R+^\s*.*)?(?:\R+^.*at .*)+)""".toRegex()

        // https://regex101.com/r/HtaGF8/1
        val JVM_EXCEPTION_NAME_PATTERN =
            """(?m)^(?:Exception in thread ".*")?.*?(.+?(?<=Exception|Error))(?:\: )(.*)(?:\R+^\s*.*)?""".toRegex()

        // https://regex101.com/r/xYGH0m/1
        val STACK_TRACE_ELEMENT_PATTERN = "at ((?:(?:\\w)+\\.?)+)\\.(\\w+)\\((\\w+).java:([0-9]+)\\)".toRegex()

        // Java class pattern (should match kotlin (maybe))
        // https://regex101.com/r/uksxY5/3
        val JAVA_CLASS_PATTERN =
            """(?m)package ((?:\w+\.?)+);[\s\S]*(public|private|protected) class (\w*) \{([\s\S]*)}""".toRegex()

    }
}
