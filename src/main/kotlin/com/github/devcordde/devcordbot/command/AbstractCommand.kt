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

package com.github.devcordde.devcordbot.command

import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.root.AbstractRootCommand
import com.github.devcordde.devcordbot.command.root.AbstractSingleCommand
import dev.kord.common.entity.DiscordGuildApplicationCommandPermission
import dev.kord.common.entity.Snowflake

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
     * Generates a list of [DiscordApplicationCommandPermissions][DiscordGuildApplicationCommandPermission]
     * required to register slash command permissions
     *
     * @param botOwners a list of ids which can bypass [Permission.BOT_OWNER]
     * @param modId the id of the role for [Permission.MODERATOR]
     * @param adminId the id of the role for [Permission.ADMIN]
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun generatePermissions(
        botOwners: List<Snowflake>,
        modId: Snowflake,
        adminId: Snowflake
    ): List<DiscordGuildApplicationCommandPermission> = when (permission) {
        Permission.ANY -> emptyList()
        Permission.BOT_OWNER -> botOwners.map {
            DiscordGuildApplicationCommandPermission(
                it,
                DiscordGuildApplicationCommandPermission.Type.User,
                true
            )
        }
        Permission.MODERATOR -> listOf(
            DiscordGuildApplicationCommandPermission(
                modId,
                DiscordGuildApplicationCommandPermission.Type.Role,
                true
            )
        )
        Permission.ADMIN -> listOf(
            DiscordGuildApplicationCommandPermission(
                modId,
                DiscordGuildApplicationCommandPermission.Type.Role,
                true
            ),
            DiscordGuildApplicationCommandPermission(
                adminId,
                DiscordGuildApplicationCommandPermission.Type.Role,
                true
            )
        )
    }
}
