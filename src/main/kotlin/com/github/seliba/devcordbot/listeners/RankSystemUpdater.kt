/*
 * Copyright 2020 Daniel Scherf & Michael Rittmeister
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

package com.github.seliba.devcordbot.listeners

import com.github.seliba.devcordbot.database.DevCordUser
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Instant
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Updates the Database based on Discord events.
 */
class DatabaseUpdater {

    /**
     * Adds a user to the database when a user joins the guild.
     */
    @SubscribeEvent
    fun onMemberJoin(event: GuildMemberJoinEvent): DevCordUser? = createUserIfNeeded(event.member.idLong)

    /**
     * Removes a user from the database when the user leaves the guild.
     */
    @SubscribeEvent
    fun onMemberLeave(event: GuildMemberRemoveEvent): Unit = deleteUser(event.member?.idLong)

    private fun createUserIfNeeded(id: Long?): DevCordUser? {
        if (id == null) {
            return null
        }

        return transaction {
            DevCordUser.findById(id) ?: DevCordUser.new(id) {}
        }
    }

    /**
     * Adds XP to a user
     */
    @SubscribeEvent
    fun onMessageSent(event: GuildMessageReceivedEvent) {
        val user = createUserIfNeeded(event.author.idLong) ?: return
        if (Duration.between(user.lastUpgrade, Instant.now()) < Duration.ofSeconds(30)) {
            return
        }
        val previousLevel = user.level
        val level = transaction {
            user.experience += Random.nextLong(5, 20)
            user.lastUpgrade = Instant.now()
            val xpToLevelup = getXpToLevelup(user.level)
            if (user.experience >= xpToLevelup) {
                user.experience -= xpToLevelup
                user.level++
            }

            user.level
        }

        if (previousLevel != level) {
            updateLevel(event, level)
        }
    }

    private fun updateLevel(event: GuildMessageReceivedEvent, level: Int) {
        val rankLevel = Level.values().firstOrNull { it.level == level } ?: return
        val guild = event.guild
        val user = event.member ?: return

        if (rankLevel.previousLevel != null) {
            val role = guild.getRoleById(rankLevel.previousLevel.roleId)
            if (role != null) {
                guild.removeRoleFromMember(user, role).queue()
            }
        }
        val role = guild.getRoleById(rankLevel.roleId)
        if (role != null) {
            guild.addRoleToMember(user, role).queue()
        }
    }

    private fun deleteUser(id: Long?) {
        if (id == null) {
            return
        }

        transaction {
            DevCordUser.findById(id)?.delete() ?: Unit
        }
    }

    private fun getXpToLevelup(level: Int) = (25 * sqrt(level.toDouble())).toLong()

}

/**
 * Required level
 * @property roleId The id of the role
 * @property level The level of the role
 * @property previousLevel The previous level
 */
@Suppress("KDocMissingDocumentation")
enum class Level(val roleId: Long, val level: Int, val previousLevel: Level?) {
    LEVEL_1(554734490359037996L, 1, null),
    LEVEL_5(554734613365391361L, 5, LEVEL_1),
    LEVEL_10(554734631866335233L, 10, LEVEL_5),
    LEVEL_20(554734647893032962L, 20, LEVEL_10),
    LEVEL_35(554734662472433677L, 35, LEVEL_20),
    LEVEL_50(563378794111565861, 50, LEVEL_35)
}