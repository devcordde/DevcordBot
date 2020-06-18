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

package com.github.devcordde.devcordbot.command.impl

import com.github.devcordde.devcordbot.command.ErrorHandler
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.constants.Emotes
import com.github.devcordde.devcordbot.dsl.editMessage
import com.github.devcordde.devcordbot.util.HastebinUtil
import com.github.devcordde.devcordbot.util.stringify
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.ChannelType
import java.time.LocalDateTime
import kotlin.coroutines.CoroutineContext

/**
 * Implementation of [ErrorHandler] that reports an error log to hastebin.
 */
class HastebinErrorHandler : ErrorHandler {
    private val logger = KotlinLogging.logger { }

    /**
     * @see ErrorHandler.handleException
     */
    override fun handleException(
        exception: Throwable,
        context: Context,
        thread: Thread,
        coroutineContext: CoroutineContext?
    ) {
        logger.error(exception) { "An error occurred whilst command execution" }
        context.respond(
            Embeds.error(
                "Ein Fehler ist aufgetreten!",
                "${Emotes.LOADING} Bitte warte einen Augenblick, während ich versuche mehr Informationen über den Fehler herauszufinden."
            )
        ).submit().thenCompose { message ->
            val error = collectErrorInformation(exception, context, thread, coroutineContext)
            HastebinUtil.postErrorToHastebin(error, context.jda.httpClient).thenApply { it to message }
        }.thenAccept { (url, message) ->
            message.editMessage(
                Embeds.error(
                    "Es ist ein Fehler aufgetreten!",
                    "Bitte zeige einem Entwickler [diesen]($url) Link um Hilfe zu erhalten."
                )
            ).queue()
        }
    }

    private fun collectErrorInformation(
        e: Throwable,
        context: Context,
        thread: Thread,
        coroutineContext: CoroutineContext?
    ): String {
        val information = StringBuilder()
        val channel = context.channel
        information.append("TextChannel: ").append('#').append(channel.name)
            .append('(').append(channel.id).appendln(")")
        val guild = context.guild
        information.append("Guild: ").append(guild.name).append('(').append(guild.id)
            .appendln(')')
        val executor = context.author
        information.append("Executor: ").append('@').append(executor.name).append('#')
            .append(executor.discriminator).append('(').append(executor.id).appendln(')')
        val selfMember = guild.selfMember
        information.append("Permissions: ").appendln(selfMember.permissions)

        if (context.message.channelType == ChannelType.TEXT) {
            information.append("Channel permissions: ").appendln(selfMember.getPermissions(context.message.textChannel))
        }

        information.append("Timestamp: ").appendln(LocalDateTime.now())
        information.append("Thread: ").appendln(thread)
        information.append("Coroutine: ").appendln(coroutineContext)
        information.append("Stacktrace: ").appendln().append(e.stringify())
        return information.toString()
    }
}
