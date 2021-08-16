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

package com.github.devcordde.devcordbot.listeners

import com.github.devcordde.devcordbot.core.DevCordBot
import com.github.devcordde.devcordbot.database.DatabaseDevCordUser
import com.github.devcordde.devcordbot.database.Tags
import com.github.devcordde.devcordbot.database.Users
import com.github.devcordde.devcordbot.util.XPUtil
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Duration
import java.time.Instant

/**
 * Updates the Database based on Discord events.
 */
class DatabaseUpdater(private val bot: DevCordBot) {

    /**
     * Registers all required listeners.
     */
    fun Kord.registerListeners() {
        onMemberJoin()
        onMemberLeave()
        onMessageSent()
    }

    private fun Kord.onMemberJoin() = on<MemberJoinEvent> {
        newSuspendedTransaction { DatabaseDevCordUser.findOrCreateById(member.id.value) }
    }

    private fun Kord.onMemberLeave() = on<MemberLeaveEvent> {
        val id = user.id

        newSuspendedTransaction {
            Tags.update({ Tags.author eq id }) {
                it[author] = kord.selfId
            }
        }

        newSuspendedTransaction {
            Users.deleteWhere { Users.id eq id }
        }
    }

    /**
     * Adds XP to a user
     */
    private fun Kord.onMessageSent() = on<MessageCreateEvent> {
        if (message.author == null || message.author?.isBot == true) {
            return@on
        }

        if (message.channel.id !in bot.config.xpWhitelist) {
            return@on
        }

        val author = message.author ?: return@on

        val user = newSuspendedTransaction { DatabaseDevCordUser.findOrCreateById(author.id.value) }

        if (user.blacklisted) {
            return@on
        }

        if (Duration.between(user.lastUpgrade, Instant.now()) < Duration.ofSeconds(15)) {
            return@on
        }

        val previousLevel = user.level
        val previousXp = user.experience
        var newLevel = previousLevel
        var newXp = previousXp
        newSuspendedTransaction {
            // For some bizarre reasons using the DAO update performs a useless SELECT query before the update query
            Users.update(
                {
                    Users.id eq message.author!!.id
                }
            ) {
                @Suppress("UNCHECKED_CAST") // Users.update will give you UpdateStatement<User> :smart:
                (it as UpdateBuilder<Users>)[lastUpgrade] = Instant.now()
                val xpToLevelup = XPUtil.getXpToLevelup(user.level)
                if (user.experience >= xpToLevelup) {
                    newXp = user.experience + 5 - xpToLevelup
                    it[level] = user.level + 1
                    newLevel++
                } else {
                    newXp = user.experience + 5
                }

                it[experience] = newXp
            }
        }

        bot.discordLogger.logEvent(
            "XP Increase",
            "Level: $previousLevel -> $newLevel;" +
                " XP: $previousXp -> $newXp",
            author
        )

        if (previousLevel != newLevel) {
            updateLevel(newLevel)
        }
    }

    private suspend fun MessageCreateEvent.updateLevel(level: Int) {
        val rankLevel = Level.values().findLast { it.level <= level } ?: return
        val guild = getGuild() ?: return
        val user = message.getAuthorAsMember() ?: return

        if (rankLevel.previousLevel != null) {
            val role = guild.getRoleOrNull(rankLevel.previousLevel.roleId)
            if (role != null && role.id in user.roleIds) {
                user.removeRole(role.id, "Rank system")
            }
        }

        val role = guild.getRoleOrNull(rankLevel.roleId)
        if (role != null && role.id !in user.roleIds) {
            user.addRole(role.id, "Rank system")
        }
    }
}

/**
 * Required level
 * @property roleId The id of the role
 * @property level The level of the role
 * @property previousLevel The previous level
 */
@Suppress("KDocMissingDocumentation")
enum class Level(
    val roleId: Snowflake,
    val level: Int,
    val previousLevel: Level?
) {
    LEVEL_1(739065032897200228, 1, null),
    LEVEL_5(739065117702094890, 5, LEVEL_1),
    LEVEL_10(739065451325423679, 10, LEVEL_5),
    LEVEL_20(739065355116347434, 20, LEVEL_10),
    LEVEL_35(739065293808205904, 35, LEVEL_20),
    LEVEL_50(739064877842432010, 50, LEVEL_35),
    LEVEL_75(739064774427541514, 75, LEVEL_50),
    LEVEL_100(739064698498187286, 100, LEVEL_75);

    constructor(roleId: Long, level: Int, previousLevel: Level?) : this(Snowflake(roleId), level, previousLevel)
}
