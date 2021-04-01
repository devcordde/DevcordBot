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

import com.github.devcordde.devcordbot.command.slashcommands.permissions.DiscordApplicationCommandPermission.Type
import net.dv8tion.jda.api.entities.ISnowflake
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject

/**
 * Representation of a slash command permission
 *
 * @param id the id of the entity the permission is for
 * @property type the [Type] of the entity the permission is for
 * @property permission whether the permission should allow execution or not
 */
data class DiscordApplicationCommandPermission(
    private val id: Long, val type: Type, val permission: Boolean
) : ISnowflake {

    /**
     * The Type of an entity which can have a pemrission
     *
     * @property value the value in which discord stores this
     */
    enum class Type(val value: Int) {
        /**
         * A role
         */
        ROLE(1),

        /**
         * A user
         */
        USER(2);

        companion object {
            /**
             * Maps [Type.value] to a [Type].
             */
            fun fromValue(value: Int): Type = values().first { it.value == value }
        }
    }

    /**
     * the id of the entity the permission is for.
     */
    override fun getIdLong(): Long = id

    /**
     * Converts this into a [DataObject] which can be sent to Discord.
     */
    fun toDataObject(): DataObject = DataObject.empty()
        .put("id", id)
        .put("type", type.value)
        .put("permission", permission)

    companion object {
        /**
         * Converts a [DataObject] to [DiscordApplicationCommandPermission]
         */
        fun fromDataObject(json: DataObject): DiscordApplicationCommandPermission = DiscordApplicationCommandPermission(
            json.getLong("id"),
            Type.fromValue(json.getInt("type")),
            json.getBoolean("permission")
        )

        /**
         * Converts a [DataArray] to a list of [DiscordApplicationCommandPermission]
         */
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

/**
 * Maps the list to a list of [DataObjects][DataObject].
 */
fun List<DiscordApplicationCommandPermission>.toDataObject(): List<DataObject> =
    map(DiscordApplicationCommandPermission::toDataObject)

/**
 * Maps the List into a [DataArray].
 */
fun List<DiscordApplicationCommandPermission>.toDataArray(): DataArray =
    DataArray.fromCollection(toDataObject())

