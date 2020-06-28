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

import com.github.devcordde.devcordbot.command.AbstractCommand
import com.github.devcordde.devcordbot.command.CommandCategory
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.constants.Embeds

/**
 * UnbanCommand.
 */
class UnbanCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("unban")
    override val displayName: String = "unban"
    override val description: String = "Entbannt einen gebannten user."
    override val usage: String = "<user-id>"
    override val permission: Permission = Permission.MODERATOR
    override val category: CommandCategory = CommandCategory.MODERATION
    override val commandPlace: CommandPlace = CommandPlace.GM

    override suspend fun execute(context: Context) {
        if (context.args.size < 1) return context.sendHelp().queue()

        val userId = context.args[0]
        val ban = runCatching {
            context.guild.retrieveBanById(userId).submit().get()
        }.getOrNull() ?: return context.respond(
            Embeds.error(
                "User nicht gefunden.",
                "Der angegebene Nutzer ist nicht gebannt."
            )
        ).queue()

        context.bot.punisher.unban(userId)

        return context.respond(
            Embeds.success(
                "User entbannt.",
                "${ban.user.name} wurde erfolgreich entbannt."
            )
        ).queue()

    }
}
