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
import com.github.devcordde.devcordbot.command.AbstractSubCommand
import com.github.devcordde.devcordbot.command.CommandCategory
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.util.Punisher

/**
 * MuteCommand.
 */
class MuteCommand(private val muteRoleId: String) : AbstractCommand() {
    init {
        registerCommands(MuteListCommand())
    }

    override val aliases: List<String> = listOf("mute")
    override val displayName: String = "mute"
    override val description: String = "Muted einen Member."
    override val usage: String = "<user> <time>"
    override val permission: Permission = Permission.MODERATOR
    override val category: CommandCategory = CommandCategory.MODERATION
    override val commandPlace: CommandPlace = CommandPlace.GM

    override suspend fun execute(context: Context) {
        if (context.args.size < 2) return context.sendHelp().queue()
        val member = context.args.member(0, context = context) ?: return

        val period = Punisher.parsePeriod(context.args[1]) ?: return context.respond(
            Embeds.error("Fehler", "Mutezeit konnte nicht geparsed werden.\nPattern: `X`y`X`m`X`w`X`d`X`h`X`m")
        ).queue()

        val muteRole = context.jda.getRoleById(muteRoleId) ?: return

        if (member.roles.contains(muteRole)) return context.respond(
            Embeds.info("Schon gemuted.", "`${member.effectiveName}` ist schon gemuted.")
        ).queue()

        context.guild.addRoleToMember(member, muteRole).queue()
        context.bot.punisher.addMute(
            member.id,
            period
        )

        return context.respond(
            Embeds.info("Muted.", "`${member.effectiveName}` wurde gemuted.")
        ).queue()
    }

    private inner class MuteListCommand : AbstractSubCommand(this) {
        override val aliases: List<String> = listOf("ls", "list")
        override val displayName: String = "list"
        override val description: String = "Listet gemutete User auf."
        override val usage: String = ""

        override suspend fun execute(context: Context) {
            TODO("Not yet implemented")
        }

    }
}
