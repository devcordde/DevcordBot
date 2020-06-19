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

package com.github.devcordde.devcordbot.commands.general

import com.github.devcordde.devcordbot.command.AbstractCommand
import com.github.devcordde.devcordbot.command.CommandCategory
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.permission.PermissionState
import com.github.devcordde.devcordbot.constants.Embeds

/**
 * Help command.
 */
class HelpCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("help", "h", "hilfe")
    override val displayName: String = "help"
    override val description: String = "Zeigt eine Liste aller Befehle"
    override val usage: String = "[command]"
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.GENERAL
    override val commandPlace: CommandPlace = CommandPlace.ALL

    override suspend fun execute(context: Context) {
        val commandName = context.args.optionalArgument(0)
        if (commandName == null) {
            sendCommandList(context)
        } else {
            sendCommandHelpMessage(context, commandName)
        }
    }

    private fun sendCommandHelpMessage(context: Context, commandName: String) {
        val command = context.commandClient.commandAssociations[commandName.toLowerCase()]

        if (command == null || context.commandClient.permissionHandler.isCovered(
                command.permission,
                context.member,
                context.devCordUser,
                acknowledgeBlacklist = false
            ) != PermissionState.ACCEPTED
        ) {
            return context.respond(
                Embeds.error(
                    "Befehl nicht gefunden!",
                    "Es scheint für dich keinen Befehl mit diesem Namen zu geben."
                )
            ).queue()
        }

        if (!command.commandPlace.matches(context.message)) {
            return context.respond(
                Embeds.error(
                    "Falscher Context!",
                    "Der Command ist in diesem Channel nicht ausführbar."
                )
            ).queue()
        }

        context.respond(Embeds.command(command)).queue()
    }

    private fun sendCommandList(context: Context) {
        context.respond(
            Embeds.info(
                "Befehls-Hilfe", """Dies ist eine Liste aller Befehle, die du benutzen kannst,
            | um mehr über einen Befehl zu erfahren kannst du `sudo help [command]` ausführen
        """.trimMargin()
            ) {
                val commands = context.commandClient.registeredCommands.filter {
                    context.commandClient.permissionHandler.isCovered(
                        it.permission,
                        context.member,
                        context.devCordUser,
                        acknowledgeBlacklist = false // Ignore BL to save DB Query since BLed users cannot execute help anyways
                    ) == PermissionState.ACCEPTED && it.commandPlace.matches(context.message)
                }
                CommandCategory.values().forEach { category ->
                    val categoryCommands = commands.filter { it.category == category }.map { it.name }
                    if (categoryCommands.isNotEmpty()) {
                        addField(
                            category.displayName,
                            categoryCommands.joinToString(prefix = "`", separator = "`, `", postfix = "`")
                        )
                    }
                }
            }
        ).queue()
    }
}
