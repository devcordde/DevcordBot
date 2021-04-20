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

import dev.kord.common.entity.ActivityType
import dev.kord.core.Kord
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.Closeable
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

/**
 * Animates the bot's activity status.
 */
@Suppress("EXPERIMENTAL_API_USAGE")
class GameAnimator(private val bot: DevCordBot, private val games: List<AnimatedGame>) : Closeable {

    private lateinit var job: Job

    @OptIn(ExperimentalTime::class)
    private val ticker = ticker(30.seconds.toLongMilliseconds())

    /**
     * Starts the game animation.
     */
    fun start() {
        job = bot.launch {
            for (unit in ticker) {
                animate()
            }
        }
    }

    /**
     * Stops the animation.
     */
    fun stop() {
        if (::job.isInitialized) {
            job.cancel()
        }
    }

    private suspend fun animate() {
        games.random().animate(bot.kord)
    }

    /**
     * Closes the resources used by the [GameAnimator].
     */
    override fun close() {
        stop()
    }

    /**
     * Represents an animated game.
     * @property content The games content
     * @property type The activity type this game should be displayed as
     */
    data class AnimatedGame(val content: String, val type: ActivityType = ActivityType.Game) {
        /**
         * Replaces possible placeholders.
         */
        suspend fun animate(kord: Kord): Unit = kord.editPresence {
            val content = content.replace("%users%", kord.guilds.first().memberCount.toString())
            when (type) {
                ActivityType.Game -> playing(content)
                ActivityType.Streaming -> streaming(content, "")
                ActivityType.Listening -> listening(content)
                ActivityType.Watching -> watching(content)
                else -> error("Unsupported type: $type")
            }
        }
    }
}
