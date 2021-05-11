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

package com.github.devcordde.devcordbot.commands.general

import com.github.devcordde.devcordbot.command.AbstractSubCommand
import com.github.devcordde.devcordbot.command.CommandCategory
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.root.AbstractRootCommand
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.database.DatabaseDevCordUser
import com.github.devcordde.devcordbot.database.Users
import com.github.devcordde.devcordbot.util.XPUtil
import com.github.devcordde.devcordbot.util.effictiveName
import dev.kord.core.behavior.interaction.InteractionResponseBehavior
import dev.kord.core.entity.User
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.rest.builder.interaction.SubCommandBuilder
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Rank command.
 */
class RankCommand : AbstractRootCommand() {
    override val name: String = "rank"
    override val description: String = "Zeigt das Level von Nutzern an."
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.GENERAL
    override val commandPlace: CommandPlace = CommandPlace.ALL

    init {
        registerCommands(StatsCommand())
        registerCommands(TopCommand())
    }

    private inner class StatsCommand : AbstractSubCommand.Command<InteractionResponseBehavior>(this) {
        override val name: String = "stats"
        override val description: String = "Zeigt den Rang eines Nutzers an."

        override suspend fun InteractionCreateEvent.acknowledge(): InteractionResponseBehavior =
            interaction.acknowledgeEphemeral()

        override fun SubCommandBuilder.applyOptions() {
            user("target", "Der Nutzer, von dem der Rang angezeigt werden soll")
        }

        override suspend fun execute(context: Context<InteractionResponseBehavior>) {
            val user = context.args.optionalUser("target")
                ?: return sendRankInformation(context.author.asUser(), context, true)
            sendRankInformation(user, context)
        }
    }

    private suspend fun sendRankInformation(
        user: User,
        context: Context<InteractionResponseBehavior>,
        default: Boolean = false
    ) {
        val entry =
            if (default) context.devCordUser else transaction { DatabaseDevCordUser.findOrCreateById(user.id.value) }
        val currentXP = entry.experience
        val nextLevelXP = XPUtil.getXpToLevelup(entry.level)
        context.respond(
            Embeds.info(
                "Rang von ${user.tag}",
                ""
            ) {
                field {
                    name = "Level"
                    value = entry.level.toString()
                    inline = true
                }
                field {
                    name = "Nächstes Level"
                    value = "$currentXP/${nextLevelXP}XP ${buildProgressBar(currentXP, nextLevelXP)}"
                }
            }
        )
    }

    private fun buildProgressBar(current: Long, next: Long): String {
        val stringBuilder = StringBuilder()
        val barProgress = (current.toDouble() / next * 20).toInt()
        stringBuilder.append("█".repeat(barProgress))
            .append("▒".repeat(20 - barProgress))
        return stringBuilder.toString()
    }

    private inner class TopCommand : AbstractSubCommand.Command<InteractionResponseBehavior>(this) {
        override val name: String = "top"
        override val description: String = "Zeigt die 10 User mit dem höchsten Rang an."

        override suspend fun InteractionCreateEvent.acknowledge(): InteractionResponseBehavior =
            interaction.acknowledgeEphemeral()

        override fun SubCommandBuilder.applyOptions() {
            int("offset", "Der Index, um den die Liste verschoben werden soll")
        }

        override suspend fun execute(context: Context<InteractionResponseBehavior>) {
            var offset = context.args.optionalInt("offset") ?: 0
            var invalidOffset = false
            var maxOffset = 0
            if (offset < 0) offset = 0
            if (offset != 0) {
                transaction {
                    maxOffset = DatabaseDevCordUser.all().count().toInt()
                    if (maxOffset <= offset) {
                        invalidOffset = true
                        offset = if (maxOffset < 10) {
                            maxOffset - 1
                        } else {
                            maxOffset - 10
                        }
                    }
                }
            }

            val users = newSuspendedTransaction {
                DatabaseDevCordUser.all().limit(10, offset.toLong())
                    .orderBy(Users.level to SortOrder.DESC, Users.experience to SortOrder.DESC)
                    .mapIndexed { index, it ->
                        val name =
                            context.guild.getMemberOrNull(it.userID)?.effictiveName ?: "Nicht auf dem Server"
                        "`${index + offset + 1}.` `$name`: Level `${it.level}`"
                    }
            }

            if (invalidOffset) {
                context.respond(
                    Embeds.warn(
                        "Rangliste | Zu hoher Offset! (Maximum: ${maxOffset - 1})",
                        users.joinToString("\n")
                    )
                )
                return
            }
            context.respond(Embeds.info("Rangliste", users.joinToString("\n")))
        }
    }
}
