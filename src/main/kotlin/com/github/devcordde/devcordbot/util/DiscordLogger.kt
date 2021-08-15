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

package com.github.devcordde.devcordbot.util

import com.github.devcordde.devcordbot.config.Config
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.core.DevCordBot
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.MessageChannel

/**
 * Utility to log discord events.
 */
class DiscordLogger(private val bot: DevCordBot) {

    /**
     * Logs an event for [user] into [Config.Discord.logChannel].
     *
     * Example description: `Schlaubi#0001 (416902379598774273)test123456789 -> https://haste.schlaubi.me/anizeluvav`
     *
     * @param title the title of the event
     * @param description the description of the even
     */
    suspend fun logEvent(title: String, description: String, user: User? = null) {
        val channel =
            bot.kord.getChannelOf<MessageChannel>(bot.config.discord.logChannel) ?: error("Log channel id is invalid")

        val prefix = user?.let { user.tag + " (${user.id.asString}) " } ?: ""
        channel.createMessage(Embeds.info(title, prefix + description))
    }
}
