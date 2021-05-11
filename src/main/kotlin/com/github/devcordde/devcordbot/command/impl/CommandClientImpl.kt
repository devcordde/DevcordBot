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

import com.github.devcordde.devcordbot.command.AbstractCommand
import com.github.devcordde.devcordbot.command.CommandClient
import com.github.devcordde.devcordbot.command.ErrorHandler
import com.github.devcordde.devcordbot.command.PermissionHandler
import com.github.devcordde.devcordbot.command.context.Arguments
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.permission.PermissionState
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.core.DevCordBot
import com.github.devcordde.devcordbot.dsl.sendMessage
import com.github.devcordde.devcordbot.event.*
import com.github.devcordde.devcordbot.util.DefaultThreadFactory
import com.github.devcordde.devcordbot.util.asMention
import com.github.devcordde.devcordbot.util.hasSubCommands
import kotlinx.coroutines.*
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import java.time.Duration
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import javax.annotation.Nonnull
import kotlin.coroutines.CoroutineContext

/**
 * Default implementation of [CommandClient].
 * @param bot the current bot instance
 * @param prefix the prefix used for commands
 */
class CommandClientImpl(
    private val bot: DevCordBot,
    private val prefix: Regex,
    override val permissionHandler: PermissionHandler,
    override val executor: CoroutineContext =
        Executors.newFixedThreadPool(
            5,
            DefaultThreadFactory("CommandExecution")
        ).asCoroutineDispatcher()
) : CommandClient {

    private val logger = KotlinLogging.logger { }
    private val delimiter = "\\s+".toRegex()
    private val messageStorage = mutableMapOf<Long, Pair<Long, Long>>()
    private val responsePool = Executors.newSingleThreadExecutor(
        DefaultThreadFactory("CommandResponseWatcher")
    ).asCoroutineDispatcher()
    private val editThreshold = Duration.of(
        30,
        ChronoUnit.SECONDS
    )

    override val commandAssociations: MutableMap<String, AbstractCommand> = mutableMapOf()
    override val errorHandler: ErrorHandler = if (bot.debugMode) DebugErrorHandler() else HastebinErrorHandler()

    internal fun acknowledgeResponse(key: Long, channelId: Long, messageId: Long) {
        messageStorage[key] = channelId to messageId
        GlobalScope.launch(responsePool) {
            delay(editThreshold.toMillis())
            messageStorage.remove(key)
        }
    }


    /**
     * Deletes old command response
     */
    @EventSubscriber
    fun onMessageDelete(event: GuildMessageDeleteEvent): Boolean = deleteOldMessage(event.messageIdLong, event.guild)

    /**
     * Listens for new private messages
     */
    @EventSubscriber
    fun onPrivateMessage(event: DevCordMessageReceivedEvent): Unit = dispatchPrivateMessageCommand(event.message, event)

    private fun dispatchPrivateMessageCommand(message: Message, event: Event) {
        if (!bot.isInitialized) return

        val author = message.author
        if (message.isWebhookMessage or author.isBot) return

        bot.guild.getMemberById(author.id) ?: return

        return parseCommand(message, event)
    }

    /**
     * Listens for message updates.
     */
    @EventSubscriber
    fun onMessageEdit(event: DevCordGuildMessageEditEvent) {
        if (Duration.between(event.message.timeCreated, OffsetDateTime.now()) > editThreshold
        ) return
        dispatchGuildCommand(event.message, event)
        deleteOldMessage(event.messageIdLong, event.guild)
    }

    /**
     * Listens for new messages.
     */
    @EventSubscriber
    fun onMessage(event: DevCordGuildMessageReceivedEvent): Unit = dispatchGuildCommand(event.message, event)

    private fun dispatchGuildCommand(message: Message, event: Event) {
        if (!bot.isInitialized) return

        if (message.guild.id != bot.guild.id) return

        val author = message.author
        if (message.isWebhookMessage or author.isBot) return

        return parseCommand(message, event)
    }

    private fun parseCommand(message: Message, event: Event) {
        val content = message.contentRaw
        val prefixLength =
            resolvePrefix(if (message.channelType == ChannelType.TEXT) message.guild else null, content)
                ?: return
        val nonPrefixedInput = content.substring(prefixLength)

        val (command, arguments) = resolveCommand(nonPrefixedInput) ?: return // No command found

        @Suppress("ReplaceNotNullAssertionWithElvisReturn") // Cannot be null in this case since it is send from a TextChannel
        val member =
            if (message.isFromType(ChannelType.TEXT)) message.member else bot.guild.getMemberById(message.author.id)

        val user = event.devCordUser

        val permissionState = permissionHandler.isCovered(
            command.permission,
            member,
            user
        )


        when (permissionState) {
            PermissionState.IGNORED -> return
            PermissionState.DECLINED -> return handleNoPermission(command.permission, message.channel)
            PermissionState.ACCEPTED -> {
                if (!command.commandPlace.matches(message)) return handleWrongContext(message.channel)
                message.channel.sendTyping()
                    .queue(fun(_: Void?) { // Since Void has a private constructor JDA passes in null, so it has to be nullable even if it is not used
                        val context = Context(bot, command, arguments, message, this, user)
                        processCommand(command, context)
                    })
            }
        }
    }

    private fun processCommand(command: AbstractCommand, context: Context) {
        val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            errorHandler.handleException(throwable, context, Thread.currentThread(), coroutineContext)
        }
        GlobalScope.launch(executor + exceptionHandler) {
            logger.info { "Command $command was executed by ${context.member}" }
            command.execute(context)
        }
    }

    private fun resolveCommand(input: String): CommandContainer? {
        tailrec fun findCommand(
            arguments: Arguments,
            associations: Map<String, AbstractCommand>,
            command: AbstractCommand? = null
        ): CommandContainer? {
            // Get invoke
            val invoke = arguments.first().toLowerCase()
            // Search command associated with invoke or return previously found command
            val foundCommand = associations[invoke] ?: return command?.let { CommandContainer(it, arguments) }
            // Cut off invoke
            val newArgs = Arguments(arguments.drop(1), raw = arguments.join().trim().substring(invoke.length).trim())
            // Look for sub commands
            if (foundCommand.hasSubCommands() and newArgs.isNotEmpty()) {
                return findCommand(newArgs, foundCommand.commandAssociations, foundCommand)
            }
            // Return command if now sub-commands were found
            return CommandContainer(foundCommand, newArgs)
        }

        return findCommand(Arguments(input.trim().split(delimiter), raw = input), commandAssociations)
    }

    private fun resolvePrefix(guild: Guild?, content: String): Int? {
        val mention = guild?.selfMember?.asMention()

        val mentionPrefix = mention?.find(content)
        val prefix = prefix.find(content)
        return when {
            mentionPrefix?.range?.first == 0 -> mentionPrefix.range.last
            prefix != null -> prefix.range.last + 1
            else -> null
        }
    }

    private fun handleWrongContext(channel: MessageChannel) {
        channel.sendMessage(
            Embeds.error(
                "Falscher Context!",
                "Der Command ist in diesem Channel nicht ausführbar."
            )
        ).queue()
    }

    private fun handleNoPermission(permission: Permission, channel: MessageChannel) {
        channel.sendMessage(
            Embeds.error(
                "Keine Berechtigung!",
                "Du benötigst mindestens die $permission Berechtigung um diesen Befehl zu benutzen"
            )
        ).queue()
    }

    private data class CommandContainer(val command: AbstractCommand, val args: Arguments)

    private fun deleteOldMessage(
        messageIdLong: Long,
        guild: @Nonnull Guild
    ): Boolean {
        val (channel, message) = messageStorage[messageIdLong] ?: return true
        guild.getTextChannelById(channel)?.retrieveMessageById(message)?.flatMap(Message::delete)?.queue()
        return false
    }
}
