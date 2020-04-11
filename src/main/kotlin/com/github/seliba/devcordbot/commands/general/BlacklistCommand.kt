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

package com.github.seliba.devcordbot.commands.general

import com.github.seliba.devcordbot.command.AbstractCommand
import com.github.seliba.devcordbot.command.AbstractSubCommand
import com.github.seliba.devcordbot.command.CommandCategory
import com.github.seliba.devcordbot.command.context.Context
import com.github.seliba.devcordbot.command.permission.Permission
import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.database.DevCordUser
import com.github.seliba.devcordbot.database.Users
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * BlacklistCommand
 */
class BlacklistCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("blacklist", "bl")
    override val displayName: String = "blacklist"
    override val description: String = "Hindert einen User daran commands auszuführen."
    override val usage: String = "<playerid>"
    override val permission: Permission = Permission.ADMIN
    override val category: CommandCategory = CommandCategory.GENERAL

    init {
        registerCommands(BlacklistListCommand())
    }

    override suspend fun execute(context: Context) {
        val user = context.args.optionalUser(0, jda = context.jda)
            ?: return context.sendHelp().queue()

        val blacklisted = transaction {
            val dcUser = DevCordUser.findById(user.idLong) ?: DevCordUser.new(user.idLong) {}

            dcUser.blacklisted = !dcUser.blacklisted
            dcUser.blacklisted
        }

        context.respond(Embeds.success(if (blacklisted) "User zur blacklist hinzugefügt." else "User aus der Blacklist entfernt."))
            .queue()
    }

    private inner class BlacklistListCommand : AbstractSubCommand(this) {
        override val aliases: List<String> = listOf("list", "l")
        override val displayName: String = "list"
        override val description: String = "Listet geblacklistete User auf."
        override val usage: String = ""

        override suspend fun execute(context: Context) {
            val userNames = transaction {
                DevCordUser.find {
                    Users.blacklisted eq true
                }.map {
                    "`${context.guild.getMemberById(it.userID)?.effectiveName ?: "Nicht auf dem Guild"}`"
                }
            }

            if (userNames.isEmpty()) {
                return
            }
            context.respond(Embeds.success("Blacklisted Users", userNames.joinToString(", "))).queue()
        }
    }
}
