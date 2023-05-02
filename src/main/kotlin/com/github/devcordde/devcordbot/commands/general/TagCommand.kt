/*
 * Copyright 2021 Daniel Scherf & Michael Rittmeister & Julian König
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
import com.github.devcordde.devcordbot.command.root.AbstractRootCommand
import com.github.devcordde.devcordbot.command.root.AbstractSingleCommand
import com.github.devcordde.devcordbot.constants.*
import com.github.devcordde.devcordbot.database.*
import com.github.devcordde.devcordbot.dsl.embed
import com.github.devcordde.devcordbot.menu.Paginator
import com.github.devcordde.devcordbot.util.HastebinUtil
import com.github.devcordde.devcordbot.util.effectiveAvatarUrl
import com.github.devcordde.devcordbot.util.readSafe
import com.github.devcordde.devcordbot.util.timeout
import dev.kord.core.behavior.interaction.InteractionResponseBehavior
import dev.kord.core.behavior.interaction.PublicInteractionResponseBehavior
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.SubCommandBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

/**
 * Tag command.
 */
class TagCommand : AbstractRootCommand() {
    private val reservedNames: List<String>
    override val name: String = "tag"
    override val description: String = "Zeigt einen Tag an."
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

    private inner class TagReadCommand : AbstractSingleCommand<InteractionResponseBehavior>() {
        override val name: String = "t"
        override val description: String = "Zeigt einen Tag an."
        override val permission: Permission = Permission.ANY
        override val category: CommandCategory = CommandCategory.GENERAL
        override val commandPlace: CommandPlace = CommandPlace.ALL

        override suspend fun InteractionCreateEvent.acknowledge(): InteractionResponseBehavior =
            interaction.acknowledgePublic()

        override fun ChatInputCreateBuilder.applyOptions() {
            string("tag", "Der Name des Tags, welcher angezeigt werden soll") {
                required = true
            }
        }

        override suspend fun execute(context: Context<InteractionResponseBehavior>) {
            val tagName = context.args.string("tag")
            val tag = newSuspendedTransaction { checkNotTagExists(tagName, context) } ?: return
            context.respond(tag.content)
            newSuspendedTransaction {
                tag.usages++
            }
        }
    }

    private inner class CreateCommand : AbstractSubCommand.Command<InteractionResponseBehavior>(this) {
        override val name: String = "create"
        override val description: String = "Erstellt einen neuen Tag."

        override suspend fun InteractionCreateEvent.acknowledge(): InteractionResponseBehavior =
            interaction.acknowledgePublic()

        override fun SubCommandBuilder.applyOptions() {
            string("name", "Der Name des zu erstellenden Tags") {
                required = true
            }
        }

        @OptIn(ExperimentalTime::class)
        override suspend fun execute(context: Context<InteractionResponseBehavior>) {
            val name = context.args.string("name")
            if (newSuspendedTransaction { checkTagExists(name, context) }) return
            val status = context.respond(
                Embeds.info(
                    "Bitte gib den Inhalt an!",
                    "Bitte gib den Inhalt des Tags in einer neuen Nachricht an."
                )
            )

            val content = context.readSafe(3.minutes)?.content ?: return run { status.timeout() }

            val tag = newSuspendedTransaction {
                Tag.new(name) {
                    this.content = content
                    author = context.author.id
                }
            }

            context.bot.launch {
                val contentLink = HastebinUtil.postToHastebin(tag.content, context.bot.httpClient)
                context.bot.discordLogger.logEvent(context.author.asUser(), "TAG_CREATE") { "$name -> $contentLink" }
            }

            context.responseStrategy.followUp(
                Embeds.success(
                    "Erfolgreich erstellt!",
                    "Der Tag mit dem Namen `${tag.name}` wurde erfolgreich erstellt."
                )
            )
        }
    }

