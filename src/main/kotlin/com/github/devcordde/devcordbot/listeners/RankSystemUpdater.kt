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

package com.github.devcordde.devcordbot.listeners

import com.github.devcordde.devcordbot.core.DevCordBot
import com.github.devcordde.devcordbot.database.DatabaseDevCordUser
import com.github.devcordde.devcordbot.database.DevCordUser
import com.github.devcordde.devcordbot.database.Tag
import com.github.devcordde.devcordbot.database.Users
import com.github.devcordde.devcordbot.event.DevCordGuildMessageReceivedEvent
import com.github.devcordde.devcordbot.util.XPUtil
import mu.KotlinLogging
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
class DatabaseUpdater(private val bot: DevCordBot) {
    private val logger = KotlinLogging.logger {}

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
    fun onMemberLeave(event: GuildMemberRemoveEvent): Unit = deleteUser(event.member?.idLong)

    /**
     * Adds XP to a user
     */
    @SubscribeEvent
    fun onMessageSent(event: DevCordGuildMessageReceivedEvent) {
        if (event.author.isBot) {
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

    private fun deleteUser(id: Long?) {
        if (id == null) {
            return
        }

        transaction {
            Users.deleteWhere { Users.id eq id }

            Tag.all().filter { it.author == id }.forEach {
                logger.info { "Autor geändert: Alter Author: ${it.author}, Name: ${it.name}" }
                it.author = bot.jda.selfUser.idLong
            }
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
    LEVEL_1(554734490359037996L, 1, null),
    LEVEL_5(554734613365391361L, 5, LEVEL_1),
    LEVEL_10(554734631866335233L, 10, LEVEL_5),
    LEVEL_20(554734647893032962L, 20, LEVEL_10),
    LEVEL_35(554734662472433677L, 35, LEVEL_20),
    LEVEL_50(563378794111565861, 50, LEVEL_35),
    LEVEL_75(696293683254919219, 75, LEVEL_50),
    LEVEL_100(696294056317157406, 100, LEVEL_75)
}