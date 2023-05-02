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

package com.github.devcordde.devcordbot.core

import com.github.devcordde.devcordbot.command.CommandClient
import com.github.devcordde.devcordbot.config.Config
import com.github.devcordde.devcordbot.util.DiscordLogger
import com.github.devcordde.devcordbot.util.GithubUtil
import com.github.devcordde.devcordbot.util.Googler
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import dev.kord.core.event.gateway.ReadyEvent
import io.ktor.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

/**
 * Main class of the bot
 */
interface DevCordBot : CoroutineScope {
    /**
     * The [CommandClient] used for command parsing.
     */
    val commandClient: CommandClient

    /**
     * The [Kord] instance.
     */
    val kord: Kord

    /**
     * The [GameAnimator] instance.
     */
    val gameAnimator: GameAnimator

    /**
     * Whether the bot received the [ReadyEvent] or not.
     */
    val isInitialized: Boolean

    /**
     * Http client used for JDA and the bot.
     */
    val httpClient: HttpClient

    /**
     * Whether the bot is in debug mode or not.
     */
    val debugMode: Boolean

    /**
     * The guild that the bot is operating on.
     */
    val guild: Guild

    /**
     * See [GithubUtil].
     */
    val github: GithubUtil

    /**
     * See [Googler].
     */
    val googler: Googler

    /**
     * The [Json] instance used for serialization.
     */
    val json: Json

    /**
     * The bots central configuration.
     * @see Config
     */
    val config: Config

    val discordLogger: DiscordLogger
}
