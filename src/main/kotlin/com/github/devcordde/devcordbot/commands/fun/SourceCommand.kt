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

package com.github.devcordde.devcordbot.commands.`fun`

import com.github.devcordde.devcordbot.command.AbstractCommand
import com.github.devcordde.devcordbot.command.AbstractSubCommand
import com.github.devcordde.devcordbot.command.CommandCategory
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.util.hasSubCommands
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction

/**
 * Source command.
 */
class SourceCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("source", "skid", "code")
    override val displayName: String = "source"
    override val description: String = "Displays the source code of the bot"
    override val usage: String = "[command]"
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.FUN
    override val commandPlace: CommandPlace = CommandPlace.ALL

    override val options: List<CommandUpdateAction.OptionData> = buildOptions {
        string("command", "Der Name des Commands für den der SourceCode angezeigt werden soll")
    }

    override suspend fun execute(context: Context) {
        val commandName = context.args.optionalString("command")
        val command = commandName?.let { findCommand(it, context) } ?: return context.respond(
            Embeds.info(
                "Quellcode:", "Den code vom Bot findest du [hier]($GITHUB_BASE)"
            )
        ).queue()
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
                "Den Quellcode des Commands findest du hier: [$classUrl]($classUrl)"
            )
        ).queue()
    }

    private fun findCommand(argument: String, context: Context): AbstractCommand? {
        tailrec fun find(
            args: List<String>,
            index: Int,
            commandAssociations: Map<String, AbstractCommand> = context.commandClient.commandAssociations
        ): AbstractCommand? {
            val current = args.getOrNull(index)
            val currentCommand = commandAssociations[current]
            val next = args.getOrNull(index + 1)
            if (currentCommand?.hasSubCommands() == true && next != null && currentCommand.commandAssociations.containsKey(
                    next
                )
            )
                return find(args, index + 1, currentCommand.commandAssociations)
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
