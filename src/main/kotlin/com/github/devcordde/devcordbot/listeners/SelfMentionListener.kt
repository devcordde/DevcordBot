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

package com.github.devcordde.devcordbot.listeners

import com.github.devcordde.devcordbot.constants.Constants
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.constants.Emotes
import com.github.devcordde.devcordbot.core.DevCordBot
import com.github.devcordde.devcordbot.dsl.editMessage
import com.github.devcordde.devcordbot.dsl.sendMessage
import com.github.devcordde.devcordbot.util.asMention
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent

/**
 * Listens for the bot being mentioned.
 */
class SelfMentionListener(private val bot: DevCordBot) {

    /**
     * Listens for new Guild messages.
     */
    @SubscribeEvent
    fun onMessageReceive(event: GuildMessageReceivedEvent) {
        if (event.author.isBot) return
        if (event.guild.selfMember.asMention().matchEntire(event.message.contentRaw) != null) {
            sendInfo(event.channel, event.jda.users.size, bot)
        }
    }

    companion object {
        /**
         * Send Bot-Information to given channel
         */
        fun sendInfo(textChannel: MessageChannel, userCount: Int, devCordBot: DevCordBot) {
            val contributors = devCordBot.github.retrieveContributors().thenApply { contributors ->
                contributors.joinToString(", ") {
                    "[${it.name}](${it.url})"
                }
            }

            textChannel.sendMessage(Embeds.info("DevCordBot") {
                addField("Programmiersprache", "[Kotlin](https://kotlinlang.org)", inline = true)
                addField("Entwickler", Emotes.LOADING, inline = true)
                addField(
                    "Source",
                    "[github.com/devcordde/Devcordbot](https://github.com/devcordde/Devcordbot)",
                    inline = true
                )
                addField("User", userCount.toString(), inline = true)
                addField("Prefix", "`${Constants.firstPrefix}`", inline = true)
            }).flatMap {
                it.editMessage(
                    Embeds.info("DevCordBot") {
                        addField("Programmiersprache", "[Kotlin](https://kotlinlang.org)", inline = true)
                        addField("Entwickler", contributors.join(), inline = true)
                        addField(
                            "Source",
                            "[github.com/devcordde/Devcordbot](https://github.com/devcordde/Devcordbot)",
                            inline = true
                        )
                        addField("User", userCount.toString(), inline = true)
                        addField("Prefix", "`${Constants.firstPrefix}`", inline = true)
                    }
                )
            }.queue()
        }
    }
}
