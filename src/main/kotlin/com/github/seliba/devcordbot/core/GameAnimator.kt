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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity
import java.util.concurrent.TimeUnit

/**
 * Animates the bots activity status.
 */
class GameAnimator(private val jda: JDA, private val games: List<AnimatedGame>) {

    @ObsoleteCoroutinesApi
    private val channel = ticker(TimeUnit.SECONDS.toMillis(30), 0)
    private val pool = DefaultThreadFactory.newSingleThreadExecutor("GameAnimator").asCoroutineDispatcher()

    /**
     * Starts the game animation.
     */
    @ObsoleteCoroutinesApi
    fun start() {
        GlobalScope.launch(pool) {
            for (unit in channel) {
                animate()
            }
        }
    }

    private fun animate() {
        jda.presence.activity = games.random().animate(jda)
    }

    /**
     * Represents an animated game.
     * @property content The games content
     * @property type The type of the activity this game should be displayed as
     */
    data class AnimatedGame(val content: String, val type: Activity.ActivityType = Activity.ActivityType.DEFAULT) {
        /**
         * Replaces possible placeholders.
         */
        fun animate(jda: JDA): Activity = Activity.of(type, content.replace("%users%", jda.users.size.toString()))
    }

}
