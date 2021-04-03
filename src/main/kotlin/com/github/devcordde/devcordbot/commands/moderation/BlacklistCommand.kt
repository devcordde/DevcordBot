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

package com.github.devcordde.devcordbot.commands.moderation

import com.github.devcordde.devcordbot.command.AbstractRootCommand
import com.github.devcordde.devcordbot.command.AbstractSubCommand
import com.github.devcordde.devcordbot.command.CommandCategory
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.database.DatabaseDevCordUser
import com.github.devcordde.devcordbot.database.Users
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * BlacklistCommand
 */
class BlacklistCommand : AbstractRootCommand() {
    override val name: String = "blacklist"
    override val description: String = "Hindert einen User daran Commands auszuführen."
    override val permission: Permission = Permission.ADMIN
    override val category: CommandCategory = CommandCategory.MODERATION
    override val commandPlace: CommandPlace = CommandPlace.ALL

    init {
        registerCommands(BlacklistListCommand())
        registerCommands(BlacklistToggleCommand())
    }

    private inner class BlacklistToggleCommand : AbstractSubCommand.Command(this) {
        override val name: String = "toggle"
        override val description: String = "Fügt hinzu/Entfernt einen Nutzer von der blacklist"

        override val options: List<CommandUpdateAction.OptionData> = buildOptions {
            user("target", "Der Nutzer der zur/von der Schwarzen Liste hinzugefügt/entfernt werden soll") {
                isRequired = true
            }
        }

        override suspend fun execute(context: Context) {
            val user = context.args.user("target")

            val blacklisted = transaction {
                val devcordUser = DatabaseDevCordUser.findOrCreateById(user.idLong)

                devcordUser.blacklisted = !devcordUser.blacklisted
                devcordUser.blacklisted
            }

            context.respond(Embeds.success(if (blacklisted) "User zur Blacklist hinzugefügt." else "User aus der Blacklist entfernt."))
                .queue()
        }
    }

    private inner class BlacklistListCommand : AbstractSubCommand.Command(this) {
        override val name: String = "list"
        override val description: String = "Listet geblacklistete User auf."

        override suspend fun execute(context: Context) {
            val userNames = transaction {
                DatabaseDevCordUser.find {
                    Users.blacklisted eq true
                }.map {
                    "`${context.guild.getMemberById(it.userID)?.effectiveName ?: "Nicht auf dem Guild"}`"
                }
            }

            if (userNames.isEmpty()) {
                return context.respond(Embeds.warn("Niemand da!", "Es ist niemand auf der blacklist")).queue()
            }
            context.respond(Embeds.success("Blacklisted Users", userNames.joinToString(", "))).queue()
        }
    }
}
