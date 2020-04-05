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

package com.github.seliba.devcordbot.core

import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.database.StarboardEntries
import com.github.seliba.devcordbot.database.StarboardEntry
import com.github.seliba.devcordbot.database.Starrer
import com.github.seliba.devcordbot.database.Starrers
import com.github.seliba.devcordbot.dsl.embed
import com.github.seliba.devcordbot.dsl.sendMessage
import com.github.seliba.devcordbot.event.EventSubscriber
import com.github.seliba.devcordbot.util.await
import kotlinx.coroutines.future.await
import mu.KotlinLogging
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveAllEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.ErrorResponseException.ignore
import net.dv8tion.jda.api.requests.ErrorResponse
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.util.concurrent.CompletableFuture
import kotlin.math.min

/**
 * Starboard.
 * @param starBoardChannelId id of the logging channel
 */
@Suppress("unused")
class Starboard(private val starBoardChannelId: Long) {

    private val logger = KotlinLogging.logger { }

    private val entriesToDelete = mutableListOf<Long>()

    /**
     * Listens for reaction clears.
     */
    // We don't have the ability to count removed reactions to clearing reactions just acts like deleting the message
    @EventSubscriber
    fun reactionClear(event: GuildMessageReactionRemoveAllEvent): Unit =
        deleteStarboardEntry(event.messageIdLong, event.guild)

    /**
     * Listens for reactions.
     */
    @EventSubscriber
    suspend fun reactionAdd(event: GuildMessageReactionAddEvent): Unit = handleRactionUpdate(event, false)

    /**
     * Listens for reaction removals.
     */
    @EventSubscriber
    suspend fun reactionRemove(event: GuildMessageReactionRemoveEvent): Unit = handleRactionUpdate(event, true)

    /**
     * Listens for starboard message edits.
     */
    @EventSubscriber
    fun starboardMessageEdited(event: GuildMessageUpdateEvent) {
        val entry =
            transaction { StarboardEntry.find { StarboardEntries.messageId eq event.messageIdLong }.firstOrNull() }
                ?: return
        val trackingMessageRetriever =
            event.guild.getTextChannelById(starBoardChannelId)?.retrieveMessageById(entry.botMessageId)
                ?: return transaction {
                    entry.delete()
                }
        trackingMessageRetriever.queue({
            transaction {
                it.editMessage(buildMessage(event.message, entry.starrers.count())).queue()
            }
        }, {
            if (it is ErrorResponseException && it.errorResponse == ErrorResponse.UNKNOWN_MESSAGE) {
                transaction {
                    entry.delete()
                }
            }
        })

    }

    private suspend fun handleRactionUpdate(event: GenericGuildMessageReactionEvent, remove: Boolean) {
        if (event.reactionEmote.isEmote || event.reactionEmote.emoji != REACTION_EMOJI) return // Check for correct emote
        val potentialEntryMessage =
            event.channel.retrieveMessageById(event.messageIdLong).await()
        val foundEntry = findEntry(event.messageIdLong) // Search for entry
        if (foundEntry == null && remove) return // In case "self-starboarding" prevention removed the reaction
        if (event.user == potentialEntryMessage.author && foundEntry == null && !remove) {
            event.user?.let {
                event.reaction.removeReaction(it).queue()
            }
            event.channel.sendMessage("Ist da einer Star-süchtig?")
                .delay(Duration.ofSeconds(10))
                .flatMap(Message::delete)
                .queue()
            return
        }
        val trackingMessage = when {
            foundEntry?.botMessageId == event.messageIdLong -> CompletableFuture.completedFuture(
                potentialEntryMessage
            ).await()
            // send a new message in case there is none
            foundEntry == null -> {
                event.guild.getTextChannelById(starBoardChannelId)
                    ?.sendMessage(
                        Embeds.loading(
                            "Neuer Starboard Eintrag!",
                            "Bitte warte noch einen Augenblick."
                        )
                    )?.await()
            }
            else -> {
                // Retrieve the old message
                event.guild.getTextChannelById(starBoardChannelId)?.retrieveMessageById(foundEntry.botMessageId)
                    ?.await()
            }
        }
        if (trackingMessage == null) {
            logger.warn { "The starboard channel could not be found! ${event.messageId}" }
            return
        }
        @Suppress("unused") // it is used :D
        val entry = transaction(statement = fun Transaction.(): StarboardEntry? {
            return foundEntry
            // Create new entry if needed
                ?: return StarboardEntry.new {
                    messageId = event.messageIdLong
                    authorId = potentialEntryMessage.author.idLong
                    botMessageId = trackingMessage.idLong
                    channelId = potentialEntryMessage.channel.idLong
                }
        })
            ?: return
        // Register new starrer
        transaction {
            val foundStarrer =
                Starrer.find { (Starrers.entry eq entry.id) and (Starrers.authorId eq event.userIdLong) }
                    .firstOrNull()
            if (remove) {
                //Check if starrer reacted twice
                if (foundStarrer != null && foundStarrer.emojis > 1) {
                    foundStarrer.emojis--
                    return@transaction
                }
                foundStarrer?.delete()
            } else {
                if (foundStarrer != null) {
                    foundStarrer.emojis++
                    return@transaction
                }
                Starrer.new {
                    authorId = event.userIdLong
                    this.entry = entry
                }
            }
        }
        val starsTotal = transaction { entry.countStarrers() }
        if (starsTotal <= 0) {
            deleteStarboardEntry(potentialEntryMessage.idLong, event.guild)
            return
        }

        val actualMessage = if (potentialEntryMessage == trackingMessage) {
            event.guild.getTextChannelById(entry.channelId)?.retrieveMessageById(entry.messageId)?.await()
        } else {
            CompletableFuture.completedFuture(potentialEntryMessage).await()
        }
        if (actualMessage == null) {
            deleteStarboardEntry(potentialEntryMessage.idLong, event.guild)
            return
        }

        trackingMessage.editMessage(buildMessage(actualMessage, starsTotal)).queue()
    }

