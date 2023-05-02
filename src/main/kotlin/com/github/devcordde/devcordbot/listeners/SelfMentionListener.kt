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
import com.github.devcordde.devcordbot.util.asMention
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * Listens for the bot being mentioned.
 */
class SelfMentionListener(private val bot: DevCordBot) {

    /**
     * Listens for new Guild messages.
     */
    fun Kord.onMessageReceive() {
        on<MessageCreateEvent> {
            if (message.author?.isBot == true) return@on
            val guild = getGuild() ?: return@on
            if (message.content.matches(guild.getMember(kord.selfId).asMention())) {
                bot.launch {
                    sendInfo(message.channel, bot)
                }
            }
        }
    }

    companion object {

        /**
         * Fetches the GitHub contributors to [devCordBot].
         */
        suspend fun fetchContributors(devCordBot: DevCordBot): String {
            val contributors = devCordBot.github.retrieveContributors()
            return contributors.joinToString(", ") {
                "[${it.name}](${it.htmlUrl})"
            }
        }

        /**
         * Creates the info embed about [bot].
         *
         * @param devs the developers field
         */
        fun makeEmbed(bot: DevCordBot, devs: String = Emotes.LOADING): EmbedBuilder = Embeds.info("DevCordBot") {
            field {
                name = "Programmiersprache"
                value = "[Kotlin](https://kotlinlang.org)"
                inline = true
            }
            field {
                name = "Entwickler"
                value = devs
                inline = true
            }
            field {
                name = "Sourcecode"
                value = "[devcordde/DevcordBot](https://github.com/devcordde/DevcordBot)"
                inline = true
            }
            field {
                name = "Nutzer"
                value = bot.guild.memberCount.toString()
                inline = true
            }
            field {
                name = "Präfix"
                value = "`${Constants.firstPrefix}`"
                inline = true
            }
        }

        /**
         * Send Bot-Information to given channel
         */
        suspend fun sendInfo(textChannel: MessageChannelBehavior, devCordBot: DevCordBot) {
            val contributors = devCordBot.async { fetchContributors(devCordBot) }

            val message = textChannel.createMessage {
                embeds.add(makeEmbed(devCordBot))
            }
            val contributorList = contributors.await()

            message.edit {
                embeds = mutableListOf(makeEmbed(devCordBot, contributorList))
            }
        }
    }
}
