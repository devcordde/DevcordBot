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

package com.github.devcordde.devcordbot.commands.`fun`

import com.github.devcordde.devcordbot.command.*
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.root.AbstractSingleCommand
import com.github.devcordde.devcordbot.constants.Embeds
import dev.kord.core.behavior.interaction.InteractionResponseBehavior
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.rest.builder.interaction.ApplicationCommandCreateBuilder

/**
 * Source command.
 */
class SourceCommand : AbstractSingleCommand<InteractionResponseBehavior>() {
    override val name: String = "source"
    override val description: String = "Zeigt den Quellcode des Bots an."
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.FUN
    override val commandPlace: CommandPlace = CommandPlace.ALL

    override fun ApplicationCommandCreateBuilder.applyOptions() {
        string("command", "Der Name des Befehls, für den der Quellcode angezeigt werden soll")
    }

    override suspend fun InteractionCreateEvent.acknowledge(): InteractionResponseBehavior =
        interaction.ackowledgePublic()

    override suspend fun execute(context: Context<InteractionResponseBehavior>) {
        val commandName = context.args.optionalString("command")
        val command = commandName?.let { findCommand(it, context) } ?: return run {
            context.respond(
                Embeds.info(
                    "Quellcode", "Den Code vom Bot findest du [hier]($GITHUB_BASE)"
                )
            )
        }
        val parentCommand = getParent(command)

        val definitionLine = command.callback.stackTrace[1].lineNumber

        @Suppress("ReplaceNotNullAssertionWithElvisReturn") // All command classes are not anonymous or local
        val classUrl =
            "$GITHUB_BASE$GITHUB_FILE_APPENDIX${
                parentCommand::class.qualifiedName!!.replace(".", "/")
                    .replace(".", "/")
            }.kt#L$definitionLine"
        context.respond(
            Embeds.info(
                "${command.name} - Source",
                "Den Quellcode des Befehls findest du hier: [$classUrl]($classUrl)"
            )
        )
    }

    private fun findCommand(argument: String, context: Context<InteractionResponseBehavior>): AbstractCommand? {
        tailrec fun find(
            args: List<String>,
            index: Int,
            commandAssociations: Map<String, AbstractCommand> = context.commandClient.commandAssociations
        ): AbstractCommand? {
            val current = args.getOrNull(index)
            val currentCommand = commandAssociations[current]
            val next = args.getOrNull(index + 1)
            val childAssociations = currentCommand as? CommandRegistry<*>
            if (childAssociations?.isNotEmpty() == true && next != null && childAssociations.commandAssociations.containsKey(
                    next
                )
            )
                return find(args, index + 1, childAssociations.commandAssociations)
            return currentCommand
        }

        return find(argument.split("\\s+".toRegex()), 0)
    }

    private fun getParent(abstractCommand: AbstractCommand): AbstractCommand =
        if (abstractCommand is AbstractSubCommand) getParent(abstractCommand.parent) else abstractCommand

    companion object {
        private const val GITHUB_BASE = "https://github.com/Devcordde/devcordbot"
        private const val GITHUB_FILE_APPENDIX = "/tree/master/src/main/kotlin/"
    }
}
