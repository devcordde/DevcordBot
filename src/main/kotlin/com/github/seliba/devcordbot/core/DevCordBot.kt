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

import com.github.seliba.devcordbot.command.CommandClient
import com.github.seliba.devcordbot.command.impl.CommandClientImpl
import com.github.seliba.devcordbot.commands.general.HelpCommand
import com.github.seliba.devcordbot.database.Users
import com.github.seliba.devcordbot.event.AnnotatedEventManger
import com.github.seliba.devcordbot.event.EventSubscriber
import com.github.seliba.devcordbot.listeners.DatabaseUpdater
import com.github.seliba.devcordbot.listeners.SelfMentionListener
import com.github.seliba.devcordbot.util.Constants
import com.zaxxer.hikari.HikariDataSource
import io.github.cdimascio.dotenv.Dotenv
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.DisconnectEvent
import net.dv8tion.jda.api.events.ReadyEvent
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * General class to manage the Discord bot.
 */
class DevCordBot(token: String, games: List<GameAnimator.AnimatedGame>, env: Dotenv) {

    private val logger = KotlinLogging.logger { }
    private lateinit var dataSource: HikariDataSource
    private var initializationStatus = false

    private val commandClient: CommandClient = CommandClientImpl(this, Constants.prefix)
    private val jda: JDA = JDABuilder(token)
        .setEventManager(AnnotatedEventManger())
        .setActivity(Activity.playing("Starting ..."))
        .setStatus(OnlineStatus.DO_NOT_DISTURB)
        .addEventListeners(this@DevCordBot, SelfMentionListener(), DatabaseUpdater(), commandClient)
        .build()

    private val gameAnimator = GameAnimator(jda, games)

    /**
     * Whether the bot received the [ReadyEvent] or not.
     */
    val isInitialized: Boolean
        get() = initializationStatus

    init {
        Runtime.getRuntime().addShutdownHook(Thread(this::shutdown))
        logger.info { "Establishing connection to the database …" }
        registerCommands()
        connectToDatabase(env)
    }

    /**
     * Fired when the Discord bot has started successfully.
     */
    @EventSubscriber
    fun whenReady(event: ReadyEvent) {
        logger.info { "Received Ready event initializing bot internals …" }
        initializationStatus = true
        event.jda.presence.setStatus(OnlineStatus.ONLINE)
        gameAnimator.start()
    }

    /**
     * Fired when the Discord connection geht's interrupted
     */
    @EventSubscriber
    fun whenDisconnected(event: DisconnectEvent) {
        logger.warn { "Bot got disconnected disabling Discord specific internals" }
        initializationStatus = false
        gameAnimator.stop()
    }

    private fun connectToDatabase(env: Dotenv) {
        dataSource = HikariDataSource()
        dataSource.jdbcUrl = "jdbc:postgresql://${env["DATABASE_HOST"]}/${env["DATABASE"]}"
        dataSource.username = env["DATABASE_USERNAME"]
        dataSource.password = env["DATABASE_PASSWORD"]
        Database.connect(dataSource)
        transaction {
            SchemaUtils.createMissingTablesAndColumns(Users)
        }
    }

    private fun shutdown() {
        gameAnimator.close()
        dataSource.close()
    }

    private fun registerCommands() {
        commandClient.registerCommands(
            HelpCommand()
        )
    }
}
