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

import com.github.devcordde.devcordbot.command.*
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.constants.*
import com.github.devcordde.devcordbot.database.*
import com.github.devcordde.devcordbot.dsl.embed
import com.github.devcordde.devcordbot.menu.Paginator
import com.github.devcordde.devcordbot.util.*
import dev.kord.core.behavior.edit
import dev.kord.rest.builder.interaction.ApplicationCommandCreateBuilder
import dev.kord.rest.builder.interaction.SubCommandBuilder
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Tag command.
 */
class TagCommand : AbstractRootCommand() {
    private val reservedNames: List<String>
    override val name: String = "tag"
    override val description: String = "Zeigt dir einen Tag an"
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.GENERAL
    override val commandPlace: CommandPlace = CommandPlace.ALL

    init {
        registerCommands(
            CreateCommand(),
            AliasCommand(),
            EditCommand(),
            InfoCommand(),
            DeleteCommand(),
            ListCommand(),
            FromCommand(),
            SearchCommand(),
            RawCommand(),
            TransferCommand()
        )
        reservedNames = registeredCommands.map(AbstractCommand::name)
    }

    internal fun registerReadCommand(commandClient: CommandClient) = commandClient.registerCommands(TagReadCommand())

    private inner class TagReadCommand : AbstractSingleCommand() {
        override val name: String = "t"
        override val description: String = "Zeigt dir einen Tag an"
        override val permission: Permission = Permission.ANY
        override val category: CommandCategory = CommandCategory.GENERAL
        override val commandPlace: CommandPlace = CommandPlace.ALL

        override fun ApplicationCommandCreateBuilder.applyOptions() {
            string("tag", "Der Name des Tags welcher angezeigt werden soll") {
                required = true
            }
        }

        override suspend fun execute(context: Context) {
            val tagName = context.args.string("tag")
            val tag = newSuspendedTransaction { checkNotTagExists(tagName, context) } ?: return
            context.respond(tag.content)
            transaction {
                tag.usages++
            }
        }
    }

    private inner class CreateCommand : AbstractSubCommand.Command(this) {
        override val name: String = "create"
        override val description: String = "Erstellt einen neuen Tag"

        override fun SubCommandBuilder.applyOptions() {
            string("name", "Der Name des zu erstellenden Tags") {
                required = true
            }
        }

        @OptIn(ExperimentalTime::class)
        override suspend fun execute(context: Context) {
            val name = context.args.string("name")
            if (newSuspendedTransaction { checkTagExists(name, context) }) return
            val status = context.respond(
                Embeds.info(
                    "Bitte gebe den Inhalt an!",
                    "Bitte gebe den Inhalt des Tags in einer neuen Nachricht an."
                )
            )

            val content = context.readSafe(Duration.minutes(3))?.content ?: return run { status.timeout() }

            val tag = transaction {
                Tag.new(name) {
                    this.content = content
                    author = context.author.id
                }
            }
            context.acknowledgement.followUp(
                Embeds.success(
                    "Erfolgreich erstellt!",
                    "Der Tag mit dem Namen `${tag.name}` wurde erfolgreich erstellt."
                )
            )
        }
    }

    private inner class AliasCommand : AbstractSubCommand.Command(this) {
        override val name: String = "alias"
        override val description: String = "Erstellt einen neuen Tag Alias"

        override fun SubCommandBuilder.applyOptions() {
            string("alias", "Der Name der Alias für den Tag") {
                required = true
            }

            string("tag", "Der Name des Tags für den die Alias erstellt werden soll") {
                required = true
            }
        }

        override suspend fun execute(context: Context) {
            val args = context.args
            val aliasName = args.string("alias")
            val tagName = args.string("tag")
            val tag = newSuspendedTransaction { checkNotTagExists(tagName, context) } ?: return
            if (newSuspendedTransaction { checkTagExists(aliasName, context) }) return
            val (newAliasName, newTagName) = transaction {
                val alias = TagAlias.new(aliasName) {
                    this.tag = tag
                }
                alias.name to alias.tag.name
            }
            context.respond(
                Embeds.success(
                    "Alias erfolgreich erstellt",
                    "Es wurde erfolgreich ein Alias mit dem Namen `$newAliasName` für den Tag `$newTagName` erstellt"
                )
            )
        }
    }

    private inner class EditCommand : AbstractSubCommand.Command(this) {
        override val name: String = "edit"
        override val description: String = "Editiert einen existierenden Tag"

        override fun SubCommandBuilder.applyOptions() {
            string("name", "Der Name des zu berarbeitenden Tags") {
                required = true
            }

            string("content", "Der neue Inhalt des Tags") {
                required = true
            }
        }

        override suspend fun execute(context: Context) {
            val (name, content) = parseTag(context) ?: return
            val tag = newSuspendedTransaction { checkNotTagExists(name, context) } ?: return
            if (checkPermission(tag, context)) return
            transaction {
                tag.content = content
            }
            context.respond(
                Embeds.success(
                    "Tag erfolgreich bearbeitet!",
                    "Du hast den Tag mit dem Namen `${tag.name}` erfolgreich bearbeitet."
                )
            )
        }
    }

