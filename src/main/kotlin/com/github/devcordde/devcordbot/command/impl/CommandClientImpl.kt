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

import com.github.devcordde.devcordbot.command.AbstractCommand
import com.github.devcordde.devcordbot.command.CommandClient
import com.github.devcordde.devcordbot.command.ErrorHandler
import com.github.devcordde.devcordbot.command.PermissionHandler
import com.github.devcordde.devcordbot.command.context.Arguments
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.permission.PermissionState
import com.github.devcordde.devcordbot.command.slashcommands.permissions.updatePermissions
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.core.DevCordBot
import com.github.devcordde.devcordbot.database.DatabaseDevCordUser
import com.github.devcordde.devcordbot.dsl.sendMessage
import com.github.devcordde.devcordbot.event.EventSubscriber
import com.github.devcordde.devcordbot.util.DefaultThreadFactory
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.commands.CommandHook
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.requests.RestAction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.Executors
import javax.annotation.CheckReturnValue
import kotlin.coroutines.CoroutineContext

/**
 * Default implementation of [CommandClient].
 * @param bot the current bot instance
 * @param prefix the prefix used for commands
 */
class CommandClientImpl(
    private val bot: DevCordBot,
    private val prefix: Regex,
    private val modRole: Long,
    private val adminRole: Long,
    private val botOwners: List<Long>,
    override val permissionHandler: PermissionHandler,
    override val executor: CoroutineContext =
        Executors.newFixedThreadPool(
            5,
            DefaultThreadFactory("CommandExecution")
        ).asCoroutineDispatcher()
) : CommandClient {

    private val logger = KotlinLogging.logger { }

    override val commandAssociations: MutableMap<String, AbstractCommand> = mutableMapOf()
    override val errorHandler: ErrorHandler = if (bot.debugMode) DebugErrorHandler() else HastebinErrorHandler()

    @CheckReturnValue
    fun updateCommands(): RestAction<Unit> {
        val commandUpdate = bot.guild.updateCommands()

        val commands = commandAssociations.values.distinct().map(AbstractCommand::toSlashCommand)
        commandUpdate.addCommands(commands)
        return commandUpdate
            .flatMap { bot.guild.retrieveCommands() }
            .flatMap {
                val registeredCommands =
                    it.map { registeredCommand -> registeredCommand to commandAssociations[registeredCommand.name]!! }

                val actions = registeredCommands.map { (slashCommand, command) ->
                    slashCommand.updatePermissions(bot.guild.id) {
                        addAll(command.myPermissions(botOwners, modRole, adminRole))
                    }
                }

                RestAction.allOf(actions)
            }
            .map { /* Unit */ }
    }

    /**
     * Consumer for slash command invocations.
     */
    @EventSubscriber
    fun onInteraction(event: SlashCommandEvent): Unit = dipatchSlashCommand(event)

    private fun dipatchSlashCommand(event: SlashCommandEvent) {
        if (!bot.isInitialized) return

        return parseCommand(event)
    }

    private fun parseCommand(event: SlashCommandEvent) {
        val command = resolveCommand(event) ?: return // No command found

        @Suppress("ReplaceNotNullAssertionWithElvisReturn") // Cannot be null in this case since it is send from a TextChannel
        val member =
            if (event.isFromGuild) event.member else bot.guild.getMemberById(event.user.id)

        val user = transaction { DatabaseDevCordUser.findOrCreateById(event.user.idLong) }

        val permissionState = permissionHandler.isCovered(
            command.permission,
            member,
            user,
            isSlashCommand = true
        )

        val arguments = Arguments(event.options.associateBy { it.name })
        event.acknowledge().queue { ack ->
            when (permissionState) {
                PermissionState.IGNORED -> return@queue
                PermissionState.DECLINED -> return@queue handleNoPermission(command.permission, ack)
                PermissionState.ACCEPTED -> {
                    if (!command.commandPlace.matches(event)) return@queue handleWrongContext(event.channel)
                    val context = Context(bot, command, arguments, event, this, user, ack)
                    processCommand(command, context)
                }
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

    private fun resolveCommand(event: SlashCommandEvent): AbstractCommand? {
        val rootCommandName = event.name
        val rootCommand = commandAssociations[rootCommandName] ?: return null
        val subCommandName = event.subcommandName
        if (subCommandName != null) {
            return rootCommand.commandAssociations[subCommandName]
        }
        return rootCommand
    }

    private fun handleWrongContext(channel: MessageChannel) {
        channel.sendMessage(
            Embeds.error(
                "Falscher Context!",
                "Der Command ist in diesem Channel nicht ausführbar."
            )
        ).queue()
    }

    private fun handleNoPermission(permission: Permission, ack: CommandHook) {
        ack.editOriginal(
            Embeds.error(
                "Keine Berechtigung!",
                "Du benötigst mindestens die $permission Berechtigung um diesen Befehl zu benutzen"
            ).toEmbedBuilder().build()
        ).queue()
    }
}
