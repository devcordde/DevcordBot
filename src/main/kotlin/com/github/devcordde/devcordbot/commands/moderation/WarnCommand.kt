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
import com.github.devcordde.devcordbot.database.Warn
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import java.util.Locale


/**
 * WarnCommand.
 */
class WarnCommand : AbstractCommand() {
    init {
        this.registerCommands(
            WarnListCommand(),
            WarnDeleteCommand()
        )
    }

    override val aliases: List<String> = listOf("warn")
    override val displayName: String = "warn"
    override val description: String = "Warns a specific user."
    override val usage: String = "<user> <reason>"
    override val permission: Permission = Permission.MODERATOR
    override val category: CommandCategory = CommandCategory.MODERATION
    override val commandPlace: CommandPlace = CommandPlace.GM

    override suspend fun execute(context: Context) {
        if (context.args.size < 2) return context.sendHelp().queue()
        val member = context.args.member(0, context = context) ?: return
        val reason = context.args.subList(1, context.args.size).joinToString(" ")

        if (reason.isBlank()) return context.sendHelp().queue()

        transaction {
            Warn.new {
                this.userId = member.idLong
                this.reason = reason
            }
        }

        return context.respond(
            Embeds.info("Warn hinzugefügt.", "`${member.effectiveName}` wurde gewarnt.")
        ).queue()
    }

    /**
     * WarnListCommand.
     */
    inner class WarnListCommand : AbstractSubCommand(this) {
        override val aliases: List<String> = listOf("list", "ls")
        override val displayName: String = "list"
        override val description: String = "Listet Verwarnungen für einen User auf."
        override val usage: String = "<user>"

        private val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.GERMANY)
            .withZone(ZoneId.systemDefault())

        override suspend fun execute(context: Context) {
            val member = context.args.member(0, context = context) ?: return

            val warns = transaction {
                Warn.all().filter { warn ->
                    warn.userId == member.idLong
                }.map {
                    "`${it.id}`: `${it.reason}` `${formatter.format(it.warnTime)}`"
                }
            }

            if (warns.isEmpty()) return context.respond(
                Embeds.info("Keine Warns.", "`${member.effectiveName}` wurde noch nicht gewarnt.")
            ).queue()

            return context.respond(
                Embeds.info("`${member.effectiveName}`s warns.", warns.joinToString("\n"))
            ).queue()
        }
    }

    /**
     * WarnDeleteCommand.
     */
    inner class WarnDeleteCommand : AbstractSubCommand(this) {
        override val aliases: List<String> = listOf("delete", "del")
        override val displayName: String = "delete"
        override val description: String = "Löscht einen Warn."
        override val usage: String = "<warnId>"

        override suspend fun execute(context: Context) {
            val warnId = context.args[0]

            if (warnId.isBlank()) return context.sendHelp().queue()

            val deleted = transaction {
                val warn = Warn.findById(UUID.fromString(warnId)) ?: return@transaction false

                warn.delete()

                return@transaction true
            }

            return if (deleted) {
                context.respond(
                    Embeds.info("Warn gelöscht.", "Der Warn mit der ID `${warnId}` wurde gelöscht.")
                ).queue()
            } else {
                context.respond(
                    Embeds.error("Warn nicht gefunden.", "Es konnte kein Warn mit der ID `${warnId}` gefunden werden.")
                ).queue()
            }
        }
    }
}