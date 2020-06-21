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

import com.github.devcordde.devcordbot.command.CommandClient
import com.github.devcordde.devcordbot.util.Punisher
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import okhttp3.OkHttpClient

/**
 * Main class of the bot
 */
interface DevCordBot {
    /**
     * The [CommandClient] used for command parsing.
     */
    val commandClient: CommandClient

    /**
     * The [JDA] instance.
     */
    val jda: JDA

    /**
     * The [GameAnimator] instance.
     */
    val gameAnimator: GameAnimator

    /**
     * Whether the bot received the [net.dv8tion.jda.api.events.ReadyEvent] or not.
     */
    val isInitialized: Boolean

    /**
     * Http client used for JDA and the bot.
     */
    val httpClient: OkHttpClient

    /**
     * Whether the bot is in debug mode or not.
     */
    val debugMode: Boolean

    /**
     * The starboard instance.
     */
    val starboard: Starboard

    /**
     * The guild that the bot is operating on.
     */
    val guild: Guild

    /**
     * The bots punisher, for punishing rude users.
     */
    val punisher: Punisher
}
