/*
 * Copyright 2020 Daniel Scherf & Michael Rittmeister
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

package com.github.seliba.devcordbot.core

import com.github.seliba.devcordbot.util.DefaultThreadFactory
import kotlinx.coroutines.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity
import java.io.Closeable
import java.util.concurrent.TimeUnit

/**
 * Animates the bot's activity status.
 */
@Suppress("EXPERIMENTAL_API_USAGE")
class GameAnimator(private val jda: JDA, private val games: List<AnimatedGame>) : Closeable {

    private lateinit var job: Job
    private val executor = DefaultThreadFactory.newSingleThreadExecutor("GameAnimator").asCoroutineDispatcher()

    /**
     * Starts the game animation.
     */
    fun start() {
        job = GlobalScope.launch(executor) {
            animate()
            delay(TimeUnit.SECONDS.toMillis(30))
        }
    }

    /**
     * Stops the animation.
     */
    fun stop() {
        job.cancel()
        executor.close()
    }

    private fun animate() {
        jda.presence.activity = games.random().animate(jda)
    }

    /**
     * Closes the resources used by the [GameAnimator].
     */
    override fun close() {
        stop()
        executor.close()
    }

    /**
     * Represents an animated game.
     * @property content The games content
     * @property type The activity type this game should be displayed as
     */
    data class AnimatedGame(val content: String, val type: Activity.ActivityType = Activity.ActivityType.DEFAULT) {
        /**
         * Replaces possible placeholders.
         */
        fun animate(jda: JDA): Activity = Activity.of(type, content.replace("%users%", jda.users.size.toString()))
    }

}
