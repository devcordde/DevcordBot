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

@file:Suppress("MemberVisibilityCanBePrivate")

package com.github.devcordde.devcordbot.database

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.time.Instant

/**
 * Representation of a devcord user.
 * @property userID the id of the user.
 * @property level the current level of the user
 * @property experience the current amount of experience points of the user
 * @property lastUpgrade the last time the user gained XP
 * @property blacklisted user is blacklisted for commands
 */
interface DevCordUser {
    val userID: Long
    var level: Int
    var experience: Long
    var lastUpgrade: Instant
    var blacklisted: Boolean
}

/**
 * Representation of a user in the database.
 * @param id the [EntityID] of the user.
 * @property userID accessor for the value of [id]
 * @property level the current level of the user
 * @property experience the current amount of experience points of the user
 * @property lastUpgrade the last time the user gained XP
 * @property blacklisted user is blacklisted for commands
 */
open class DatabaseDevCordUser(id: EntityID<Long>) : LongEntity(id), DevCordUser {
    companion object : LongEntityClass<DatabaseDevCordUser>(Users) {
        /**
         * Returns the [DevCordUser] corresponding to [id] and created one if needed.
         */
        fun findOrCreateById(id: Long): DevCordUser = findById(id) ?: new(id) { }

        /**
         * Returns the [DevCordUser] corresponding to [id] and created one if needed.
         */
        fun findOrCreateById(id: Snowflake): DevCordUser = findOrCreateById(id.value)
    }

    override val userID: Long
        get() = id.value
    override var level: Int by Users.level
    override var experience: Long by Users.experience
    override var lastUpgrade: Instant by Users.lastUpgrade
    override var blacklisted: Boolean by Users.blacklisted
}

/**
 * Representation of a tag in the database.
 * @property name the name of the tag
 * @property usages amount of times the tag was used
 * @property author the id of the author of the tag
 * @property content the content of the tag
 * @property createdAt the timestamp of the creation of the tag
 */
class Tag(name: EntityID<String>) : Entity<String>(name) {
    companion object : EntityClass<String, Tag>(Tags) {

        /**
         * Finds the first [Tag] by its [name].
         */
        fun findByNameId(name: String): Tag? = find { upper(Tags.name) eq name.toUpperCase() }.firstOrNull()

        /**
         * Maximum length of a tag name.
         */
        const val NAME_MAX_LENGTH: Int = 32
    }

    val name: String
        get() = id.value
    var usages: Int by Tags.usages
    var author: Long by Tags.author
    var content: String by Tags.content
    val createdAt: Instant by Tags.createdAt

}

/**
 * Alias of a [Tag].
 * @property name the name of the alias
 * @property tag the tag the alias is for
 */
class TagAlias(alias: EntityID<String>) : Entity<String>(alias) {
    companion object : EntityClass<String, TagAlias>(TagAliases) {
        /**
         * Finds the first [TagAlias] by its [name].
         */
        fun findByNameId(name: String): TagAlias? = find { upper(TagAliases.name) eq name.toUpperCase() }.firstOrNull()
    }

    val name: String
        get() = id.value
    var tag: Tag by Tag referencedOn TagAliases.tag
}
