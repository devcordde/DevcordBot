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

import net.dv8tion.jda.api.entities.ISnowflake
import net.dv8tion.jda.api.utils.data.DataObject

class DiscordApplicationCommandPermissions(
    private val id: Long,
    val applicationId: Long,
    val guildId: Long,
    val permissions: List<DiscordApplicationCommandPermission>
) : ISnowflake {
    override fun getIdLong(): Long = id

    fun toDataObject(): DataObject = DataObject.empty()
        .put("id", id)
        .put("application_id", applicationId)
        .put("guild_id", guildId)
        .put("permissions", permissions.toDataArray())

    companion object {
        fun fromDataObject(json: DataObject): DiscordApplicationCommandPermissions =
            DiscordApplicationCommandPermissions(
                json.getLong("id"),
                json.getLong("application_id"),
                json.getLong("guild_id"),
                DiscordApplicationCommandPermission.fromDataArray(json.getArray("permissions"))
            )
    }
}
