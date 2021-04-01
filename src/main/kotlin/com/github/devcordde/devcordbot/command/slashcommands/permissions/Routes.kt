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

import net.dv8tion.jda.internal.requests.Route

/**
 * Collection of [Routes][Route] related to slash command permissions.
 */
object SlashCommandPermissionRoutes {
    /**
     * Route which retrieves all permissions on a guild.
     */
    val GET_GUILD_APPLICATION_COMMAND_PERMISSIONS: Route = Route.get(
        "/applications/{application_id}/guilds/{guild_id}/commands/permissions"
    )

    /**
     * Route which retrieves the permissions of a specific command.
     */
    val GET_APPLICATION_COMMAND_PERMISSIONS: Route = Route.get(
        "/applications/{application_id}/guilds/{guild_id}/commands/{command_id}/permissions"
    )

    /**
     * Route which updates the permissions of a command.
     */
    val PUT_APPLICATION_COMMAND_PERMISSIONS: Route = Route.put(
        "/applications/{application_id}/guilds/{guild_id}/commands/{command_id}/permissions"
    )
}
