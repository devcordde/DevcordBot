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

import com.github.devcordde.devcordbot.database.Punishments.channelId
import com.github.devcordde.devcordbot.database.Punishments.executionTime
import com.github.devcordde.devcordbot.database.Punishments.id
import com.github.devcordde.devcordbot.database.Punishments.kind
import com.github.devcordde.devcordbot.database.Punishments.punishmentId
import com.github.devcordde.devcordbot.database.Punishments.userId
import com.github.devcordde.devcordbot.database.StarboardEntries.authorId
import com.github.devcordde.devcordbot.database.StarboardEntries.botMessageId
import com.github.devcordde.devcordbot.database.StarboardEntries.channelId
import com.github.devcordde.devcordbot.database.StarboardEntries.messageId
import com.github.devcordde.devcordbot.database.Starrers.authorId
import com.github.devcordde.devcordbot.database.Starrers.emojis
import com.github.devcordde.devcordbot.database.Starrers.entry
import com.github.devcordde.devcordbot.database.TagAliases.name
import com.github.devcordde.devcordbot.database.TagAliases.tag
import com.github.devcordde.devcordbot.database.Tags.author
import com.github.devcordde.devcordbot.database.Tags.content
import com.github.devcordde.devcordbot.database.Tags.createdAt
import com.github.devcordde.devcordbot.database.Tags.name
import com.github.devcordde.devcordbot.database.Tags.usages
import com.github.devcordde.devcordbot.database.Users.blacklisted
import com.github.devcordde.devcordbot.database.Users.experience
import com.github.devcordde.devcordbot.database.Users.id
import com.github.devcordde.devcordbot.database.Users.lastUpgrade
import com.github.devcordde.devcordbot.database.Users.level
import com.github.devcordde.devcordbot.database.Warns.id
import com.github.devcordde.devcordbot.database.Warns.reason
import com.github.devcordde.devcordbot.database.Warns.userId
import com.github.devcordde.devcordbot.database.Warns.warnId
import com.github.devcordde.devcordbot.database.Warns.warnTime
import net.dv8tion.jda.api.entities.Message
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.timestamp
import java.time.Instant
import java.util.*

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


/**
 * Representation of warns table.
 * @property id @see [warnId]
 * @property warnId the id of the warn.
 * @property userId the id of the warned user.
 * @property reason the reason message.
 * @property warnTime the time of the warn.
 */
object Warns : UUIDTable() {
    override val id: Column<EntityID<UUID>> get() = warnId
    val warnId: Column<EntityID<UUID>> = uuid("warn_id").autoGenerate().entityId()
    val userId: Column<Long> = long("user_id")
    val reason: Column<String> = varchar("reason", 120)
    val warnTime: Column<Instant> = timestamp("warn_time").default(Instant.now())

    override val primaryKey: PrimaryKey = PrimaryKey(warnId)
}

/**
 * Representation of punishments table.
 * @property id @see [punishmentId]
 * @property punishmentId the id of the punishment.
 * @property kind the kind of the punishment.
 * @property userId the punished user.
 * @property channelId the channel the punishment is executed for.
 * @property executionTime the time the punishment runs out.
 */
object Punishments : UUIDTable() {
    override val id: Column<EntityID<UUID>> get() = punishmentId

    val punishmentId: Column<EntityID<UUID>> = uuid("punishment_id").autoGenerate().entityId()
    val kind: Column<String> = varchar("kind", 120)
    val userId: Column<String> = varchar("user_id", 50)
    var channelId: Column<String?> = varchar("channel_id", 50).nullable()
    val executionTime: Column<Instant> = timestamp("execution_time")

    override val primaryKey: PrimaryKey = PrimaryKey(punishmentId)
}