    /**
     * Listens for message bulk deletions.
     */
    @EventSubscriber
    fun messagesDeleted(event: MessageBulkDeleteEvent): Unit =
        event.messageIds.forEach {
            deleteStarboardEntry(
                it.toLong(), // Why is there no messageIdLongs JDA??
                event.guild
            )
        }

    /**
     * Listens for message deletions.
     */
    @EventSubscriber
    fun messageDeleted(event: MessageDeleteEvent): Unit = deleteStarboardEntry(event.messageIdLong, event.guild)

    /**
     * Deletes a starboard entry on [guild] by [messageId].
     */
    fun deleteStarboardEntry(messageId: Long, guild: Guild) {
        val entry = findEntry(messageId) ?: return
        val entryId = entry.id.value
        if (entryId in entriesToDelete) return
        entriesToDelete.add(entryId)
        guild.getTextChannelById(entry.channelId)?.clearReactionsById(entry.messageId)
            ?.queue(null, ignore(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.UNKNOWN_CHANNEL))
        guild.getTextChannelById(starBoardChannelId)?.retrieveMessageById(entry.botMessageId)
            ?.flatMap { it.delete() }?.queue(null, ignore(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.UNKNOWN_CHANNEL))
        transaction {
            entry.starrers.forEach { it.delete() }
            entry.delete()
        }
        entriesToDelete.remove(entryId)
    }

    private fun findEntry(messageId: Long): StarboardEntry? {
        return transaction {
            StarboardEntry.find { (StarboardEntries.messageId eq messageId) or (StarboardEntries.botMessageId eq messageId) }
                .firstOrNull()
        }
    }

    // Skidded from Vale.py :D Source. https://look-at.it/brw
    private fun calculateGradientColor(stars: Int): Int {
        val percentage = min((stars / 13.0), 1.0) // Limit at 1.0 (100%)
        val red = 255
        val green = ((194 * percentage) + (253 * (1 - percentage))).toInt()
        val blue = ((12 * percentage) + (247 * (1 - percentage))).toInt()
        return (red shl 16) + (green shl 8) + blue
    }

    private fun starEmoji(stars: Int): String {
        return when (stars) {
            in 0..5 -> ":star:"
            in 5..10 -> ":star2:"
            in 10..25 -> ":dizzy:"
            else -> ":sparkles:"
        }
    }

    private fun buildMessage(message: Message, stars: Int): Message {
        val emoji = starEmoji(stars)
        val content = if (stars > 1) {
            "$emoji **$stars** ${message.textChannel.asMention} ID: ${message.id}"
        } else {
            "$emoji ${message.textChannel.asMention} ID: ${message.id}"
        }
        val messageBuilder = MessageBuilder(content)
        val embed = embed {

            title {
                title = "Jump to message"
                url = message.jumpUrl
            }

            description = message.contentRaw
            color = color(calculateGradientColor(stars))
            image = message.embeds.firstOrNull()?.image?.url
            timeStamp = message.timeCreated

            author {
                name = message.author.name
                iconUrl = message.author.avatarUrl
            }

            val attachment = message.attachments.firstOrNull()
            if (attachment != null) {
                if (attachment.isImage) {
                    image = attachment.url
                } else {
                    addField("Attachment:", "[${attachment.fileName}](${attachment.url})")
                }
            }
        }.toEmbedBuilder().build()
        messageBuilder.setEmbed(embed)
        return messageBuilder.build()
    }

    companion object {
        /**
         * Emoji that adds messages to starboard.
         */
        const val REACTION_EMOJI: String = "⭐"
    }
}
