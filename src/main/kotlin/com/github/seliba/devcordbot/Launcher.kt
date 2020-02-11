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

package com.github.seliba.devcordbot

import com.github.seliba.devcordbot.core.DevCordBotImpl
import com.github.seliba.devcordbot.core.GameAnimator
import io.github.cdimascio.dotenv.dotenv
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Activity
import kotlin.system.exitProcess


private val logger = KotlinLogging.logger {}

/**
 * DevCordBot entry point.
 */
fun main() {
    val env = dotenv()
    val token = env["DISCORD_TOKEN"]
    var games = env["GAMES"]?.split(";")?.map {
        if (it.startsWith("!")) {
            GameAnimator.AnimatedGame(it, Activity.ActivityType.LISTENING)
        } else {
            GameAnimator.AnimatedGame(it, Activity.ActivityType.DEFAULT)
        }
    }

    if (token == null) {
        logger.error { "The Discord bot token must not be null" }
        exitProcess(1)
    }
    if (games == null) {
        logger.warn { "Games could not be found, returning to fallback status..." }
        games = listOf(GameAnimator.AnimatedGame("with errors"))
    }

    logger.info { "Launching DevCordBot..." }
    DevCordBotImpl(token, games, env)
}