    private inner class AliasCommand : AbstractSubCommand.Command<InteractionResponseBehavior>(this) {
        override val name: String = "alias"
        override val description: String = "Erstellt einen neuen Alias für einen Tag."

        override fun SubCommandBuilder.applyOptions() {
            string("alias", "Der Name des Alias für den Tag") {
                required = true
            }

            string("tag", "Der Name des Tags, für den der Alias erstellt werden soll") {
                required = true
            }
        }

        override suspend fun InteractionCreateEvent.acknowledge(): InteractionResponseBehavior =
            interaction.acknowledgeEphemeral()

        override suspend fun execute(context: Context<InteractionResponseBehavior>) {
            val args = context.args
            val aliasName = args.string("alias")
            val tagName = args.string("tag")
            val tag = newSuspendedTransaction { checkNotTagExists(tagName, context) } ?: return
            if (newSuspendedTransaction { checkTagExists(aliasName, context) }) return
            val (newAliasName, newTagName) = newSuspendedTransaction {
                val alias = TagAlias.new(aliasName) {
                    this.tag = tag
                }
                alias.name to alias.tag.name
            }

            context.bot.discordLogger.logEvent(context.author.asUser(), "TAG_ALIAS") { "$name -> $aliasName" }

            context.respond(
                Embeds.success(
                    "Alias erfolgreich erstellt",
                    "Es wurde erfolgreich ein Alias mit dem Namen `$newAliasName` für den Tag `$newTagName` erstellt"
                )
            )
        }
    }

    private inner class EditCommand : AbstractSubCommand.Command<InteractionResponseBehavior>(this) {
        override val name: String = "edit"
        override val description: String = "Bearbeitet einen existierenden Tag."

        override fun SubCommandBuilder.applyOptions() {
            string("name", "Der Name des zu berarbeitenden Tags") {
                required = true
            }
        }

        override suspend fun InteractionCreateEvent.acknowledge(): InteractionResponseBehavior =
            interaction.acknowledgePublic()

        @OptIn(ExperimentalTime::class)
        override suspend fun execute(context: Context<InteractionResponseBehavior>) {
            val name = context.args.string("name")
            val tag = newSuspendedTransaction { checkNotTagExists(name, context) } ?: return
            if (checkPermission(tag, context)) return

            val status = context.respond(
                Embeds.info(
                    "Bitte gib den Inhalt an!",
                    "Bitte gib den Inhalt des Tags in einer neuen Nachricht an."
                )
            )

            val content = context.readSafe(3.minutes)?.content ?: return run { status.timeout() }

            val oldContent = tag.content

            newSuspendedTransaction {
                tag.content = content
            }

            context.bot.launch {
                val newContentLink = HastebinUtil.postToHastebin(content, context.bot.httpClient)
                val oldContentLink = HastebinUtil.postToHastebin(oldContent, context.bot.httpClient)
                context.bot.discordLogger.logEvent(
                    context.author.asUser(),
                    "TAG_UPDATE"
                ) { "$name ($oldContentLink)-> $newContentLink" }
            }

            context.respond(
                Embeds.success(
                    "Tag erfolgreich bearbeitet!",
                    "Du hast den Tag mit dem Namen `${tag.name}` erfolgreich bearbeitet."
                )
            )
        }
    }

