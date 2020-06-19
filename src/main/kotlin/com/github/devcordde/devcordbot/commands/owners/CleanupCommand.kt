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

package com.github.devcordde.devcordbot.commands.owners

import com.github.devcordde.devcordbot.command.AbstractCommand
import com.github.devcordde.devcordbot.command.CommandCategory
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.database.DevCordUser
import com.github.devcordde.devcordbot.database.Tag
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Cleanup Command
 */
class CleanupCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("cleanup")
    override val displayName: String = "Cleanup"
    override val description: String = "Entfernt die Level von ungültigen Membern"
    override val usage: String = "<tagname>"
    override val category: CommandCategory = CommandCategory.BOT_OWNER
    override val permission: Permission = Permission.BOT_OWNER
    override val commandPlace: CommandPlace = CommandPlace.ALL

    private val logger = KotlinLogging.logger {}

    override suspend fun execute(context: Context) {
        val guild = context.bot.guild
        val cleanedUsers = cleanupRanks(guild)
        val cleanedTags = cleanupTags(guild, context.bot.jda.selfUser)

        return context.respond(
            Embeds.info(
                "Erfolgreich ausgeführt!",
                """
                Entfernte User: $cleanedUsers
                Veränderte Tags: $cleanedTags
                """
            )
        ).queue()
    }

    private fun cleanupRanks(guild: Guild): Int {
        var clearedEntries = 0
        transaction {
            DevCordUser.all().forEach {
                if (!isMemberOfGuild(guild, it.userID)) {
                    logger.info { "User gelöscht: ID ${it.userID}, Level: ${it.level}, XP: ${it.experience}" }
                    it.delete()
                    clearedEntries++
                }
            }
        }
        return clearedEntries
    }

    private fun cleanupTags(guild: Guild, selfUser: User): Int {
        var movedEntries = 0
        transaction {
            Tag.all().forEach {
                if (!isMemberOfGuild(guild, it.author)) {
                    logger.info { "Autor geändert: Alter Author: ${it.author}, Name: ${it.name}" }
                    it.author = selfUser.idLong
                    movedEntries++
                }
            }
        }
        return movedEntries
    }

    private fun isMemberOfGuild(guild: Guild, userID: Long): Boolean {
        return guild.getMemberById(userID) != null
    }
}
