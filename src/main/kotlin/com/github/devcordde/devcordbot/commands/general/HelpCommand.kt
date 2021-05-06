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

import com.github.devcordde.devcordbot.command.AbstractSingleCommand
import com.github.devcordde.devcordbot.command.CommandCategory
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.permission.PermissionState
import com.github.devcordde.devcordbot.constants.Embeds
import dev.kord.rest.builder.interaction.ApplicationCommandCreateBuilder
import java.util.*

/**
 * Help command.
 */
class HelpCommand : AbstractSingleCommand() {
    override val name: String = "help"
    override val description: String = "Zeigt eine Liste aller Befehle."
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.GENERAL
    override val commandPlace: CommandPlace = CommandPlace.ALL

    override fun ApplicationCommandCreateBuilder.applyOptions() {
        string("command", "Der Name eines Befehls, für den Hilfe angezeigt werden soll")
    }

    override suspend fun execute(context: Context) {
        val commandName = context.args.optionalString("command")
        if (commandName == null) {
            sendCommandList(context)
        } else {
            sendCommandHelpMessage(context, commandName)
        }
    }

    private suspend fun sendCommandHelpMessage(context: Context, commandName: String) {
        val command = context.commandClient.commandAssociations[commandName.lowercase(Locale.getDefault())]

        if (command == null || context.commandClient.permissionHandler.isCovered(
                command.permission,
                context.member,
                context.devCordUser,
                acknowledgeBlacklist = false
            ) != PermissionState.ACCEPTED
        ) {
            context.respond(
                Embeds.error(
                    "Befehl nicht gefunden!",
                    "Es scheint für dich keinen Befehl mit diesem Namen zu geben."
                )
            )
            return
        }

        if (!command.commandPlace.matches(context.event)) {
            context.respond(
                Embeds.error(
                    "Falscher Ort!",
                    "Der Command ist in diesem Kanal nicht ausführbar."
                )
            )
            return
        }

        context.respond(Embeds.command(command))
    }

    private suspend fun sendCommandList(context: Context) {
        context.respond(
            Embeds.info(
                "Befehls-Hilfe",
                """Dies ist eine Liste aller Befehle, die du benutzen kannst,
            | um mehr über einen Befehl zu erfahren, kannst du `sudo help [command]` ausführen.
        """.trimMargin()
            ) {
                val commands = context.commandClient.registeredCommands.filter {
                    context.commandClient.permissionHandler.isCovered(
                        it.permission,
                        context.member,
                        context.devCordUser,
                        acknowledgeBlacklist = false // Ignore BL to save DB Query since BLed users cannot execute help anyways
                    ) == PermissionState.ACCEPTED && it.commandPlace.matches(context.event)
                }
                CommandCategory.values().forEach { category ->
                    val categoryCommands = commands.filter { it.category == category }.map { it.name }
                    if (categoryCommands.isNotEmpty()) {
                        field {
                            name = category.displayName
                            value =
                                categoryCommands.joinToString(prefix = "`", separator = "`, `", postfix = "`")
                        }
                    }
                }
            }
        )
    }
}