    private inner class InfoCommand : AbstractSubCommand.Command(this) {
        override val name: String = "info"
        override val description: String = "Zeigt Informationen über einen Tag an"

        override fun SubCommandBuilder.applyOptions() {
            string("tag", "Der Name des Tags für den eine Info angezeigt werden soll") {
                required = true
            }
        }

        override suspend fun execute(context: Context) {
            val args = context.args
            val name = args.string("tag")
            val tag = newSuspendedTransaction { checkNotTagExists(name, context) } ?: return
            val rank = transaction {
                Tags.select { (Tags.usages greaterEq tag.usages) }.count()
            }
            val author = context.kord.getUser(tag.author)
            context.respond(
                embed {
                    color = Colors.BLUE
                    val creator = author?.username ?: "Mysteriöse Person"
                    author {
                        this.name = creator
                        icon = author?.effectiveAvatarUrl
                    }
                    field {
                        this.name = "Erstellt von"
                        value = creator
                        inline = true
                    }
                    field {
                        this.name = "Benutzungen"
                        value = tag.usages.toString()
                        inline = true
                    }
                    field {
                        this.name = "Rang"
                        value = rank.toString()
                        inline = true
                    }
                    field {
                        this.name = "Erstellt"
                        value = Constants.DATE_FORMAT.format(tag.createdAt)
                    }
                    transaction {
                        val aliases = TagAlias.find { TagAliases.tag eq tag.name }
                        if (!aliases.empty()) {
                            field {
                                this.name = "Aliase"
                                value = aliases.joinToString(
                                    prefix = "`",
                                    separator = "`, `",
                                    postfix = "`"
                                ) { it.name }
                                inline = true
                            }
                        }
                    }
                }
            )
        }
    }

    private inner class DeleteCommand : AbstractSubCommand.Command(this) {
        override val name: String = "delete"
        override val description: String = "Löscht einen Tag"

        override fun SubCommandBuilder.applyOptions() {
            string("tag", "Der Name des Tags der gelöscht soll") {
                required = true
            }
        }

        override suspend fun execute(context: Context) {
            val tag = newSuspendedTransaction { checkNotTagExists(context.args.string("tag"), context) } ?: return
            if (checkPermission(tag, context)) return

            transaction {
                TagAliases.deleteWhere { TagAliases.tag.eq(tag.name) }
                tag.delete()
            }

            context.respond(
                Embeds.success(
                    "Tag erfolgreich gelöscht!",
                    "Du hast den Tag mit dem Namen `${tag.name}` erfolgreich gelöscht."
                )
            )
        }
    }

    private inner class TransferCommand : AbstractSubCommand.Command(this) {
        override val name: String = "transfer"
        override val description: String = "Überschreibt einen Tag an einen anderen Benutzer"

        override fun SubCommandBuilder.applyOptions() {
            user("target", "Der neue Besitzer des Tags") {
                required = true
            }

            string("tag", "Der Name des Tags der übertragen werden soll") {
                required = true
            }
        }

        override suspend fun execute(context: Context) {
            val args = context.args
            val user = args.user("target")

            val tagName = args.string("tag")
            val tag = newSuspendedTransaction { checkNotTagExists(tagName, context) } ?: return

            if (checkPermission(tag, context)) return

            transaction {
                tag.author = user.id
            }

            context.respond(
                Embeds.success(
                    "Tag erfolgreich überschrieben!",
                    "Der Tag wurde erfolgreich an ${user.mention} überschrieben."
                )
            )
        }
    }

    private inner class ListCommand : AbstractSubCommand.Command(this) {
        override val name: String = "list"
        override val description: String = "Gibt eine Liste aller Tags aus"
        override val commandPlace: CommandPlace = CommandPlace.GUILD_MESSAGE

        override suspend fun execute(context: Context) {
            val tags = transaction { Tag.all().orderBy(Tags.usages to SortOrder.DESC).map(Tag::name) }
            if (tags.isEmpty()) {
                context.respond(Embeds.error("Keine Tags gefunden!", "Es gibt keine Tags."))
                return
            }
            Paginator(tags, context.author.asUser(), context, "Tags")
        }
    }

    private inner class FromCommand : AbstractSubCommand.Command(this) {
        override val name: String = "from"
        override val description: String = "Gibt eine Liste aller Tags eines bestimmten Benutzers aus"
        override val commandPlace: CommandPlace = CommandPlace.GUILD_MESSAGE

        override fun SubCommandBuilder.applyOptions() {
            user("author", "Der Benutzer für den die Tags angezeigt werden sollen")
        }

        override suspend fun execute(context: Context) {
            val user = context.args.optionalUser("author") ?: context.author
            val tags = transaction { Tag.find { Tags.author eq user.id }.map(Tag::name) }
            if (tags.isEmpty()) {
                context.respond(Embeds.error("Keine Tags gefunden!", "Es gibt keine Tags von diesem User."))
                return
            }
            val author = context.author.asUser()
            Paginator(tags, author, context, "Tags von ${author.username}")
        }
    }

