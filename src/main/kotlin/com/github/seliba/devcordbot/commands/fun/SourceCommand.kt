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

package com.github.seliba.devcordbot.commands.`fun`

import com.github.seliba.devcordbot.command.AbstractCommand
import com.github.seliba.devcordbot.command.AbstractSubCommand
import com.github.seliba.devcordbot.command.CommandCategory
import com.github.seliba.devcordbot.command.context.Arguments
import com.github.seliba.devcordbot.command.context.Context
import com.github.seliba.devcordbot.command.permission.Permission
import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.util.hasSubCommands

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

    override suspend fun execute(context: Context) {
        val command = findCommand(context) ?: return context.respond(
            Embeds.info(
                "Quellcode:", "Den code vom Bot findest du [hier]($GITHUB_BASE)"
            )
        ).queue()
        val parentCommand = getParent(command)

        val definitionLine = command.callback.stackTrace[1].lineNumber

        @Suppress("ReplaceNotNullAssertionWithElvisReturn") // All command classes are not anonymous or local
        val classUrl =
            "$GITHUB_BASE$GITHUB_FILE_APPENDIX${parentCommand::class.qualifiedName!!.replace(".", "/")
                .replace(".", "/")}.kt#L$definitionLine"
        context.respond(
            Embeds.info(
                "${command.name} - Source",
                "Den Quellcode des Commands findest du hier: [$classUrl]($classUrl)"
            )
        ).queue()
    }

    private fun findCommand(context: Context): AbstractCommand? {
        tailrec fun find(
            args: Arguments,
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
        return find(context.args, 0)
    }

    private fun getParent(abstractCommand: AbstractCommand): AbstractCommand =
        if (abstractCommand is AbstractSubCommand) getParent(abstractCommand.parent) else abstractCommand

    companion object {
        private const val GITHUB_BASE = "https://github.com/Devcordde/devcordbot"
        private const val GITHUB_FILE_APPENDIX = "/tree/master/src/main/kotlin/"
    }
}
