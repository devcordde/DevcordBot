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

package com.github.devcordde.devcordbot

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.github.devcordde.devcordbot.config.Config
import com.github.devcordde.devcordbot.constants.Constants
import dev.kord.core.Kord
import io.sentry.Sentry
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import org.slf4j.LoggerFactory
import com.github.devcordde.devcordbot.core.DevCordBotImpl as DevCordBot
import org.slf4j.event.Level as SLF4JLevel

/**
 * DevCordBot entry point.
 */
suspend fun main(args: Array<String>) {
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

    val config = Config()

    if (debugMode) {
        Sentry.init("") // Initilizing sentry with null does mute sentry
    } else {
        Sentry.init("${config.sentry.dsn}?stacktrace.app.packages=com.github.devcordde.devcordbot")
    }

    Constants.hastebinUrl = config.hasteHost

    val kord = Kord(config.discord.token)
    val guild = kord.getGuild(config.discord.guildId) ?: error("Could not find dev guild")

    DevCordBot(config, debugMode, kord, guild)
}
