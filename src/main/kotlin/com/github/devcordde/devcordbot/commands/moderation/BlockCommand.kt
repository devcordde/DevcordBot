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
import com.github.devcordde.devcordbot.util.Punisher

/**
 * BlockCommand.
 */
class BlockCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("block")
    override val displayName: String = "block"
    override val description: String = "Blocks a user for a specific time for a channel."
    override val usage: String = "<user> <channel> <time>"
    override val permission: Permission = Permission.MODERATOR
    override val category: CommandCategory = CommandCategory.MODERATION
    override val commandPlace: CommandPlace = CommandPlace.GM

    override suspend fun execute(context: Context) {
        if (context.args.size < 3) return context.sendHelp().queue()

        val member = context.args.member(0, context = context) ?: return

        val channel = context.args.channel(1, context = context) ?: return

        val period = Punisher.parsePeriod(context.args[2]) ?: return context.respond(
            Embeds.error("Fehler", "Banzeit konnte nicht geparsed werden.\nPattern: `X`y`X`m`X`w`X`d`X`h")
        ).queue()

        channel.createPermissionOverride(member).setDeny(net.dv8tion.jda.api.Permission.MESSAGE_WRITE).queue()
        context.bot.punisher.addBlock(member, channel, period)

        context.respond(
            Embeds.success(
                "User geblockt.",
                "`${member.effectiveName}` wurde erfolgreich vom Channel `${channel.name}` ausgeschlossen."
            )
        )
    }
}