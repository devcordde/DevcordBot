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
import com.github.devcordde.devcordbot.command.impl.CommandClientImpl
import com.github.devcordde.devcordbot.command.impl.RolePermissionHandler
import com.github.devcordde.devcordbot.commands.`fun`.SourceCommand
import com.github.devcordde.devcordbot.commands.general.*
import com.github.devcordde.devcordbot.commands.general.jdoodle.EvalCommand
import com.github.devcordde.devcordbot.commands.moderation.BlacklistCommand
import com.github.devcordde.devcordbot.commands.owners.CleanupCommand
import com.github.devcordde.devcordbot.commands.owners.RedeployCommand
import com.github.devcordde.devcordbot.constants.Constants
import com.github.devcordde.devcordbot.database.TagAliases
import com.github.devcordde.devcordbot.database.Tags
import com.github.devcordde.devcordbot.database.Users
import com.github.devcordde.devcordbot.util.GithubUtil
import com.github.devcordde.devcordbot.util.Googler
import com.zaxxer.hikari.HikariDataSource
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import dev.kord.core.event.gateway.DisconnectEvent
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.gateway.ResumedEvent
import dev.kord.core.on
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.coroutines.CoroutineContext
import com.github.devcordde.devcordbot.commands.owners.EvalCommand as OwnerEvalCommand

/**
 * General class to manage the Discord bot.
 */
internal class DevCordBotImpl(
    games: List<GameAnimator.AnimatedGame>,
    env: Dotenv,
    override val debugMode: Boolean,
    override val kord: Kord,
    override val guild: Guild
) : DevCordBot {

    private val logger = KotlinLogging.logger { }
    private lateinit var dataSource: HikariDataSource

    private val modRoleId = env["MOD_ROLE"]!!.toLong()
    private val adminRoleId = env["ADMIN_ROLE"]!!.toLong()
    private val botOwners = env["BOT_OWNERS"]!!.split(',').map { it.toLong() }

    override val commandClient: CommandClient =
        CommandClientImpl(this, Constants.prefix, RolePermissionHandler(emptyList()))
    override val json: Json = Json {
        ignoreUnknownKeys = true
    }
    override val httpClient: HttpClient = HttpClient(OkHttp) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(json)
        }
    }
    override val github: GithubUtil = GithubUtil(httpClient)
    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()

    override val googler: Googler = Googler(env["CSE_KEY"]!!, env["CSE_ID"]!!)

    override val gameAnimator = GameAnimator(this, games)

    /**
     * Whether the bot received the [ReadyEvent] or not.
     */
    override var isInitialized: Boolean = false
        private set

    init {
        Runtime.getRuntime().addShutdownHook(Thread(this::shutdown))
        logger.info { "Establishing connection to the database …" }
        connectToDatabase(env)

        logger.info { "Registering commands …" }
        registerCommands(env)
        kord.listeners(env)
    }

    private fun Kord.listeners(env: Dotenv) {
        whenReady()
        whenDisconnected()
        whenResumed()
    }

    /**
     * Fired when the Discord bot has started successfully.
     */
    private fun Kord.whenReady() = on<ReadyEvent> {
        logger.info { "Received Ready event initializing bot internals …" }
        isInitialized = true
        kord.editPresence {
            status = PresenceStatus.Online
        }
        gameAnimator.start()

        (commandClient as CommandClientImpl).updateCommands()
    }

    /**
     * Fired when the Discord connection gets interrupted
     */
    private fun Kord.whenDisconnected() = on<DisconnectEvent> {
        logger.warn { "Bot got disconnected (code: $this) disabling Discord specific internals" }
        isInitialized = false
        gameAnimator.stop()
    }

    /**
     * Fired when the bot can resume its previous connections when reconnecting.
     */
    private fun Kord.whenResumed() = on<ResumedEvent> { reinitialize() }

    private fun reinitialize() {
        logger.info {
            //language=TEXT
            "Bot reconnected reinitializing internals …"
        }
        isInitialized = true
        gameAnimator.start()
    }

    private fun connectToDatabase(env: Dotenv) {
        dataSource = HikariDataSource().apply {
            jdbcUrl = "jdbc:postgresql://${env["DATABASE_HOST"]}/${env["DATABASE"]}"
            username = env["DATABASE_USERNAME"]
            password = env["DATABASE_PASSWORD"]
        }
        Database.connect(dataSource)
        transaction {
            SchemaUtils.createMissingTablesAndColumns(Users, Tags, TagAliases)
            //language=PostgreSQL
            exec("SELECT * FROM pg_extension WHERE extname = 'pg_trgm'") { rs ->
                //language=text
                require(rs.next()) { "pg_tgrm extension must be available. See https://dba.stackexchange.com/a/165301" }
            }
        }
    }

    private fun shutdown() {
        gameAnimator.close()
        dataSource.close()
    }

    private fun registerCommands(env: Dotenv) {
        commandClient.registerCommands(
            HelpCommand(),
            TagCommand().apply {
                registerReadCommand(commandClient)
            },
            EvalCommand(),
            OwnerEvalCommand(),
            SourceCommand(),
            RankCommand(),
            RanksCommand(),
            BlacklistCommand(),
            InfoCommand(),
            CleanupCommand(),
            GoogleCommand()
        )

        val redeployHost = env["REDEPLOY_HOST"]
        val redeployToken = env["REDEPLOY_TOKEN"]
        if (redeployHost != null && redeployToken != null && redeployHost.isNotBlank() && redeployToken.isNotBlank()) {
            commandClient.registerCommands(RedeployCommand(redeployHost, redeployToken))
        }
    }
}