    private inner class InfoCommand : AbstractSubCommand.Command<InteractionResponseBehavior>(this) {
        override val name: String = "info"
        override val description: String = "Zeigt Informationen zu einem Tag an."

        override fun SubCommandBuilder.applyOptions() {
            string("tag", "Der Name des Tags für den eine Info angezeigt werden soll") {
                required = true
            }
        }

        override suspend fun InteractionCreateEvent.acknowledge(): InteractionResponseBehavior =
            interaction.acknowledgeEphemeral()

        override suspend fun execute(context: Context<InteractionResponseBehavior>) {
            val args = context.args
            val name = args.string("tag")
            val tag = newSuspendedTransaction { checkNotTagExists(name, context) } ?: return
            val rank = newSuspendedTransaction {
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
                    newSuspendedTransaction {
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

    private inner class DeleteCommand : AbstractSubCommand.Command<InteractionResponseBehavior>(this) {
        override val name: String = "delete"
        override val description: String = "Löscht einen Tag."

        override fun SubCommandBuilder.applyOptions() {
            string("tag", "Der Name des Tags, der gelöscht soll") {
                required = true
            }
        }

        override suspend fun InteractionCreateEvent.acknowledge(): InteractionResponseBehavior =
            interaction.acknowledgeEphemeral()

        override suspend fun execute(context: Context<InteractionResponseBehavior>) {
            val tag = newSuspendedTransaction { checkNotTagExists(context.args.string("tag"), context) } ?: return
            if (checkPermission(tag, context)) return

            newSuspendedTransaction {
                TagAliases.deleteWhere { TagAliases.tag.eq(tag.name) }
                tag.delete()
            }

            context.bot.discordLogger.logEvent(context.author.asUser(), "TAG_DELETE") { name }

            context.respond(
                Embeds.success(
                    "Tag erfolgreich gelöscht!",
                    "Du hast den Tag mit dem Namen `${tag.name}` erfolgreich gelöscht."
                )
            )
        }
    }

    private inner class TransferCommand : AbstractSubCommand.Command<InteractionResponseBehavior>(this) {
        override val name: String = "transfer"
        override val description: String = "Überschreibt einen Tag an einen anderen Benutzer."

        override fun SubCommandBuilder.applyOptions() {
            user("target", "Der neue Besitzer des Tags") {
                required = true
            }

            string("tag", "Der Name des Tags, der übertragen werden soll") {
                required = true
            }
        }

        override suspend fun InteractionCreateEvent.acknowledge(): InteractionResponseBehavior =
            interaction.acknowledgePublic()

        override suspend fun execute(context: Context<InteractionResponseBehavior>) {
            val args = context.args
            val user = args.user("target")

            val tagName = args.string("tag")
            val tag = newSuspendedTransaction { checkNotTagExists(tagName, context) } ?: return

            if (checkPermission(tag, context)) return

            newSuspendedTransaction {
                tag.author = user.id
            }

            context.bot.discordLogger.logEvent(context.author.asUser(), "TAG_TRANSFER") {
                "$name -> ${user.tag} (${user.id})"
            }

            context.respond(
                Embeds.success(
                    "Tag erfolgreich überschrieben!",
                    "Der Tag wurde erfolgreich an ${user.mention} überschrieben."
                )
            )
        }
    }

    private inner class ListCommand : AbstractSubCommand.Command<PublicInteractionResponseBehavior>(this) {
        override val name: String = "list"
        override val description: String = "Zeigt eine Liste aller Tags an."
        override val commandPlace: CommandPlace = CommandPlace.GUILD_MESSAGE

        override suspend fun InteractionCreateEvent.acknowledge(): PublicInteractionResponseBehavior =
            interaction.acknowledgePublic()

        override suspend fun execute(context: Context<PublicInteractionResponseBehavior>) {
            val tags = newSuspendedTransaction { Tag.all().orderBy(Tags.usages to SortOrder.DESC).map(Tag::name) }
            if (tags.isEmpty()) {
                context.respond(Embeds.error("Keine Tags gefunden!", "Es gibt keine Tags."))
                return
            }
            Paginator(tags, context.author.asUser(), context, "Tags")
        }
    }

    private inner class FromCommand : AbstractSubCommand.Command<PublicInteractionResponseBehavior>(this) {
        override val name: String = "from"
        override val description: String = "Zeigt eine Liste aller Tags eines Nutzers an."
        override val commandPlace: CommandPlace = CommandPlace.GUILD_MESSAGE

        override fun SubCommandBuilder.applyOptions() {
            user("author", "Der Nutzer, dessen Tags angezeigt werden sollen")
        }

        override suspend fun InteractionCreateEvent.acknowledge(): PublicInteractionResponseBehavior =
            interaction.acknowledgePublic()

        override suspend fun execute(context: Context<PublicInteractionResponseBehavior>) {
            val user = context.args.optionalUser("author") ?: context.author
            val tags = newSuspendedTransaction { Tag.find { Tags.author eq user.id }.map(Tag::name) }
            if (tags.isEmpty()) {
                context.respond(Embeds.error("Keine Tags gefunden!", "Es gibt keine Tags von diesem Nutzer."))
                return
            }
            val author = context.author.asUser()
            Paginator(tags, author, context, "Tags von ${author.username}")
        }
    }

    private inner class SearchCommand : AbstractSubCommand.Command<PublicInteractionResponseBehavior>(this) {
        override val name: String = "search"
        override val description: String = "Zeigt die ersten 25 Tags mit dem angegebenen Namen an."
        override val commandPlace: CommandPlace = CommandPlace.GUILD_MESSAGE

        override suspend fun InteractionCreateEvent.acknowledge(): PublicInteractionResponseBehavior =
            interaction.acknowledgePublic()

        override fun SubCommandBuilder.applyOptions() {
            string("query", "Die Query, nach der gesucht werden soll") {
                required = true
            }
        }

        override suspend fun execute(context: Context<PublicInteractionResponseBehavior>) {
            val name = context.args.string("query")
            val tags = newSuspendedTransaction {
                Tag.find { Tags.name similar name }.orderBy(similarity(Tags.name, name) to SortOrder.DESC).limit(25)
                    .map(Tag::name)
            }
            if (tags.isEmpty()) {
                context.respond(Embeds.error("Keine Tags gefunden!", "Es gibt keine Tags mit diesem Namen."))
                return
            }
            Paginator(tags, context.author.asUser(), context, "Suche nach $name")
        }
    }

    private inner class RawCommand : AbstractSubCommand.Command<InteractionResponseBehavior>(this) {
        override val name: String = "raw"
        override val description: String = "Zeigt einen Tag ohne Markdown-Formatierung an."

        override suspend fun InteractionCreateEvent.acknowledge(): InteractionResponseBehavior =
            interaction.acknowledgeEphemeral()

        override fun SubCommandBuilder.applyOptions() {
            string("tag", "Der Name des Tags, der unformatiert angezeigt werden soll") {
                required = true
            }
        }

        override suspend fun execute(context: Context<InteractionResponseBehavior>) {
            val tagName = context.args.string("tag")
            val tag = newSuspendedTransaction { checkNotTagExists(tagName, context) } ?: return
            val content =
                MarkdownSanitizer.escape(tag.content).replace("\\```", "\\`\\`\\`") // Discords markdown renderer suxx
            if (content.length > MAX_CONTENT_LENGTH) {
                val message = context.respond(Emotes.LOADING)
                val code = HastebinUtil.postToHastebin(tag.content, context.bot.httpClient)
                message.edit { this.content = code }
                return
            }
            context.respond(content)
        }
    }

    private suspend fun checkPermission(
        tag: Tag,
        context: Context<InteractionResponseBehavior>
    ): Boolean {
        if (tag.author != context.author.id && !context.hasModerator()) {
            context.respond(
                Embeds.error(
                    "Keine Berechtigung!",
                    "Nur Teammitglieder können fremde Tags bearbeiten."
                )
            )
            return true
        }
        return false
    }

    private suspend fun checkTagExists(name: String, context: Context<InteractionResponseBehavior>): Boolean {
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

    private suspend fun checkNotTagExists(name: String, context: Context<InteractionResponseBehavior>): Tag? {
        if (checkNameLength(name, context)) return null
        if (checkReservedName(name, context)) return null

        val foundTag = Tag.findByIdentifier(name)
        return if (foundTag != null) foundTag else {
            val similarTag =
                Tag.find { Tags.name similar name }.orderBy(similarity(Tags.name, name) to SortOrder.DESC).firstOrNull()
            val similarTagHint = if (similarTag != null) " Meinst du vielleicht `${similarTag.name}`?" else ""
            context.respond(
                Embeds.error(
                    "Tag nicht gefunden!",
                    "Es wurde kein Tag mit dem Namen `$name` gefunden.$similarTagHint"
                )
            )
            return null
        }
    }

    private suspend fun checkNameLength(name: String, context: Context<InteractionResponseBehavior>): Boolean {
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

    private suspend fun checkReservedName(name: String, context: Context<InteractionResponseBehavior>): Boolean {
        if (name.split(' ').first() in reservedNames) {
            context.respond(
                Embeds.error(
                    "Reservierter Name!",
                    "Die folgenden Namen sind reserviert: `${reservedNames.joinToString()}`."
                )
            )
            return true
        }
        return false
    }
}
