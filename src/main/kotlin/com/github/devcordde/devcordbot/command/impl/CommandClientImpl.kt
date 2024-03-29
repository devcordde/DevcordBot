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

import com.github.devcordde.devcordbot.command.*
import com.github.devcordde.devcordbot.command.context.Arguments
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.context.ResponseStrategy
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.permission.PermissionState
import com.github.devcordde.devcordbot.command.root.AbstractRootCommand
import com.github.devcordde.devcordbot.command.root.AbstractSingleCommand
import com.github.devcordde.devcordbot.command.root.RegisterableCommand
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.core.DevCordBot
import com.github.devcordde.devcordbot.database.DatabaseDevCordUser
import com.github.devcordde.devcordbot.database.DevCordUser
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.EphemeralInteractionResponseBehavior
import dev.kord.core.behavior.interaction.InteractionResponseBehavior
import dev.kord.core.behavior.interaction.PublicInteractionResponseBehavior
import dev.kord.core.entity.Member
import dev.kord.core.entity.interaction.GroupCommand
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.core.entity.interaction.InteractionCommand
import dev.kord.core.entity.interaction.SubCommand
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
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
    override val executor: CoroutineContext = Dispatchers.IO + SupervisorJob()
) : CommandClient {

    init {
        bot.kord.registerCommandKiller()
    }

    private val logger = KotlinLogging.logger { }

    override val commandAssociations: MutableMap<String, AbstractCommand> = mutableMapOf()
    override val errorHandler: ErrorHandler = if (bot.debugMode) DebugErrorHandler() else HastebinErrorHandler()

    /**
     * Updates the slash commands definitions.
     */
    suspend fun updateCommands() {
        val guildId = bot.guild.id
        bot.kord.createGuildApplicationCommands(guildId) {
            commandAssociations.values.distinct().forEach {
                when (it) {
                    is RegisterableCommand -> with(it) { applyCommand() }
                    else -> error("Invalid command: $it")
                }
            }
        }
    }

    /**
     * Consumer for slash command invocations.
     */
    override fun Kord.onInteraction(): Job = on<InteractionCreateEvent> { dipatchSlashCommand(this) }

    private suspend fun dipatchSlashCommand(event: InteractionCreateEvent) {
        if (!bot.isInitialized) return

        return parseCommand(event)
    }

    private suspend fun parseCommand(event: InteractionCreateEvent) {
        val interaction = event.interaction as? GuildChatInputCommandInteraction ?: return
        val command = resolveCommand(interaction.command) ?: return // No command found
        val executableCommand = command as? ExecutableCommand<*> ?: error("$command is not executable")

        @Suppress("ReplaceNotNullAssertionWithElvisReturn") // Cannot be null in this case since it is sent from a TextChannel
        val member = interaction.member.asMember()

        val user = newSuspendedTransaction { DatabaseDevCordUser.findOrCreateById(interaction.user.id) }

        val permissionState = permissionHandler.isCovered(
            command.permission,
            member,
            user,
            isSlashCommand = true
        )

        val arguments = Arguments(interaction.command.options)

        executableCommand.run(event, permissionState, command, arguments, user, member)
    }

    private suspend fun <T : InteractionResponseBehavior> ExecutableCommand<T>.run(
        event: InteractionCreateEvent,
        permissionState: PermissionState,
        command: AbstractCommand,
        arguments: Arguments,
        user: DevCordUser,
        member: Member
    ) {
        val acknowledgement = event.acknowledge()
        val responseStrategy = when (acknowledgement) {
            is PublicInteractionResponseBehavior -> ResponseStrategy.PublicResponseStrategy(acknowledgement)
            is EphemeralInteractionResponseBehavior -> ResponseStrategy.EphemeralResponseStrategy(acknowledgement)
            else -> error("Unexpected type of acknowledgement: $acknowledgement")
        }

        when (permissionState) {
            PermissionState.IGNORED -> return
            PermissionState.DECLINED -> return handleNoPermission(command.permission, responseStrategy)
            PermissionState.ACCEPTED -> {
                val context =
                    Context(
                        bot,
                        command,
                        arguments,
                        event,
                        this@CommandClientImpl,
                        user,
                        acknowledgement,
                        responseStrategy,
                        member
                    )
                if (!command.commandPlace.matches(event)) return handleWrongContext(context)
                process(command, context)
            }
        }
    }

    private fun <T : InteractionResponseBehavior> ExecutableCommand<T>.process(
        command: AbstractCommand,
        context: Context<T>
    ) {
        val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            errorHandler.handleException(throwable, context, Thread.currentThread(), coroutineContext)
        }
        bot.launch(executor + exceptionHandler) {
            logger.info { "Command $command was executed by ${context.member}" }
            when (command) {
                is AbstractSingleCommand<*> -> execute(context)
                is AbstractSubCommand.Command<*> -> execute(context)
            }
        }
    }

    private fun resolveCommand(rootInteractionCommand: InteractionCommand): AbstractCommand? {
        val rootCommandName = rootInteractionCommand.rootName
        val rootCommand = commandAssociations[rootCommandName] ?: return null
        if (rootCommand is AbstractSingleCommand<*>) return rootCommand
        val command = rootCommand as AbstractRootCommand
        val base = if (rootInteractionCommand is GroupCommand) {
            (command.commandAssociations[rootInteractionCommand.name] as AbstractSubCommand.Group).commandAssociations
        } else {
            rootCommand.commandAssociations
        }
        val subCommandName = (rootInteractionCommand as? SubCommand)?.name
        if (subCommandName != null) {
            return base[subCommandName]
        }
        return rootCommand
    }

    private suspend fun handleWrongContext(context: Context<*>) {
        context.respond(
            Embeds.error(
                "Falscher Context!",
                "Der Command ist in diesem Channel nicht ausführbar."
            )
        )
    }

    private suspend fun handleNoPermission(permission: Permission, responseStrategy: ResponseStrategy) {
        responseStrategy.respond {
            embeds.add(
                Embeds.error(
                    "Keine Berechtigung!",
                    "Du benötigst mindestens die $permission Berechtigung um diesen Befehl zu benutzen"
                )
            )
        }
    }
}
