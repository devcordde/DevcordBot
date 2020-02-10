/*
 * Copyright 2020 Daniel Scherf & Michael Rittmeister
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
import com.github.seliba.devcordbot.command.perrmission.Permissions
import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.core.DevCordBot
import com.github.seliba.devcordbot.dsl.sendMessage
import com.github.seliba.devcordbot.event.EventSubscriber
import com.github.seliba.devcordbot.util.DefaultThreadFactory
import com.github.seliba.devcordbot.util.hasSubCommands
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

/**
 * Default implementation of [CommandClient].
 * @param bot the current bot instance
 * @param prefix the prefix used for commands
 */
class CommandClientImpl(
    private val bot: DevCordBot, prefix: Regex, override val executor: CoroutineContext =
        Executors.newFixedThreadPool(
            5,
            DefaultThreadFactory("CommandExecution")
        ).asCoroutineDispatcher()
) : CommandClient {

    private val logger = KotlinLogging.logger { }
    private val delimiter = "\\s+".toRegex()
    private val prefix = prefix.toPattern()

    override val permissionHandler: PermissionHandler = RolePermissionHandler()
    override val commandAssociations: MutableMap<String, AbstractCommand> = mutableMapOf()

    override val errorHandler: ErrorHandler = HastebinErrorHandler()

    /**
     * Listens for message updates.
     */
    @EventSubscriber
    fun onMessageEdit(event: GuildMessageUpdateEvent): Unit = dispatchCommand(event.message)

    /**
     * Listens for new messages.
     */
    @EventSubscriber
    fun onMessage(event: GuildMessageReceivedEvent): Unit = dispatchCommand(event.message)

    private fun dispatchCommand(message: Message) {
        if (!bot.isInitialized) return
        val author = message.author

        if (message.isWebhookMessage or author.isBot or author.isFake) return

        return parseCommand(message)
    }

    private fun parseCommand(message: Message) {
        val rawInput = message.contentRaw
        val prefix = resolvePrefix(message.guild, rawInput) ?: return

        val nonPrefixedInput = rawInput.substring(prefix.length)

        val rawArgs = nonPrefixedInput.trim().split(delimiter)

        if (rawArgs.isEmpty()) return // No command provided

        val (command, arguments) = resolveCommand(rawArgs) ?: return // No command found

        message.textChannel.sendTyping().queue()
        @Suppress("ReplaceNotNullAssertionWithElvisReturn") // Cannot be null in this case since it is send from a TextChannel
        if (!permissionHandler.isCovered(
                command.permissions,
                message.member!!
            )
        ) return handleNoPermission(command.permissions, message.textChannel)

        val context = Context(bot, command, arguments, message, this)

        processCommand(command, context)
    }

    private fun processCommand(command: AbstractCommand, context: Context) {
        logger.info { "Command $command was executed by ${context.member}" }
        val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            errorHandler.handleException(throwable, context, Thread.currentThread(), coroutineContext)
        }
        GlobalScope.launch(executor + exceptionHandler) {
            command.execute(context)
        }
    }

    private fun resolveCommand(rawArgs: List<String>): CommandContainer? {
        tailrec fun findCommand(
            args: List<String>,
            associations: Map<String, AbstractCommand>,
            command: AbstractCommand? = null
        ): Pair<AbstractCommand, List<String>>? {
            // Get invoke
            val invoke = args.first().toLowerCase()
            // Search command associated with invoke or return previously found command
            val foundCommand = associations[invoke] ?: return command?.to(args)
            // Cut off invoke
            val newArgs = if (args.size > 1) args.subList(1, args.size) else emptyList()
            // Look for sub commands
            if (foundCommand.hasSubCommands() and newArgs.isNotEmpty()) {
                return findCommand(newArgs, foundCommand.commandAssociations, foundCommand)
            }
            // Return command if now sub-commands were found
            return foundCommand to newArgs
        }

        val (command, args) = findCommand(rawArgs, commandAssociations) ?: return null
        return CommandContainer(command, Arguments(args))
    }

    private fun resolvePrefix(guild: Guild, content: String): String? {
        val mention = guild.selfMember.asMention
        val matcher = prefix.matcher(content)
        return when {
            content.startsWith(mention) -> mention
            matcher.matches() -> matcher.group(1)
            else -> null
        }
    }

    private fun handleNoPermission(permissions: Permissions, channel: TextChannel) {
        channel.sendMessage(
            Embeds.error(
                "Keine Berechtigung!",
                "Du benötigst mindestens die $permissions Berechtigung um diesen Befehl zu benutzen"
            )
        ).queue()
    }

    private data class CommandContainer(val command: AbstractCommand, val args: Arguments)
}
