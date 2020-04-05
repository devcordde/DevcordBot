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

package com.github.seliba.devcordbot.commands.general

import com.github.seliba.devcordbot.command.AbstractCommand
import com.github.seliba.devcordbot.command.CommandCategory
import com.github.seliba.devcordbot.command.context.Context
import com.github.seliba.devcordbot.command.permission.Permission
import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.database.DevCordUser
import com.github.seliba.devcordbot.database.Users
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Leaderboard command.
 */
class LeaderboardCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("leaderboard", "top", "best", "thebest")
    override val displayName: String = "Leaderboard"
    override val description: String = "Zeigt einer Liste der aktivesten Mitglieder"
    override val usage: String = "[offset]"
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.GENERAL

    override fun execute(context: Context) {
        val offset = context.args.optionalInt(0) ?: 0
        val members = transaction {
            DevCordUser.all().limit(10, offset).orderBy(Users.level to SortOrder.DESC).toList()
        }

        val formattedMembers = members.joinToString("\n") {
            "<@!${it.id}> - Level ${it.level}"
        }

        context.respond(Embeds.info("Top 10 - Members", formattedMembers)).queue()
    }

}