package com.github.devcordde.devcordbot.listeners

import com.github.devcordde.devcordbot.command.impl.RolePermissionHandler
import io.github.cdimascio.dotenv.dotenv
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class DevmarktRequestUpdater() {

    private val client = OkHttpClient()

    @SubscribeEvent
    fun onMessageReceived(event: MessageReceivedEvent) {
        val id = event.member?.id
        val botid = event.jda.selfUser.id
        val channel = event.channel.id
        val env = dotenv()
        var requestChannel = env["DEVMARKT_REQUEST_CHANNEL"]

        if (id!!.contentEquals(botid)) {
            if (requestChannel?.let { channel.contentEquals(it) }!!) {
                val embed = event.message.embeds[0]
                if (embed != null) {
                    if (embed.title?.contentEquals("Neue Devmarkt-Anfrage")!!) {
                        val check = event.guild.getEmoteById(739219792476504134)
                        if (check != null) {
                            event.message.addReaction(check).queue()
                            println("Emote hinzufügen???")
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onReactionInDevmarktRequestChannel(event: MessageReactionAddEvent) {
        val id = event.userId;
        val channel = event.channel.id
        val botid = event.jda.selfUser.id
        val env = dotenv()
        val requestChannel = env["DEVMARKT_REQUEST_CHANNEL"]

        if (id != botid) {

            if (requestChannel?.let { channel.contentEquals(it) }!!) {
                val embed = event.retrieveMessage().complete().embeds[0]
                if (embed.title?.contentEquals("Neue Devmarkt-Anfrage")!!) {

                    val req_id = embed.fields.stream()
                        .filter { field -> "Request-ID".equals(field.name) }
                        .findAny()
                        .orElse(null).value

                    var access_token = env["BOT_ACCESS_TOKEN"]

                    if (event.reactionEmote.name == "check") {

                        val formBody = FormBody.Builder()
                            .add("moderator_id", id)
                            .add("action", "accept")
                            .add("access_token", access_token!!)
                            .add("req_id", req_id!!)
                            .build()

                        val request = Request.Builder()
                            .url(env["DEVMARKT_BASE_URL"] + "/process.php")
                            .post(formBody)
                            .build()

                        client.newCall(request).execute()

                    }
                }
            } else println("iwas ist null")
        } else println("botid")

    }
}
