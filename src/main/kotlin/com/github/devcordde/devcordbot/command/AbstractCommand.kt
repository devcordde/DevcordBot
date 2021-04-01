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

package com.github.devcordde.devcordbot.command

import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.slashcommands.OptionsBuilder
import com.github.devcordde.devcordbot.command.slashcommands.permissions.DiscordApplicationCommandPermission
import com.github.devcordde.devcordbot.command.slashcommands.permissions.PermissiveCommandData

import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction

/**
 * Skeleton of a command.
 * @property aliases list of strings that invoke the command
 * @property name the name used in usage messages
 * @property displayName name that is used on help messages
 * @property description the description of the command
 * @property usage the full usage of the command
 * @property permission the command permissions
 * @property commandAssociations all alias-command associations of sub-commands
 * @property category the [CommandCategory] of the command
 * @property commandPlace th [CommandPlace] of the command
 * @property callback an [Exception] that is supposed to highlight class defention line
 */
abstract class AbstractCommand : CommandRegistry<AbstractSubCommand> {
    open val callback: Exception = Exception()

    override val commandAssociations: MutableMap<String, AbstractSubCommand> = mutableMapOf()

    abstract val aliases: List<String>
    val name: String
        get() = aliases.first()
    abstract val displayName: String
    abstract val description: String
    abstract val usage: String
    abstract val permission: Permission
    abstract val category: CommandCategory
    abstract val commandPlace: CommandPlace
    open val options: List<CommandUpdateAction.OptionData> = emptyList()

    /**
     * Invokes the command.
     * @param context the [Context] in which the command is invoked
     */
    abstract suspend fun execute(context: Context)

    internal fun toSlashCommand(): CommandUpdateAction.CommandData {
        try {
            val command = PermissiveCommandData(
                name, description
            )
            command.defaultPermission = permission == Permission.ANY
            options.forEach(command::addOption)
            commandAssociations.values
                .asSequence()
                .distinct()
                .map(AbstractSubCommand::toSubSlashCommand)
                .forEach(command::addSubcommand)

            return command
        } catch (e: Exception) {
            throw IllegalStateException(
                "Could not process command with name $name",
                e
            )
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun myPermissions(botOwners: List<Long>, modId: Long, adminId: Long): List<DiscordApplicationCommandPermission> =
        when (permission) {
            Permission.ANY -> emptyList()
            Permission.BOT_OWNER -> botOwners.map {
                DiscordApplicationCommandPermission(
                    it,
                    DiscordApplicationCommandPermission.Type.USER,
                    true
                )
            }
            Permission.MODERATOR -> listOf(
                DiscordApplicationCommandPermission(
                    modId,
                    DiscordApplicationCommandPermission.Type.ROLE,
                    true
                )
            )
            Permission.ADMIN -> listOf(
                DiscordApplicationCommandPermission(
                    modId,
                    DiscordApplicationCommandPermission.Type.ROLE,
                    true
                ),
                DiscordApplicationCommandPermission(
                    adminId,
                    DiscordApplicationCommandPermission.Type.ROLE,
                    true
                )
            )
        }


    @OptIn(ExperimentalStdlibApi::class)
    protected fun buildOptions(builder: OptionsBuilder.() -> Unit): List<CommandUpdateAction.OptionData> {
        return buildList {
            OptionsBuilder(this).apply(builder)
        }
    }
}
