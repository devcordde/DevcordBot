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

import com.github.devcordde.devcordbot.database.DatabaseDevCordUser
import com.github.devcordde.devcordbot.database.DevCordUser
import com.github.devcordde.devcordbot.database.Tags
import com.github.devcordde.devcordbot.database.Users
import com.github.devcordde.devcordbot.event.DevCordGuildMessageReceivedEvent
import com.github.devcordde.devcordbot.util.XPUtil
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Duration
import java.time.Instant

/**
 * Updates the Database based on Discord events.
 */
class DatabaseUpdater(private val whiteList: List<String>) {

    /**
     * Adds a user to the database when a user joins the guild.
     */
    @SubscribeEvent
    fun onMemberJoin(event: GuildMemberJoinEvent): DevCordUser? =
        transaction { DatabaseDevCordUser.findOrCreateById(event.user.idLong) }

    /**
     * Removes a user from the database when the user leaves the guild.
     */
    @SubscribeEvent
    fun onMemberLeave(event: GuildMemberRemoveEvent) {
        val id = event.member?.idLong ?: return

        transaction {
            Tags.update({ Tags.author eq id }) {
                it[author] = event.jda.selfUser.idLong
            }
        }

        transaction {
            Users.deleteWhere { Users.id eq id }
        }
    }

    /**
     * Adds XP to a user
     */
    @SubscribeEvent
    fun onMessageSent(event: DevCordGuildMessageReceivedEvent) {
        if (event.author.isBot) {
            return
        }

        if (event.channel.id !in whiteList) {
            return
        }

        val user = event.devCordUser

        if (user.blacklisted) {
            return
        }

        if (Duration.between(user.lastUpgrade, Instant.now()) < Duration.ofSeconds(15)) {
            return
        }
        val previousLevel = user.level
        var newLevel = previousLevel
        transaction {
            // For some bizarre reasons using the DAO update performs a useless SELECT query before the update query
            Users.update(
                {
                    Users.id eq event.author.idLong
                }
            ) {
                @Suppress("UNCHECKED_CAST") // Users.update will give you UpdateStatement<User> :smart:
                (it as UpdateBuilder<Users>)[lastUpgrade] = Instant.now()
                val xpToLevelup = XPUtil.getXpToLevelup(user.level)
                if (user.experience >= xpToLevelup) {
                    it[experience] = user.experience + 5 - xpToLevelup
                    it[level] = user.level + 1
                    newLevel++
                } else {
                    it[experience] = user.experience + 5
                }
            }
        }

        if (previousLevel != newLevel) {
            updateLevel(event, newLevel)
        }
    }

    private fun updateLevel(event: GuildMessageReceivedEvent, level: Int) {
        val rankLevel = Level.values().findLast { it.level <= level } ?: return
        val guild = event.guild
        val user = event.member ?: return

        if (rankLevel.previousLevel != null) {
            val role = guild.getRoleById(rankLevel.previousLevel.roleId)
            if (role != null && role in user.roles) {
                guild.removeRoleFromMember(user, role).queue()
            }
        }

        val role = guild.getRoleById(rankLevel.roleId)
        if (role != null && role !in user.roles) {
            guild.addRoleToMember(user, role).queue()
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
    val roleId: Long,
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
    LEVEL_100(739064698498187286, 100, LEVEL_75)
}