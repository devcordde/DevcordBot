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

package com.github.seliba.devcordbot.command.impl

import com.github.seliba.devcordbot.command.AbstractCommand
import com.github.seliba.devcordbot.command.CommandClient
import com.github.seliba.devcordbot.command.ErrorHandler
import com.github.seliba.devcordbot.command.PermissionHandler
import com.github.seliba.devcordbot.command.context.Arguments
import com.github.seliba.devcordbot.command.context.Context
import com.github.seliba.devcordbot.command.permission.Permission
import com.github.seliba.devcordbot.command.permission.PermissionState
import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.core.DevCordBot
import com.github.seliba.devcordbot.dsl.sendMessage
import com.github.seliba.devcordbot.event.EventSubscriber
import com.github.seliba.devcordbot.util.DefaultThreadFactory
import com.github.seliba.devcordbot.util.asMention
import com.github.seliba.devcordbot.util.asNickedMention
import com.github.seliba.devcordbot.util.hasSubCommands
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import java.time.Duration
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
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

    override val commandAssociations: MutableMap<String, AbstractCommand> = mutableMapOf()
    override val errorHandler: ErrorHandler = if (bot.debugMode) DebugErrorHandler() else HastebinErrorHandler()

    /**
     * Listens for new private messages
     */
    @EventSubscriber
    fun onPrivateMessage(event: PrivateMessageReceivedEvent): Unit = dispatchPrivateMessageCommand(event.message)

    private fun dispatchPrivateMessageCommand(message: Message) {
        if (!bot.isInitialized) return

        val author = message.author
        if (message.isWebhookMessage or author.isBot or author.isFake) return

        return parseCommand(message)
    }

    /**
     * Listens for message updates.
     */
    @EventSubscriber
    fun onMessageEdit(event: GuildMessageUpdateEvent) {
        if (Duration.between(event.message.timeCreated, OffsetDateTime.now()) > Duration.of(
                30,
                ChronoUnit.SECONDS
            )
        ) return
        dispatchGuildCommand(event.message)
    }

    /**
     * Listens for new messages.
     */
    @EventSubscriber
    fun onMessage(event: GuildMessageReceivedEvent): Unit = dispatchGuildCommand(event.message)

    private fun dispatchGuildCommand(message: Message) {
        if (!bot.isInitialized) return

        if (message.guild.id != bot.guild.id) return

        val author = message.author
        if (message.isWebhookMessage or author.isBot or author.isFake) return

        return parseCommand(message)
    }

    private fun parseCommand(message: Message) {
        val nonPrefixedInput = stripPrefix(message) ?: return

        val (command, arguments) = resolveCommand(nonPrefixedInput) ?: return // No command found

        @Suppress("ReplaceNotNullAssertionWithElvisReturn") // Cannot be null in this case since it is send from a TextChannel
        val member =
            if (message.isFromType(ChannelType.TEXT)) message.member else bot.guild.getMemberById(message.author.id)
        val permissionState = permissionHandler.isCovered(
            command.permission,
            member
        )

        when (permissionState) {
            PermissionState.IGNORED -> return
            PermissionState.DECLINED -> return handleNoPermission(command.permission, message.channel)
            PermissionState.ACCEPTED -> {
                if (!command.commandPlace.matches(message)) return handleWrongContext(message.channel)
                message.channel.sendTyping()
                    .queue(fun(_: Void?) { // Since Void has a private constructor JDA passes in null, so it has to be nullable even if it is not used
                        val context = Context(bot, command, arguments, message, this)
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
            val invoke = arguments.first()
            // Search command associated with invoke or return previously found command
            val foundCommand = associations[invoke] ?: return command?.let { CommandContainer(it, arguments) }
            // Cut off invoke
            val newArgs = Arguments(arguments.drop(1), raw = arguments.join().substring(invoke.length).trim())
            // Look for sub commands
            if (foundCommand.hasSubCommands() and newArgs.isNotEmpty()) {
                return findCommand(newArgs, foundCommand.commandAssociations, foundCommand)
            }
            // Return command if now sub-commands were found
            return CommandContainer(foundCommand, newArgs)
        }

        return findCommand(Arguments(input.trim().split(delimiter), raw = input), commandAssociations)
    }

    private fun stripPrefix(message: Message): String? {
        val rawInput = message.contentRaw

        when (message.channelType) {
            ChannelType.TEXT -> {
                val prefixLength = resolvePrefix(message.guild, rawInput) ?: return null
                return rawInput.substring(prefixLength).trim()
            }
            ChannelType.PRIVATE -> {
                val prefix = prefix.find(rawInput) ?: return null
                return rawInput.substring(prefix.range.last + 1).trim()
            }
            else -> {
                return null
            }
        }
    }

    private fun resolvePrefix(guild: Guild, content: String): Int? {
        val mention = guild.selfMember.asMention()
        val nickedMention = guild.selfMember.asNickedMention()

        val prefix = prefix.find(content)
        return when {
            content.startsWith(mention) -> mention.length
            content.startsWith(nickedMention) -> nickedMention.length
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
}
