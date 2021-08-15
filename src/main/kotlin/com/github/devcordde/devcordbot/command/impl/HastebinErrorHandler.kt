/*
 * Copyright 2021 Daniel Scherf & Michael Rittmeister & Julian König
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
import com.github.devcordde.devcordbot.util.HastebinUtil
import dev.kord.core.behavior.channel.GuildChannelBehavior
import dev.kord.core.entity.channel.GuildChannel
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.TopGuildChannel
import kotlinx.coroutines.launch
import mu.KotlinLogging
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
        context: Context<*>,
        thread: Thread,
        coroutineContext: CoroutineContext?
    ) {
        logger.error(exception) { "An error occurred whilst command execution" }
        context.bot.launch {
            val message = context.respond(
                Embeds.error(
                    "Ein Fehler ist aufgetreten!",
                    "${Emotes.LOADING} Bitte warte einen Augenblick, während ich versuche mehr Informationen über den Fehler herauszufinden."
                )
            )

            val error = collectErrorInformation(exception, context, thread, coroutineContext)
            val url = HastebinUtil.postToHastebin(error, context.bot.httpClient)

            message.edit(
                Embeds.error(
                    "Es ist ein Fehler aufgetreten!",
                    "Bitte zeige einem Entwickler [diesen]($url) Link um Hilfe zu erhalten."
                )
            )
        }
    }
}

private suspend fun collectErrorInformation(
    e: Throwable,
    context: Context<*>,
    thread: Thread,
    coroutineContext: CoroutineContext?
): String {
    val information = StringBuilder()
    val channel = context.channel
    information.append("TextChannel: ").append('#').append(channel.asChannelOrNull()?.data?.name)
        .append('(').append(channel.id).appendLine(")")
    val guild = context.guild
    information.append("Guild: ").append(guild.asGuild().name).append('(').append(guild.id)
        .appendLine(')')
    val executor = context.author.asUser()
    information.append("Executor: ").append('@').append(executor.tag).append('(').append(executor.id).appendLine(')')
    val selfMember = guild.getMember(guild.kord.selfId)
    information.append("Permissions: ").appendLine(selfMember.getPermissions())

    if (context.channel is GuildChannelBehavior) {
        val guildChannel = context.channel.asChannel() as TopGuildChannel
        information.append("Channel permissions: ")
            .appendLine(guildChannel.getEffectivePermissions(selfMember.id))
    }

    information.append("Timestamp: ").appendLine(LocalDateTime.now())
    information.append("Thread: ").appendLine(thread)
    information.append("Coroutine: ").appendLine(coroutineContext)
    information.append("Stacktrace: ").appendLine().append(e.stackTraceToString())
    return information.toString()
}
