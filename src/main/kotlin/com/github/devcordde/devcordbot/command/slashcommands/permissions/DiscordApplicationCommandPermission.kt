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
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject

data class DiscordApplicationCommandPermission(
    private val id: Long, val type: Type, val permission: Boolean
) : ISnowflake {

    enum class Type(val value: Int) {
        ROLE(1),
        USER(2);

        companion object {
            fun fromValue(value: Int) = values().first { it.value == value }
        }
    }

    override fun getIdLong(): Long = id

    fun toDataObject(): DataObject = DataObject.empty()
        .put("id", id)
        .put("type", type.value)
        .put("permission", permission)

    companion object {
        fun fromDataObject(json: DataObject): DiscordApplicationCommandPermission = DiscordApplicationCommandPermission(
            json.getLong("id"),
            Type.fromValue(json.getInt("type")),
            json.getBoolean("permission")
        )

        fun fromDataArray(array: DataArray): List<DiscordApplicationCommandPermission> =
            array.map {
                when (it) {
                    is HashMap<*, *> -> {
                        val dataObject = DataObject.empty()
                        it.forEach { key, value ->
                            dataObject.put(key.toString(), value)
                        }
                        fromDataObject(dataObject)
                    }
                    is DataObject -> fromDataObject(it)
                    else -> error("Unexpected element in permissions: $it ${it::class.qualifiedName}")
                }
            }
    }
}

fun List<DiscordApplicationCommandPermission>.toDataObject(): List<DataObject> =
    map(DiscordApplicationCommandPermission::toDataObject)

fun List<DiscordApplicationCommandPermission>.toDataArray(): DataArray =
    DataArray.fromCollection(toDataObject())

