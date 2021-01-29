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
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import okhttp3.FormBody
import okhttp3.Request
import java.awt.Color
import java.time.OffsetDateTime
import java.time.temporal.TemporalAccessor

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

    private var denyRequestQueue: HashMap<Long, String> = HashMap<Long, String>()

    private fun isNewEntryMessage(message: Message): Boolean {
        val embeds = message.embeds
        return (embeds.isNotEmpty() && "Neue Devmarkt-Anfrage" == embeds[0].title)
    }

    /**
     * Reacts if a message is received
     */
    @SubscribeEvent
    fun onMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.channel.id != modChannel
        ) {
            return
        }

        if (event.message.member?.idLong == event.jda.selfUser.idLong
            && isNewEntryMessage(event.message)
        ) {

            val check = event.guild.getEmoteById(emoteCheckId) ?: return
            val block = event.guild.getEmoteById(emoteBlockId) ?: return
            event.message.addReaction(check).queue()
            event.message.addReaction(block).queue()

        }

        val member = event.member

        val id = event.member?.idLong ?: return
        println("User-IDddd in Hashmap" + denyRequestQueue[id])
        if (denyRequestQueue.contains(id)) {

            denyRequestQueue.remove(event.author.idLong)
            val reason = event.message.contentRaw
            val builder: EmbedBuilder = EmbedBuilder()

            builder.setTitle("Begründung")
            builder.addField("Begründung", reason, true)
            builder.addField("Request-ID", denyRequestQueue[event.member?.idLong], true)
            builder.setColor(Color.RED)
            builder.setTimestamp(OffsetDateTime.now())

            event.channel.sendMessage(builder.build()).queue()

        }
    }

    /**
     * Sends the event on an incoming reaction.
     */
    @SubscribeEvent
    fun onReactionInDevmarktRequestChannel(event: MessageReactionAddEvent) {
        if (event.channel.id != modChannel
            || event.userId == event.jda.selfUser.id
        ) {
            return
        }

        val message = event.retrieveMessage().complete()

        if (!isNewEntryMessage(message)) {
            return
        }

        val reactionEmoteId = event.reactionEmote.id

        val requestId = message.embeds[0].fields.stream()
            .filter { field -> "Request-ID" == field.name }
            .findAny()
            .orElse(null).value ?: return

        if (reactionEmoteId == emoteCheckId) {

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

        } else if (reactionEmoteId == emoteBlockId) {
            val id = event.member?.idLong ?: return
            denyRequestQueue[id] = requestId
            println("User-ID in Hashmap" + denyRequestQueue[id])
        }

    }
}