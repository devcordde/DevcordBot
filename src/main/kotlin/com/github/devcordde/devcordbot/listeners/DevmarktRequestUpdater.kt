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

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import okhttp3.FormBody
import okhttp3.Request
import java.awt.Color
import java.time.OffsetDateTime
import javax.annotation.Nullable

/**
 * Sends an event to the Devmarkt bot on emote action.
 */
class DevmarktRequestUpdater(
    private val modChannel: String,
    private val accessToken: String,
    private val baseUrl: String,
    private val emoteCheckId: String,
    @Suppress("unused") private val emoteBlockId: String
) {

    private fun isNewEntryMessage(message: Message): Boolean {
        val embeds = message.embeds
        return (embeds.isNotEmpty() && "Neue Devmarkt-Anfrage" == embeds[0].title)
    }

    private fun isNewReasonMessage(message: Message): Boolean {
        val embeds = message.embeds
        return (embeds.isNotEmpty() && "Begründung" == embeds[0].title)
    }

    private fun getFieldValue(message: Message, name: String): @Nullable String? {
        return message.embeds[0].fields.stream()
            .filter { field -> name == field.name }
            .findAny()
            .orElse(null).value
    }

    /**
     * Sends the event on an incoming reaction.
     */
    @SubscribeEvent
    fun onMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.channel.id != modChannel
            || event.message.member?.idLong != event.jda.selfUser.idLong
            || !isNewEntryMessage(event.message)
        ) {
            return
        }

        val check = event.guild.getEmoteById(emoteCheckId) ?: return
        event.message.addReaction(check).queue()
    }

    /**
     * Sends the event on an incoming reaction.
     */
    @SubscribeEvent
    fun onReactionInDevmarktRequestChannel(event: MessageReactionAddEvent) {
        if (event.channel.id != modChannel
            || event.userId == event.jda.selfUser.id
            || event.reactionEmote.id != emoteCheckId
        ) {
            return
        }

        val message = event.retrieveMessage().complete()
        if (!isNewEntryMessage(message)) {
            return
        }

        val requestId = getFieldValue(message, "Request-ID") ?: return

        val formBody = FormBody.Builder()
            .add("moderator_id", event.userId)
            .add("action", "accept")
            .add("access_token", accessToken)
            .add("req_id", requestId)
            .build()

        val request = Request.Builder()
            .url("$baseUrl/process.php")
            .post(formBody)
            .build()

        event.jda.httpClient.newCall(request).execute()
    }

    /**
     * Reacts if a message is received
     */
    @SubscribeEvent
    fun onReceiveDenyMessage(event: GuildMessageReceivedEvent) {
        if (event.channel.id != modChannel
        ) {
            return
        }

        val check = event.guild.getEmoteById(emoteCheckId) ?: return
        val block = event.guild.getEmoteById(emoteBlockId) ?: return
        val message = event.message
        val referencedMessage = event.message.referencedMessage ?: return

        if(!isNewEntryMessage(referencedMessage)) {
            return
        }

        if (!message.contentRaw.startsWith("deny:")) {
            return
        }

        val reason = event.message.contentRaw
        val builder: EmbedBuilder = EmbedBuilder()
        val user = event.member?.user ?: return

        val requestId = getFieldValue(referencedMessage, "Request-ID") ?: return
        val requestTitel = getFieldValue(referencedMessage, "Titel") ?: return
        val requestAuthor = referencedMessage.embeds[0].footer?.text ?: return
        val requestColor = referencedMessage.embeds[0].color ?: return

        builder.setTitle("Begründung")
        builder
            .addField("Titel", "`$requestTitel`", true)
            .addField("Author", "`$requestAuthor`", true)
            .addField("Begründung", "`" + reason.replace("deny:", "") + "`", false)
            .addField("Request-ID", requestId, true)
            .setFooter(user.name + "#" + user.discriminator, user.effectiveAvatarUrl)
            .setColor(requestColor)
            .setTimestamp(OffsetDateTime.now())

        val messageReason = event.channel.sendMessage(builder.build()).complete()
        messageReason.addReaction(check).queue()
        messageReason.addReaction(block).queue()
        event.message.delete().queue()

    }

    /**
     * Sends the event on an incoming reaction.
     */
    @SubscribeEvent
    fun onReactionOnReasonMessage(event: MessageReactionAddEvent) {
        if (event.channel.id != modChannel
            || event.userId == event.jda.selfUser.id
        ) {
            return
        }

        val message = event.retrieveMessage().complete() ?: return

        if (!isNewReasonMessage(message)) {
            return
        }

        val reactionEmoteId = event.reactionEmote.id
        val requestId = getFieldValue(message, "Request-ID") ?: return

        if (reactionEmoteId == emoteCheckId) {

            val reason = getFieldValue(message, "Begründung") ?: return

            val formBody = FormBody.Builder()
                .add("moderator_id", event.userId)
                .add("action", "decline")
                .add("access_token", accessToken)
                .add("req_id", requestId)
                .add("reason", reason)
                .build()

            val request = Request.Builder()
                .url("$baseUrl/process.php")
                .post(formBody)
                .build()

            message.delete().queue()
            event.jda.httpClient.newCall(request).execute()

        } else if (reactionEmoteId == emoteBlockId) {
            message.delete().queue()
        }

    }
}