    private inner class SearchCommand : AbstractSubCommand.Command(this) {
        override val name: String = "search"
        override val description: String = "Gibt die ersten 25 Tags mit dem angegebenen Namen"
        override val commandPlace: CommandPlace = CommandPlace.GUILD_MESSAGE

        override fun SubCommandBuilder.applyOptions() {
            string("query", "Die Query nach der gesucht werden soll") {
                required = true
            }
        }

        override suspend fun execute(context: Context) {
            val name = context.args.string("query")
            val tags = transaction {
                Tag.find { Tags.name similar name }.orderBy(similarity(Tags.name, name) to SortOrder.DESC).limit(25)
                    .map(Tag::name)
            }
            if (tags.isEmpty()) {
                context.respond(Embeds.error("Keine Tags gefunden!", "Es gibt keine Tags von diesem Namen."))
                return
            }
            Paginator(tags, context.author.asUser(), context, "Suche für $name")
        }
    }

    private inner class RawCommand : AbstractSubCommand.Command(this) {
        override val name: String = "raw"
        override val description: String = "Zeigt dir einen Tag ohne Markdown an"

        override fun SubCommandBuilder.applyOptions() {
            string("tag", "Der Name des Tags der unformatiert angezeigt werden soll") {
                required = true
            }
        }

        override suspend fun execute(context: Context) {
            val tagName = context.args.string("tag")
            val tag = newSuspendedTransaction { checkNotTagExists(tagName, context) } ?: return
            val content =
                MarkdownSanitizer.escape(tag.content).replace("\\```", "\\`\\`\\`") // Discords markdown renderer suxx
            if (content.length > MAX_CONTENT_LENGTH) {
                val message = context.respond(Emotes.LOADING)
                val code = HastebinUtil.postErrorToHastebin(content, context.bot.httpClient)
                message.edit { this.content = code }
                return
            }
            context.respond(content)
        }
    }

    private fun Tag.Companion.findByName(name: String) = findByNameId(name) ?: TagAlias.findById(name)?.tag

    private suspend fun checkPermission(
        tag: Tag,
        context: Context
    ): Boolean {
        if (tag.author != context.author.id && !context.hasModerator()) {
            context.respond(
                Embeds.error(
                    "Keine Berechtigung!",
                    "Nur Teammitglieder können nicht selbst-erstelle Tags bearbeiten."
                )
            )
            return true
        }
        return false
    }

    private suspend fun parseTag(
        context: Context,
        tagParam: String = "name",
        contentParam: String = "content"
    ): Pair<String, String>? {
        val args = context.args
        val name = args.string(tagParam)
        val content = args.string(contentParam)
        if (name.isBlank() or content.isBlank()) {
            context.sendHelp()
            return null
        }
        if (checkNameLength(name, context) or checkReservedName(name, context)) return null

        return name to content
    }

    private suspend fun checkTagExists(name: String, context: Context): Boolean {
        val tag = Tag.findByName(name)
        if (tag != null) {
            context.respond(
                Embeds.error(
                    "Tag existiert bereits!", "Dieser Tag existiert bereits."
                )
            )
            return true
        }
        return false
    }

    private suspend fun checkNotTagExists(name: String, context: Context): Tag? {
        val foundTag = Tag.findByName(name)
        return if (foundTag != null) foundTag else {
            val similarTag =
                Tag.find { Tags.name similar name }.orderBy(similarity(Tags.name, name) to SortOrder.DESC).firstOrNull()
            val similarTagHint = if (similarTag != null) " Meintest du vielleicht `${similarTag.name}`?" else ""
            context.respond(
                Embeds.error(
                    "Tag nicht gefunden!",
                    "Es wurde kein Tag mit dem Namen `$name` gefunden.$similarTagHint"
                )
            )
            return null
        }
    }

    private suspend fun checkNameLength(name: String, context: Context): Boolean {
        if (name.length > Tag.NAME_MAX_LENGTH) {
            context.respond(
                Embeds.error(
                    "Zu langer Name!",
                    "Der Name darf maximal ${Tag.NAME_MAX_LENGTH} Zeichen lang sein."
                )
            )
            return true
        }
        return false
    }

    private suspend fun checkReservedName(name: String, context: Context): Boolean {
        if (name.split(' ').first() in reservedNames) {
            context.respond(
                Embeds.error(
                    "Reservierter Name!",
                    "Die folgenden Namen sind reserviert `${reservedNames.joinToString()}`."
                )
            )
            return true
        }
        return false
    }
}
