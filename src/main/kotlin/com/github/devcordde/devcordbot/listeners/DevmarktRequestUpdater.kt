package com.github.devcordde.devcordbot.listeners

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class DevmarktRequestUpdater(val requestChannel: String,val accessToken : String,val baseUrl : String,val emoteCheckId : String,val emoteBlockId : String) {

    @SubscribeEvent
    fun onMessageReceived(event: MessageReceivedEvent) {
        val channel = event.channel.id
        if(channel != requestChannel) {
            return
        }
        val id = event.member?.id ?: return
        val botid = event.jda.selfUser.id
        if(!id.contentEquals(botid)) {
            return
        }
        if(event.message.embeds.size == 0){
            return
        }
        val title = event.message.embeds[0].title ?: return
        val check = event.guild.getEmoteById(emoteCheckId) ?: return

        if (!title.contentEquals("Neue Devmarkt-Anfrage")) {
            return
        }
        print("breqq")
        event.message.addReaction(check).queue()
    }

    @SubscribeEvent
    fun onReactionInDevmarktRequestChannel(event: MessageReactionAddEvent) {
        val channel = event.channel.id
        if (channel != requestChannel) {
            return
        }
        val id = event.userId
        val botId = event.jda.selfUser.id
        if (id == botId) {
            return
        }
        val embed = event.retrieveMessage().complete().embeds[0] ?: return
        val title = embed.title ?: return
        if (!title.contentEquals("Neue Devmarkt-Anfrage")) {
            return
        }
        if (event.reactionEmote.id == emoteCheckId) {
            return
        }
        val requestId = embed.fields.stream()
            .filter { field -> "Request-ID" == field.name }
            .findAny()
            .orElse(null).value

        val formBody = FormBody.Builder()
            .add("moderator_id", id)
            .add("action", "accept")
            .add("access_token", accessToken)
            .add("req_id", requestId!!)
            .build()

        val request = Request.Builder()
            .url("$baseUrl/process.php")
            .post(formBody)
            .build()

        event.jda.httpClient.newCall(request).execute()

    }
}

