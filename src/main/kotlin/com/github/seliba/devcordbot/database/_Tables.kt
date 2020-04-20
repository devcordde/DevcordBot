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

package com.github.seliba.devcordbot.database

import com.github.seliba.devcordbot.database.StarboardEntries.authorId
import com.github.seliba.devcordbot.database.StarboardEntries.botMessageId
import com.github.seliba.devcordbot.database.StarboardEntries.channelId
import com.github.seliba.devcordbot.database.StarboardEntries.messageId
import com.github.seliba.devcordbot.database.Starrers.authorId
import com.github.seliba.devcordbot.database.Starrers.emojis
import com.github.seliba.devcordbot.database.Starrers.entry
import com.github.seliba.devcordbot.database.TagAliases.name
import com.github.seliba.devcordbot.database.TagAliases.tag
import com.github.seliba.devcordbot.database.Tags.author
import com.github.seliba.devcordbot.database.Tags.content
import com.github.seliba.devcordbot.database.Tags.createdAt
import com.github.seliba.devcordbot.database.Tags.name
import com.github.seliba.devcordbot.database.Tags.usages
import com.github.seliba.devcordbot.database.Users.blacklisted
import com.github.seliba.devcordbot.database.Users.experience
import com.github.seliba.devcordbot.database.Users.id
import com.github.seliba.devcordbot.database.Users.lastUpgrade
import com.github.seliba.devcordbot.database.Users.level
import net.dv8tion.jda.api.entities.Message
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.timestamp
import java.time.Instant

/**
 * Representation of the user table in the database.
 * @property id The id row of the table, is also the primary key
 * @property level The level row of the table
 * @property experience The experience row of the table
 * @property lastUpgrade the last time the user gained XP
 * @property blacklisted user is blacklisted for commands
 */
object Users : IdTable<Long>() {
    override val id: Column<EntityID<Long>> = long("id").entityId()
    val level: Column<Int> = integer("level").default(1)
    val experience: Column<Long> = long("experience").default(0L)
    val lastUpgrade: Column<Instant> = timestamp("last_experience_gained").default(Instant.now())
    val blacklisted: Column<Boolean> = bool("blacklisted").default(false)

    override val primaryKey: PrimaryKey = PrimaryKey(id)
}

/**
 * Representation of the tags table in the database.
 * @property name the name of the tag
 * @property usages amount of times the tag was used
 * @property author author of the tag
 * @property content the content of the tag
 * @property createdAt the timestamp of the creation of the tag
 */
object Tags : IdTable<String>() {
    /**
     * @see name
     */
    override val id: Column<EntityID<String>>
        get() = name
    val name: Column<EntityID<String>> = text("name").entityId()
    val usages: Column<Int> = integer("usages").default(0)
    val author: Column<Long> = long("author")
    val content: Column<String> = varchar("content", Message.MAX_CONTENT_LENGTH)
    val createdAt: Column<Instant> = timestamp("created_at").clientDefault { Instant.now() }

    override val primaryKey: PrimaryKey = PrimaryKey(id)
}

/**
 * Representation of the tag alias table in the database.
 * @property name the name of the alias
 * @property tag the tag of the alias
 */
object TagAliases : IdTable<String>() {
    /**
     * @see name
     */
    override val id: Column<EntityID<String>>
        get() = name

    val name: Column<EntityID<String>> = varchar("alias", Tag.NAME_MAX_LENGTH).entityId()

    val tag: Column<EntityID<String>> = reference("tag", Tags)

    override val primaryKey: PrimaryKey = PrimaryKey(id)
}

/**
 * Representation of starboard entries table.
 * @property botMessageId the id of the bot's tracking message
 * @property messageId the id of the starred message
 * @property channelId the id of the channel the message was sent in
 * @property authorId the id of the author who sent the message
 */
object StarboardEntries : LongIdTable() {
    val botMessageId: Column<Long> = long("bot_message_id")
    val messageId: Column<Long> = long("message_id")
    val channelId: Column<Long> = long("channel_id")
    val authorId: Column<Long> = long("author_id")
}

/**
 * Representation of the Starrers table.
 * @property authorId id of the user who starred
 * @property entry the starred starboardentry
 * @property emojis the amount of emojis the starrer added
 * @see StarboardEntries
 */
object Starrers : LongIdTable() {
    val authorId: Column<Long> = long("author_id")
    val entry: Column<EntityID<Long>> = reference("entry_id", StarboardEntries)
    val emojis: Column<Int> = integer("emojis").default(1)
}
