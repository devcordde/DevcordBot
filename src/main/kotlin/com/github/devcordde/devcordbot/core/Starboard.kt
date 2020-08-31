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

package com.github.devcordde.devcordbot.core

import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.database.StarboardEntries
import com.github.devcordde.devcordbot.database.StarboardEntry
import com.github.devcordde.devcordbot.database.Starrer
import com.github.devcordde.devcordbot.database.Starrers
import com.github.devcordde.devcordbot.dsl.embed
import com.github.devcordde.devcordbot.dsl.sendMessage
import com.github.devcordde.devcordbot.event.EventSubscriber
import com.github.devcordde.devcordbot.util.await
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
import net.dv8tion.jda.api.requests.RestAction
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import kotlin.math.min

/**
 * Starboard.
 * @param starBoardChannelId id of the logging channel
 */
@Suppress("unused")
class Starboard(private val starBoardChannelId: Long, private val limit: Int) {

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
                val starrers = Starrer.find { Starrers.starredMessage eq event.messageIdLong }
                it.editMessage(buildMessage(event.message, starrers.count())).queue()
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
        val foundEntry = findEntry(event.messageIdLong)
        if (event.user?.idLong == foundEntry?.authorId) {
            return event.channel.sendMessage(
                "Ich weiß es ist wahnsinnig geil nen eigenen Stern zu haben aber hast du mal im Internet nachgeschaut wieviel das überhaupt kostet?? Bist du dir sicher, dass du deinen eigenen willst /cc ${event.user?.asMention}"
            )
                .delay(Duration.ofSeconds(10))
                .flatMap(Message::delete)
                .queue()
        }
        transaction {
            val foundStarrer = Starrer.find { Starrers.authorId eq event.userIdLong }.firstOrNull()
            if (remove) {
                foundStarrer?.delete()
            } else {
                if (foundStarrer != null) {
                    event.reaction.removeReaction(event.user!!).queue()
                    return@transaction null
                }
                Starrer.new {
                    authorId = event.userIdLong
                    starredMessage = event.messageIdLong
                    emojis = 1
                }
            }
        } ?: return
        val starrers = transaction { Starrer.find { Starrers.starredMessage eq potentialEntryMessage.idLong }.toList() }

        println("limmiting")
        if (starrers.size >= limit) {
            println("passed limit")
            val channel = event.guild.getTextChannelById(starBoardChannelId) ?: return
            val trackingMessage: RestAction<Message?> = when {
                foundEntry != null -> channel.retrieveMessageById(foundEntry.botMessageId)
                else -> channel.sendMessage(
                    Embeds.loading(
                        "Ein neuer Stern wird geboren!",
                        "Bald ist ein neuer Stern am Himmel."
                    )
                )
            }

            val message = trackingMessage.await() ?: return

            if (foundEntry == null) {
                transaction {
                    StarboardEntry.new {
                        authorId = potentialEntryMessage.author.idLong
                        botMessageId = message.idLong
                        channelId = potentialEntryMessage.channel.idLong
                        messageId = potentialEntryMessage.idLong
                    }
                }
            }
            message.editMessage(buildMessage(potentialEntryMessage, starrers.size)).queue()
        } else if (foundEntry != null) {
            deleteStarboardEntry(event.messageIdLong, event.guild)
        }
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
        guild.getTextChannelById(starBoardChannelId)?.retrieveMessageById(entry.botMessageId)
            ?.flatMap { it.delete() }?.queue(null, ignore(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.UNKNOWN_CHANNEL))
        transaction {
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
