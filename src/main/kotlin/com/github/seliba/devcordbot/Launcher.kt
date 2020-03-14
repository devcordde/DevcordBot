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

import com.github.seliba.devcordbot.core.GameAnimator
import io.github.cdimascio.dotenv.dotenv
import io.sentry.Sentry
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlin.system.exitProcess
import mu.KotlinLogging
import org.slf4j.event.Level as SLF4JLevel
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import net.dv8tion.jda.api.entities.Activity
import org.slf4j.LoggerFactory
import com.github.seliba.devcordbot.core.DevCordBotImpl as DevCordBot

private val logger = KotlinLogging.logger {}

/**
 * DevCordBot entry point.
 */
fun main(args: Array<String>) {
    val cliParser = ArgParser("devcordbot")
    val debugMode by cliParser.option(
        ArgType.Boolean,
        shortName = "d",
        fullName = "debug",
        description = "Disables HastebinErrorHandler and Sentry"
    ).default(false)
    val logLevelRaw by cliParser.option(
        ArgType.Choice(SLF4JLevel.values().map { it.toString() }),
        shortName = "ll",
        fullName = "log-level",
        description = "Sets the Logging level of the bot"
    ).default("INFO")
    cliParser.parse(args)

    val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    rootLogger.level = Level.valueOf(logLevelRaw)

    val env = dotenv()
    if (debugMode) {
        Sentry.init() // Initilizing sentry with null does mute sentry
    } else {
        env["SENTRY_DSN"]?.let {
            Sentry.init("$it?stacktrace.app.packages=com.github.seliba.devcordbot")
        }
    }

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
    DevCordBot(token, games, env, debugMode)
}
