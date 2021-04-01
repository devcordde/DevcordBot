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

package com.github.devcordde.devcordbot.command.slashcommands.permissions

import net.dv8tion.jda.api.AccountType
import net.dv8tion.jda.api.entities.Command
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.exceptions.AccountTypeException
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.internal.JDAImpl
import net.dv8tion.jda.internal.requests.RestActionImpl
import net.dv8tion.jda.internal.utils.Checks

fun Command.updatePermissions(
    guildId: String,
    permissionBuilder: PermissionsBuilder.() -> Unit
): RestAction<DiscordApplicationCommandPermissions> {
    AccountTypeException.check(jda.accountType, AccountType.BOT)
    Checks.isSnowflake(guildId, "Message ID")

    val jda = jda as JDAImpl

    val route = SlashCommandPermissionRoutes.PUT_APPLICATION_COMMAND_PERMISSIONS.compile(
        jda.selfUser.applicationId,
        guildId,
        id
    )

    val permissions = PermissionsBuilder().apply(permissionBuilder).build()

    return RestActionImpl(
        jda,
        route,
        permissions
    ) { response, _ ->
        DiscordApplicationCommandPermissions.fromDataObject(response.`object`)
    }
}

fun Guild.retrieveApplicationCommandPermissions(): RestAction<DiscordApplicationCommandPermissions> {
    val jda = jda as JDAImpl

    val route = SlashCommandPermissionRoutes.GET_GUILD_APPLICATION_COMMAND_PERMISSIONS.compile(
        jda.selfUser.applicationId,
        id
    )

    return RestActionImpl(
        jda,
        route
    ) { response, _ -> DiscordApplicationCommandPermissions.fromDataObject(response.`object`) }
}

fun Guild.retrieveApplicationCommandPermissions(command: Command): RestAction<DiscordApplicationCommandPermissions> =
    retrieveApplicationCommandPermissions(command.id)

fun Guild.retrieveApplicationCommandPermissions(commandId: String): RestAction<DiscordApplicationCommandPermissions> {
    val jda = jda as JDAImpl

    val route = SlashCommandPermissionRoutes.GET_APPLICATION_COMMAND_PERMISSIONS.compile(
        jda.selfUser.applicationId,
        id,
        commandId
    )

    return RestActionImpl(
        jda,
        route
    ) { response, _ -> DiscordApplicationCommandPermissions.fromDataObject(response.`object`) }
}
