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

package com.github.devcordde.devcordbot.listeners

import com.github.devcordde.devcordbot.config.Config
import com.github.devcordde.devcordbot.core.DevCordBot
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.on
import dev.kord.rest.Image
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import java.time.Instant

/**
 * Sends an event to the Devmarkt bot on emote action.
 */
class DevmarktRequestUpdater(
    private val bot: DevCordBot,
) {

    private val config: Config.Devmarkt get() = bot.config.devmarkt

    /**
     * Registers the necessary listeners.
     */
    fun Kord.registerListeners() {
        onMessageReceived()
        onReactionInDevmarktRequestChannel()
        onReceiveDenyMessage()
        onReactonOnReasonMessage()
    }

    private fun isNewEntryMessage(message: Message): Boolean {
        val embeds = message.embeds
        return (embeds.isNotEmpty() && "Neue Devmarkt-Anfrage" == embeds[0].title)
    }

    private fun isNewReasonMessage(message: Message): Boolean {
        val embeds = message.embeds
        return (embeds.isNotEmpty() && "Begründung" == embeds[0].title)
    }

    private fun getFieldValue(message: Message, name: String): String? {
        return message.embeds[0].fields
            .firstOrNull { field -> name == field.name }?.value
    }

    /**
     * Sends the event on an incoming reaction.
     */
    private fun Kord.onMessageReceived() = on<MessageCreateEvent> {
        if (message.channelId != config.requestChannel
            || message.author?.id != kord.selfId
            || !isNewEntryMessage(message)
        ) {
            return@on
        }

        val check = getGuild()?.getEmoji(config.checkEmote) ?: return@on
        message.addReaction(check)
    }

    /**
     * Sends the event on an incoming reaction.
     */
    private fun Kord.onReactionInDevmarktRequestChannel() = on<ReactionAddEvent> {
        if (message.channelId != config.requestChannel
            || userId == kord.selfId
            || (emoji as? ReactionEmoji.Custom)?.id != config.checkEmote
        ) {
            return@on
        }

        val realMessage = message.asMessage()
        if (!isNewEntryMessage(realMessage)) {
            return@on
        }

        val requestId = getFieldValue(realMessage, "Request-ID") ?: return@on

        process(userId, requestId, action = "accept")
    }

    /**
     * Reacts if a message is received
     */
    private fun Kord.onReceiveDenyMessage() = on<MessageCreateEvent> {
        if (message.channelId != config.requestChannel) {
            return@on
        }

        val guild = getGuild() ?: return@on

        val check = guild.getEmoji(config.checkEmote)
        val block = guild.getEmoji(config.blockEmote)
        val message = message
        val referencedMessage = message.referencedMessage ?: return@on

        if (!isNewEntryMessage(referencedMessage) || !message.content.startsWith("deny: ")) {
            return@on
        }

        val reason = message.content.drop(6)
        val user = message.author ?: return@on

        val requestId = getFieldValue(referencedMessage, "Request-ID") ?: return@on
        val requestTitel = getFieldValue(referencedMessage, "Titel") ?: return@on
        val requestAuthor = referencedMessage.embeds[0].footer?.text ?: return@on
        val requestColor = referencedMessage.embeds[0].color ?: return@on

        val messageReason = message.channel.createEmbed {
            title = "Begründung"
            field {
                name = "Titel"
                value = "`$requestTitel`"
                inline = true
            }
            field {
                name = "Autor"
                value = "`$requestAuthor`"
                inline = true
            }
            field {
                name = "Begründung"
                value = "`$reason`"
                inline = false
            }
            field {
                name = "Request-ID"
                value = requestId
                inline = true
            }

            footer {
                text = user.tag
                icon = user.avatar.run { if (isCustom) getUrl(Image.Size.Size64) else defaultUrl }
            }

            color = requestColor
            timestamp = Instant.now()
        }

        messageReason.addReaction(check)
        messageReason.addReaction(block)
        message.delete()
    }

    /**
     * Sends the event on an incoming reaction.
     */
    private fun Kord.onReactonOnReasonMessage() = on<ReactionAddEvent> {
        if (message.channel.id != config.requestChannel
            || userId == kord.selfId
        ) {
            return@on
        }

        val realMessage = getMessageOrNull() ?: return@on

        if (!isNewReasonMessage(realMessage)) {
            return@on
        }

        val reactionEmoteId = (emoji as? ReactionEmoji.Custom)?.id ?: return@on
        val requestId = getFieldValue(realMessage, "Request-ID") ?: return@on

        if (reactionEmoteId == config.blockEmote) {
            message.delete()
        }
        if (reactionEmoteId != config.checkEmote) {
            return@on
        }

        val reason = getFieldValue(realMessage, "Begründung") ?: return@on


        message.delete()
        process(userId, requestId, reason, "decline")
    }

    private suspend fun process(
        userId: Snowflake,
        requestId: String,
        reason: String? = null,
        action: String
    ) {
        bot.httpClient.post<Unit>(config.baseUrl) {
            url {
                path("process.php")
            }

            formData {
                append("moderator_id", userId.asString)
                append("action", action)
                append("access_token", config.accessToken)
                append("req_id", requestId)
                if (reason != null) {
                    append("reason", reason)
                }
            }
        }
    }
}
