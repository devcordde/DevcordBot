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

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.utils.data.DataObject

/**
 * Builder for slash command permissions.
 */
class PermissionsBuilder(private val permissions: MutableList<DiscordApplicationCommandPermission> = mutableListOf()) :
    MutableList<DiscordApplicationCommandPermission> by permissions {

    /**
     * Set's the permission for [role] to [allow].
     */
    fun role(role: Role, allow: Boolean = true): Unit = role(role.idLong, allow)

    /**
     * Set's the permission for the role with [id] to [allow].
     */
    fun role(id: Long, allow: Boolean = true) {
        permissions.add(
            DiscordApplicationCommandPermission(
                id,
                DiscordApplicationCommandPermission.Type.ROLE,
                allow
            )
        )
    }

    /**
     * Set's the permission for [member] to [allow].
     */
    fun member(member: Member, allow: Boolean = true): Unit = user(member.user, allow)

    /**
     * Set's the permission for [user] to [allow].
     */
    fun user(user: User, allow: Boolean = true): Unit = user(user.idLong, allow)

    /**
     * Set's the permission for the user with [id] to [allow].
     */
    fun user(id: Long, allow: Boolean = true) {
        permissions.add(
            DiscordApplicationCommandPermission(
                id,
                DiscordApplicationCommandPermission.Type.USER,
                allow
            )
        )
    }

    /**
     * Converts this builder to a [DataObject] which can be sent to Discord.
     */
    fun build(): DataObject = DataObject.empty()
        .put("permissions", permissions.toDataArray())
}
