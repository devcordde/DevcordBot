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

import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.slashcommands.OptionsBuilder
import com.github.devcordde.devcordbot.command.slashcommands.permissions.DiscordApplicationCommandPermission
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction

/**
 * Base class for all commands.
 * **DO NOT INHERIT FROM THISE** Use [AbstractSingleCommand] and [AbstractRootCommand] instead
 *
 * @property name the name of the command
 * @property description the description of the command
 * @property permission the [Permission] required to execute this command
 * @property category the [CommandCategory] of this command
 * @property commandPlace the [CommandPlace] in which this command can be executed
 */
abstract class AbstractCommand {
    /**
     * Internal field.
     */
    open val callback: Exception = Exception()

    abstract val name: String
    abstract val description: String
    abstract val permission: Permission
    abstract val category: CommandCategory
    open val commandPlace: CommandPlace = CommandPlace.ALL


    /**
     * Generates a list of [DiscordApplicationCommandPermissions][DiscordApplicationCommandPermission]
     * required to register slash command permissions
     *
     * @param botOwners a list of ids which can bypass [Permission.BOT_OWNER]
     * @param modId the id of the role for [Permission.MODERATOR]
     * @param adminId the id of the role for [Permission.ADMIN]
     */
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


    /**
     * Utility function to create slash command options.
     *
     * @see OptionsBuilder
     */
    @OptIn(ExperimentalStdlibApi::class)
    protected fun buildOptions(builder: OptionsBuilder.() -> Unit): List<CommandUpdateAction.OptionData> {
        return buildList {
            OptionsBuilder(this).apply(builder)
        }
    }
}
