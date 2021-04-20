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
import com.github.devcordde.devcordbot.constants.Colors
import com.github.devcordde.devcordbot.constants.Constants
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.constants.Emotes
import com.github.devcordde.devcordbot.database.*
import com.github.devcordde.devcordbot.dsl.embed
import com.github.devcordde.devcordbot.dsl.sendMessage
import com.github.devcordde.devcordbot.menu.Paginator
import com.github.devcordde.devcordbot.util.*
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

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

    fun registerReadCommand(commandClient: CommandClient) = commandClient.registerCommands(TagReadCommand())

    private inner class TagReadCommand : AbstractSingleCommand() {
        override val name: String = "t"
        override val description: String = "Zeigt dir einen Tag an"
        override val permission: Permission = Permission.ANY
        override val category: CommandCategory = CommandCategory.GENERAL
        override val commandPlace: CommandPlace = CommandPlace.ALL
        override val options: List<CommandUpdateAction.OptionData> = buildOptions {
            string("tag", "Der Name des Tags welcher angezeigt werden soll") {
                isRequired = true
            }
        }

        override suspend fun execute(context: Context) {
            val tagName = context.args.string("tag")
            val tag = transaction { checkNotTagExists(tagName, context) } ?: return
            context.respond(tag.content)
                .allowedMentions(listOf(MentionType.ROLE, MentionType.EMOTE, MentionType.CHANNEL))
                .queue()
            transaction {
                tag.usages++
            }
        }
    }

    private inner class CreateCommand : AbstractSubCommand.Command(this) {
        override val name: String = "create"
        override val description: String = "Erstellt einen neuen Tag"
        override val options: List<CommandUpdateAction.OptionData> = buildOptions {
            string("name", "Der Name des zu erstellenden Tags") {
                isRequired = true
            }
        }

        @OptIn(ExperimentalTime::class)
        override suspend fun execute(context: Context) {
            val name = context.args.string("name")
            if (transaction { checkTagExists(name, context) }) return
            val status = context.respond(
                Embeds.info(
                    "Bitte gebe den Inhalt an!",
                    "Bitte gebe den Inhalt des Tags in einer neuen Nachricht an."
                )
            ).await()

            val content = context.readSafe(3.minutes)?.contentRaw ?: return status.timeout().queue()

            val tag = transaction {
                Tag.new(name) {
                    this.content = content
                    author = context.author.idLong
                }
            }
            context.ack.sendMessage(
                Embeds.success(
                    "Erfolgreich erstellt!",
                    "Der Tag mit dem Namen `${tag.name}` wurde erfolgreich erstellt."
                )
            ).queue()
        }
    }

    private inner class AliasCommand : AbstractSubCommand.Command(this) {
        override val name: String = "alias"
        override val description: String = "Erstellt einen neuen Tag Alias"
        override val options: List<CommandUpdateAction.OptionData> = buildOptions {
            string("alias", "Der Name der Alias für den Tag") {
                isRequired = true
            }

            string("tag", "Der Name des Tags für den die Alias erstellt werden soll") {
                isRequired = true
            }
        }

        override suspend fun execute(context: Context) {
            val args = context.args
            val aliasName = args.string("alias")
            val tagName = args.string("tag")
            val tag = transaction { checkNotTagExists(tagName, context) } ?: return
            if (transaction { checkTagExists(aliasName, context) }) return
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
            ).queue()
        }
    }

    private inner class EditCommand : AbstractSubCommand.Command(this) {
        override val name: String = "edit"
        override val description: String = "Editiert einen existierenden Tag"

        override val options: List<CommandUpdateAction.OptionData> = buildOptions {
            string("name", "Der Name des zu berarbeitenden Tags") {
                isRequired = true
            }

            string("content", "Der neue Inhalt des Tags") {
                isRequired = true
            }
        }

        override suspend fun execute(context: Context) {
            val (name, content) = parseTag(context) ?: return
            val tag = transaction { checkNotTagExists(name, context) } ?: return
            if (checkPermission(tag, context)) return
            transaction {
                tag.content = content
            }
            context.respond(
                Embeds.success(
                    "Tag erfolgreich bearbeitet!",
                    "Du hast den Tag mit dem Namen `${tag.name}` erfolgreich bearbeitet."
                )
            ).queue()
        }
    }

    private inner class InfoCommand : AbstractSubCommand.Command(this) {
        override val name: String = "info"
        override val description: String = "Zeigt Informationen über einen Tag an"

        override val options: List<CommandUpdateAction.OptionData> = buildOptions {
            string("tag", "Der Name des Tags für den eine Info angezeigt werden soll") {
                isRequired = true
            }
        }

        override suspend fun execute(context: Context) {
            val args = context.args
            val name = args.string("tag")
            val tag = transaction { checkNotTagExists(name, context) } ?: return
            val rank = transaction {
                Tags.select { (Tags.usages greaterEq tag.usages) }.count()
            }
            val author = context.jda.getUserById(tag.author)
            context.respond(
                embed {
                    color = Colors.BLUE
                    val creator = author?.name ?: "Mysteriöse Person"
                    author {
                        this.name = creator
                        iconUrl = author?.avatarUrl
                    }
                    addField("Erstellt von", creator, inline = true)
                    addField("Benutzungen", tag.usages.toString(), inline = true)
                    addField("Rang", rank.toString(), inline = true)
                    addField("Erstellt", Constants.DATE_FORMAT.format(tag.createdAt))
                    transaction {
                        val aliases = TagAlias.find { TagAliases.tag eq tag.name }
                        if (!aliases.empty()) {
                            addField(
                                "Aliase",
                                aliases.joinToString(
                                    prefix = "`",
                                    separator = "`, `",
                                    postfix = "`"
                                ) { it.name },
                                inline = true
                            )
                        }
                    }
                }
            ).queue()
        }

    }

    private inner class DeleteCommand : AbstractSubCommand.Command(this) {
        override val name: String = "delete"
        override val description: String = "Löscht einen Tag"

        override val options: List<CommandUpdateAction.OptionData> = buildOptions {
            string("tag", "Der Name des Tags der gelöscht soll") {
                isRequired = true
            }
        }

        override suspend fun execute(context: Context) {
            val tag = transaction { checkNotTagExists(context.args.string("tag"), context) } ?: return
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
            ).queue()
        }
    }

    private inner class TransferCommand : AbstractSubCommand.Command(this) {
        override val name: String = "transfer"
        override val description: String = "Überschreibt einen Tag an einen anderen Benutzer"

        override val options: List<CommandUpdateAction.OptionData> = buildOptions {
            user("target", "Der neue Besitzer des Tags") {
                isRequired = true
            }

            string("tag", "Der Name des Tags der übertragen werden soll") {
                isRequired = true
            }
        }

        override suspend fun execute(context: Context) {
            val args = context.args
            val user = args.user("target")

            val tagName = args.string("tag")
            val tag = transaction { checkNotTagExists(tagName, context) } ?: return

            if (checkPermission(tag, context)) return

            transaction {
                tag.author = user.idLong
            }

            return context.respond(
                Embeds.success(
                    "Tag erfolgreich überschrieben!",
                    "Der Tag wurde erfolgreich an ${user.asMention} überschrieben."
                )
            ).queue()
        }
    }

    private inner class ListCommand : AbstractSubCommand.Command(this) {
        override val name: String = "list"
        override val description: String = "Gibt eine Liste aller Tags aus"
        override val commandPlace: CommandPlace = CommandPlace.GUILD_MESSAGE

        override suspend fun execute(context: Context) {
            val tags = transaction { Tag.all().orderBy(Tags.usages to SortOrder.DESC).map(Tag::name) }
            if (tags.isEmpty()) {
                return context.respond(Embeds.error("Keine Tags gefunden!", "Es gibt keine Tags.")).queue()
            }
            Paginator(tags, context.author, context, "Tags")
        }
    }

    private inner class FromCommand : AbstractSubCommand.Command(this) {
        override val name: String = "from"
        override val description: String = "Gibt eine Liste aller Tags eines bestimmten Benutzers aus"
        override val commandPlace: CommandPlace = CommandPlace.GUILD_MESSAGE

        override val options: List<CommandUpdateAction.OptionData> = buildOptions {
            user("author", "Der Benutzer für den die Tags angezeigt werden sollen")
        }

        override suspend fun execute(context: Context) {
            val user = context.args.optionalUser("author") ?: context.author
            val tags = transaction { Tag.find { Tags.author eq user.idLong }.map(Tag::name) }
            if (tags.isEmpty()) {
                return context.respond(Embeds.error("Keine Tags gefunden!", "Es gibt keine Tags von diesem User."))
                    .queue()
            }
            Paginator(tags, context.author, context, "Tags von ${user.name}")
        }
    }

    private inner class SearchCommand : AbstractSubCommand.Command(this) {
        override val name: String = "search"
        override val description: String = "Gibt die ersten 25 Tags mit dem angegebenen Namen"
        override val commandPlace: CommandPlace = CommandPlace.GUILD_MESSAGE

        override val options: List<CommandUpdateAction.OptionData> = buildOptions {
            string("query", "Die Query nach der gesucht werden soll") {
                isRequired = true
            }
        }

        override suspend fun execute(context: Context) {
            val name = context.args.string("query")
            val tags = transaction {
                Tag.find { Tags.name similar name }.orderBy(similarity(Tags.name, name) to SortOrder.DESC).limit(25)
                    .map(Tag::name)
            }
            if (tags.isEmpty()) {
                return context.respond(Embeds.error("Keine Tags gefunden!", "Es gibt keine Tags von diesem Namen."))
                    .queue()
            }
            Paginator(tags, context.author, context, "Suche für $name")
        }
    }

    private inner class RawCommand : AbstractSubCommand.Command(this) {
        override val name: String = "raw"
        override val description: String = "Zeigt dir einen Tag ohne Markdown an"

        override val options: List<CommandUpdateAction.OptionData> = buildOptions {
            string("tag", "Der Name des Tags der unformatiert angezeigt werden soll") {
                isRequired = true
            }
        }

        override suspend fun execute(context: Context) {
            val tagName = context.args.string("tag")
            val tag = transaction { checkNotTagExists(tagName, context) } ?: return
            val content =
                MarkdownSanitizer.escape(tag.content).replace("\\```", "\\`\\`\\`") // Discords markdown renderer suxx
            if (content.length > Message.MAX_CONTENT_LENGTH) {
                val message = context.respond(Emotes.LOADING).await()
                val code = HastebinUtil.postErrorToHastebin(content, context.bot.httpClient)
                return message.editMessage(code).queue()
            }
            context.respond(content).queue()
        }
    }

    private fun Tag.Companion.findByName(name: String) = findByNameId(name) ?: TagAlias.findById(name)?.tag

    private fun checkPermission(
        tag: Tag,
        context: Context
    ): Boolean {
        if (tag.author != context.author.idLong && !context.hasModerator()) {
            context.respond(
                Embeds.error(
                    "Keine Berechtigung!",
                    "Nur Teammitglieder können nicht selbst-erstelle Tags bearbeiten."
                )
            ).queue()
            return true
        }
        return false
    }

    private fun parseTag(
        context: Context,
        tagParam: String = "name",
        contentParam: String = "content"
    ): Pair<String, String>? {
        val args = context.args
        val name = args.string(tagParam)
        val content = args.string(contentParam)
        if (name.isBlank() or content.isBlank()) {
            context.sendHelp().queue()
            return null
        }
        if (checkNameLength(name, context) or checkReservedName(name, context)) return null

        return name to content
    }

    private fun checkTagExists(name: String, context: Context): Boolean {
        val tag = Tag.findByName(name)
        if (tag != null) {
            context.respond(
                Embeds.error(
                    "Tag existiert bereits!", "Dieser Tag existiert bereits."
                )
            ).queue()
            return true
        }
        return false

    }

    private fun checkNotTagExists(name: String, context: Context): Tag? {
        val foundTag = Tag.findByName(name)
        return if (foundTag != null) foundTag else {
            val similarTag =
                Tag.find { Tags.name similar name }.orderBy(similarity(Tags.name, name) to SortOrder.DESC).firstOrNull()
            val similarTagHint = if (similarTag != null) " Meintest du vielleicht `${similarTag.name}`?" else ""
            return context.respond(
                Embeds.error(
                    "Tag nicht gefunden!",
                    "Es wurde kein Tag mit dem Namen `${name}` gefunden.$similarTagHint"
                )
            ).queue().run { null }
        }
    }

    private fun checkNameLength(name: String, context: Context): Boolean {
        if (name.length > Tag.NAME_MAX_LENGTH) {
            context.respond(
                Embeds.error(
                    "Zu langer Name!",
                    "Der Name darf maximal ${Tag.NAME_MAX_LENGTH} Zeichen lang sein."
                )
            ).queue()
            return true
        }
        return false
    }

    private fun checkReservedName(name: String, context: Context): Boolean {
        if (name.split(' ').first() in reservedNames) {
            context.respond(
                Embeds.error(
                    "Reservierter Name!",
                    "Die folgenden Namen sind reserviert `${reservedNames.joinToString()}`."
                )
            ).queue()
            return true
        }
        return false
    }
}
