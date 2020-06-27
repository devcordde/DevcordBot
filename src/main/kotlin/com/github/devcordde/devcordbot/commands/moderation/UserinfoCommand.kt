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
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

/**
 * UserinfoCommand.
 */
class UserinfoCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("userinfo")
    override val displayName: String = "userinfo"
    override val description: String = "Shows information about the User."
    override val usage: String = "<user>"
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.MODERATION
    override val commandPlace: CommandPlace = CommandPlace.GM

    private val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(Locale.GERMANY)
        .withZone(ZoneId.systemDefault())

    override suspend fun execute(context: Context) {
        if (context.args.size < 1) {
            val member = context.member ?: return
            return sendInfo(context, member)
        }

        val member = context.args.member(0, context = context) ?: return
        if (context.author.id == member.id) {
            return sendInfo(context, member)
        }

        if (!context.hasPermission(Permission.MODERATOR)) return context.respond(
            Embeds.error(
                "Keine Permission.",
                "Du darfst nur deine eigenen Informationen abrufen."
            )
        ).queue()


        sendInfo(context, member)
    }

    private fun sendInfo(context: Context, member: Member) {
        return context.respond(
            EmbedBuilder()
                .setDescription(member.asMention)
                .setThumbnail(member.user.avatarUrl)
                .setAuthor(member.user.asTag, null, member.user.avatarUrl)
                .addField("Joined", member.timeJoined.format(formatter), true)
                .addField("Registered", member.timeCreated.format(formatter), true)
                .addField(
                    "Roles [${member.roles.size}]",
                    member.roles.joinToString(" ") { role -> role.asMention },
                    false
                )
                .addField("Key Permissions", member.permissions.joinToString(", ") { permission ->
                    permission.name
                }, false)
                .addField("Acknowledgements", member.permissionsExplicit.joinToString(", ") { permission ->
                    permission.name
                }, false)
                .setFooter("ID: ${member.user.id}")
                .setTimestamp(Instant.now())
                .build()
        ).queue()
    }
}
