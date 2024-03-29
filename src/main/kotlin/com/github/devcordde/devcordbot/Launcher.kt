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

package com.github.devcordde.devcordbot

import ch.qos.logback.classic.ClassicConstants
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.github.devcordde.devcordbot.config.Config
import com.github.devcordde.devcordbot.constants.Constants
import dev.kord.core.Kord
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.sentry.Sentry
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.pathString
import com.github.devcordde.devcordbot.core.DevCordBotImpl as DevCordBot
import org.slf4j.event.Level as SLF4JLevel

private val logger by lazy { KotlinLogging.logger {} }

/**
 * DevCordBot entry point.
 */
suspend fun main(args: Array<String>) {
    val config = Config()
    initializeLogger(config)
    val cliParser = ArgParser("devcordbot")
    val debugMode by cliParser.option(
        ArgType.Boolean,
        shortName = "d",
        fullName = "debug",
        description = "Disables HastebinErrorHandler and Sentry"
    ).default(false)
    val logLevelRaw by cliParser.option(
        ArgType.Choice(SLF4JLevel.values().toList(), { SLF4JLevel.valueOf(it) }),
        shortName = "ll",
        fullName = "log-level",
        description = "Sets the Logging level of the bot"
    ).default(SLF4JLevel.INFO)
    cliParser.parse(args)

    if (debugMode) {
        Sentry.init("") // Initilizing sentry with empty dsn does mute sentry
    } else {
        Sentry.init { options ->
            options.apply {
                dsn = "${config.sentry.dsn}?stacktrace.app.packages=com.github.devcordde.devcordbot"
                sampleRate = 1.0
            }
        }
    }

    val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    rootLogger.level = Level.toLevel(logLevelRaw.toInt())

    Constants.hastebinUrl = config.hasteHost

    val kord = Kord(config.discord.token) {
        httpClient = HttpClient(CIO)
    }

    DevCordBot(config, debugMode, kord).start()
}

private fun initializeLogger(config: Config) {
    val customConfig = Path(config.loggerConfig)
    if (!customConfig.exists()) {
        logger.warn { "Could not find ${customConfig.pathString}. Discord logging will be disabled" }
        return
    }

    System.setProperty(ClassicConstants.CONFIG_FILE_PROPERTY, customConfig.absolutePathString())
}
