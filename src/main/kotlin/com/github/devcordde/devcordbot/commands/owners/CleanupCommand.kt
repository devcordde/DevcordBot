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

import com.github.devcordde.devcordbot.command.AbstractSingleCommand
import com.github.devcordde.devcordbot.command.CommandCategory
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.database.DatabaseDevCordUser
import com.github.devcordde.devcordbot.database.Tag
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * Cleanup Command
 */
class CleanupCommand : AbstractSingleCommand() {
    override val name: String = "cleanup"
    override val description: String = "Entfernt die Level von ungültigen Membern."
    override val category: CommandCategory = CommandCategory.BOT_OWNER
    override val permission: Permission = Permission.BOT_OWNER
    override val commandPlace: CommandPlace = CommandPlace.ALL

    private val logger = KotlinLogging.logger {}

    override suspend fun execute(context: Context) {
        val guild = context.bot.guild
        val cleanedUsers = cleanupRanks(guild)
        val cleanedTags = cleanupTags(guild, context.bot.kord.selfId)

        context.respond(
            Embeds.info(
                "Erfolgreich ausgeführt!",
                """
                Entfernte Nutzer: $cleanedUsers
                Übertragene Tags: $cleanedTags
                """
            )
        )
    }

    private suspend fun cleanupRanks(guild: Guild): Int {
        var clearedEntries = 0
        newSuspendedTransaction {
            DatabaseDevCordUser.all().forEach {
                if (!isMemberOfGuild(guild, it.userID)) {
                    logger.info { "User gelöscht: ID ${it.userID}, Level: ${it.level}, XP: ${it.experience}" }
                    it.delete()
                    clearedEntries++
                }
            }
        }
        return clearedEntries
    }

    private suspend fun cleanupTags(guild: Guild, selfId: Snowflake): Int {
        var movedEntries = 0
        newSuspendedTransaction {
            Tag.all().forEach {
                if (!isMemberOfGuild(guild, it.author)) {
                    logger.info { "Autor geändert: Alter Author: ${it.author}, Name: ${it.name}" }
                    it.author = selfId
                    movedEntries++
                }
            }
        }
        return movedEntries
    }

    private suspend fun isMemberOfGuild(guild: Guild, userID: Snowflake): Boolean =
        guild.getMemberOrNull(userID) != null
}
