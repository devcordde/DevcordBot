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
import com.github.seliba.devcordbot.command.AbstractSubCommand
import com.github.seliba.devcordbot.command.CommandCategory
import com.github.seliba.devcordbot.command.context.Context
import com.github.seliba.devcordbot.command.permission.Permission
import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.core.Starboard
import com.github.seliba.devcordbot.database.StarboardEntries
import com.github.seliba.devcordbot.database.StarboardEntry
import com.github.seliba.devcordbot.menu.Paginator
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.internal.utils.Helpers
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Command providing moderation tools to interact with [Starboard].
 */
class StarboardCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("starboard", "sb", "stars", "tothestars", "amejonah", "eclipse")
    override val displayName: String = "Starboard"
    override val description: String = "Starboard halt erklärt sich hoffentlich selbst"
    override val usage: String = ""
    override val permission: Permission = Permission.MODERATOR
    override val category: CommandCategory = CommandCategory.GENERAL

    init {
        registerCommands(
            StarCommand(),
            UnstarCommand(),
            StarrersCommand()
        )
    }

    override fun execute(context: Context): Unit = context.sendHelp().queue() // You're supposed to use sub commands

    private inner class StarCommand : AbstractSubCommand(this) {
        override val aliases: List<String> = listOf("iamquitesurethisisimportant", "star")
        override val displayName: String = "Star"
        override val description: String = "Lässt den bot eine message starren"
        override val usage: String = "<messageId>"

        override fun execute(context: Context) {
            val id = validateId(context) ?: return
            context.channel.retrieveMessageById(id).queue(fun(it: Message) {
                if (checkEntryNotExists(it.idLong, context)) return
                it.addReaction(Starboard.REACTION_EMOJI).queue()
                context.respond(Embeds.success("Erfolgreich!", "Die Nachricht wurde erfolgreich gestarred.")).queue()
            }, {
                context.respond(
                    Embeds.error(
                        "Nicht gefunden!",
                        "Die Nachricht wurde nicht gefunden, bitte führe den Befehl im selben Kanal, in dem die Nachricht ist aus."
                    )
                ).queue()
            })
        }
    }

    private inner class UnstarCommand : AbstractSubCommand(this) {
        override val aliases: List<String> = listOf("unstar")
        override val displayName: String = "Unstar"
        override val description: String = "Entfernt eine Nachricht aus dem Starboard"
        override val usage: String = "<messageId>"

        override fun execute(context: Context) {
            val id = validateId(context) ?: return
            val entry = checkEntryExists(id, context) ?: return
            context.bot.starboard.deleteStarboardEntry(entry.messageId, context.guild)
            context.respond(Embeds.success("Gelöscht!", "Der Eintrag wurde erfolgreich aus dem Starboard entfernt."))
                .queue()
        }
    }

    private inner class StarrersCommand : AbstractSubCommand(this) {
        override val aliases: List<String> = listOf("starrers", "stargazors", "lovers", "eclipse-users")
        override val displayName: String = "Starrers"
        override val description: String = """Zeigt eine Liste aller "Starrer" an """

        override val usage: String = "<messageId>"

        override fun execute(context: Context) {
            val id = validateId(context) ?: return
            val entry = checkEntryExists(id, context) ?: return
            val starrers = transaction {
                entry.starrers.map {
                    context.jda.getUserById(it.authorId)?.asMention ?: "Misteröser Eclipse-Nutzer"
                }
            }
            Paginator(
                starrers,
                context.author,
                context.channel,
                "Stargazors"
            )
        }
    }

    private fun checkEntryNotExists(messageId: Long, context: Context): Boolean {
        val entry = transaction { StarboardEntry.find { StarboardEntries.messageId eq messageId }.firstOrNull() }
        if (entry != null) {
            context.respond(
                Embeds.warn(
                    "Bereits gestarred!",
                    "Du kannst nur Nachrichten, die noch nicht im Starboard sind starren."
                )
            ).queue()
            return true
        }
        return false
    }

    private fun checkEntryExists(messageId: Long, context: Context): StarboardEntry? {
        val entry = transaction {
            StarboardEntry.find { (StarboardEntries.messageId eq messageId) or (StarboardEntries.botMessageId eq messageId) }
                .firstOrNull()
        }
        if (entry == null) {
            context.respond(
                Embeds.warn(
                    "Nicht gestarred!",
                    "Diese Nachricht wurde nicht gesatrred"
                )
            ).queue()
        }
        return entry
    }

    private fun validateId(context: Context): Long? {
        val id = context.args.join()
        if (id.length > 20 || !Helpers.isNumeric(id)) {
            context.respond(Embeds.error("Ungültige Id!", "Bitte gebe eine gültige id an.")).queue()
            return null
        }
        return id.toLong()
    }
}
