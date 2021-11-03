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
import com.github.devcordde.devcordbot.command.impl.CommandClientImpl
import com.github.devcordde.devcordbot.command.impl.RolePermissionHandler
import com.github.devcordde.devcordbot.commands.`fun`.SourceCommand
import com.github.devcordde.devcordbot.commands.general.*
import com.github.devcordde.devcordbot.commands.general.jdoodle.EvalCommand
import com.github.devcordde.devcordbot.commands.moderation.BlacklistCommand
import com.github.devcordde.devcordbot.commands.owners.CleanupCommand
import com.github.devcordde.devcordbot.commands.owners.RedeployCommand
import com.github.devcordde.devcordbot.config.Config
import com.github.devcordde.devcordbot.constants.Constants
import com.github.devcordde.devcordbot.constants.Emotes
import com.github.devcordde.devcordbot.core.autohelp.DevCordTagSupplier
import com.github.devcordde.devcordbot.database.TagAliases
import com.github.devcordde.devcordbot.database.Tags
import com.github.devcordde.devcordbot.database.Users
import com.github.devcordde.devcordbot.listeners.DatabaseUpdater
import com.github.devcordde.devcordbot.listeners.DevmarktRequestUpdater
import com.github.devcordde.devcordbot.listeners.SelfMentionListener
import com.github.devcordde.devcordbot.listeners.addNameWatcher
import com.github.devcordde.devcordbot.util.DiscordLogger
import com.github.devcordde.devcordbot.util.GithubUtil
import com.github.devcordde.devcordbot.util.Googler
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import dev.kord.core.event.gateway.DisconnectEvent
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.gateway.ResumedEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.schlaubi.forp.analyze.client.RemoteStackTraceAnalyzer
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import me.schlaubi.autohelp.AutoHelp
import me.schlaubi.autohelp.autoHelp
import me.schlaubi.autohelp.kord.kordContext
import me.schlaubi.autohelp.kord.kordEventSource
import me.schlaubi.autohelp.kord.useKordMessageRenderer
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.coroutines.CoroutineContext
import kotlin.time.ExperimentalTime
import com.github.devcordde.devcordbot.commands.owners.EvalCommand as BotOwnerEvalCommand

/**
 * General class to manage the Discord bot.
 */
internal class DevCordBotImpl(
    override val config: Config,
    override val debugMode: Boolean,
    override val kord: Kord
) : DevCordBot {

    private val logger = KotlinLogging.logger { }
    override val discordLogger: DiscordLogger = DiscordLogger()
    private lateinit var dataSource: HikariDataSource

    override val commandClient: CommandClient =
        CommandClientImpl(this, Constants.prefix, RolePermissionHandler(config.permissions.botOwners))
    override val json: Json = Json {
        ignoreUnknownKeys = true
    }
    override val httpClient: HttpClient = HttpClient(OkHttp) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(json)
        }
        install(HttpTimeout)
    }
    override val github: GithubUtil = GithubUtil(httpClient)
    override val coroutineContext: CoroutineContext = Dispatchers.IO + SupervisorJob()

    override val googler: Googler = Googler(this)

    override val gameAnimator = GameAnimator(this)

    override val guild: Guild
        get() = runBlocking { kord.getGuild(config.discord.guildId) } ?: error("Could not get Bot guild")

    override val autoHelp: AutoHelp = autoHelp {
        tagSupplier = DevCordTagSupplier
        loadingEmote = Emotes.LOADING

        useKordMessageRenderer(kord)
        htmlRenderer { de.nycode.bankobot.docdex.htmlRenderer.convert(this) }

        analyzer = RemoteStackTraceAnalyzer {
            httpEngine = CIO
            serverUrl = config.autoHelp.host
            authKey = config.autoHelp.key
            dispatcher = coroutineContext
        }

        dispatcher = coroutineContext

        kordContext {
            kordEventSource(kord)
            filter {
                it.kordMessage.author?.isBot != true && it.channelId in config.autoHelp.channels
            }
        }
    }

    /**
     * Whether the bot received the [ReadyEvent] or not.
     */
    override var isInitialized: Boolean = false
        private set

    init {
        Runtime.getRuntime().addShutdownHook(Thread(this::shutdown))
        kord.listeners()
    }

    @OptIn(PrivilegedIntent::class)
    suspend fun start() {
        logger.info { "Establishing connection to the database..." }
        connectToDatabase()
        logger.info { "Registering commands..." }
        registerCommands()

        kord.login {
            intents = Intents.nonPrivileged + Intent.GuildMembers
        }
    }

    private fun Kord.listeners() {
        whenReady()
        whenDisconnected()
        whenResumed()
        kord.addNameWatcher(this@DevCordBotImpl)

        val ratProtector = RatProtector(this@DevCordBotImpl)
        with(ratProtector) {
            onReactionAdd()
        }

        val selfMentionListener = SelfMentionListener(this@DevCordBotImpl)
        with(selfMentionListener) {
            onMessageReceive()
        }

        val databaseUpdater = DatabaseUpdater(this@DevCordBotImpl)
        with(databaseUpdater) {
            registerListeners()
        }

        with(commandClient) {
            onInteraction()
        }

        val devmarkt = DevmarktRequestUpdater(this@DevCordBotImpl)
        with(devmarkt) {
            registerListeners()
        }
    }

    /**
     * Fired when the Discord bot has started successfully.
     */
    @OptIn(ExperimentalTime::class)
    private fun Kord.whenReady() = on<ReadyEvent> {
        logger.info { "Received Ready event, initializing bot internals..." }
        isInitialized = true
    }

    /**
     * Fired when the Discord connection gets interrupted
     */
    private fun Kord.whenDisconnected() = on<DisconnectEvent> {
        logger.warn { "Bot got disconnected (code: $this), disabling Discord specific internals" }
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

    private suspend fun connectToDatabase() {
        val databaseConfig = config.database
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://${databaseConfig.host}/${databaseConfig.database}"
            username = databaseConfig.username
            password = databaseConfig.password

            maximumPoolSize = databaseConfig.maximumPoolSize
        }
        dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        newSuspendedTransaction {
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
        runBlocking {
            autoHelp.close()
        }
    }

    private suspend fun registerCommands() {
        commandClient.registerCommands(
            HelpCommand(),
            TagCommand().apply {
                registerReadCommand(commandClient)
            },
            EvalCommand(),
            BotOwnerEvalCommand(),
            SourceCommand(),
            RankCommand(),
            RanksCommand(),
            BlacklistCommand(),
            InfoCommand(),
            CleanupCommand(),
            GoogleCommand()
        )

        val redeployHost = config.redeployment.host
        val redeployToken = config.redeployment.token
        if (redeployHost != null && redeployToken != null && redeployHost.isNotBlank() && redeployToken.isNotBlank()) {
            commandClient.registerCommands(RedeployCommand(redeployHost, redeployToken))
        }

        (commandClient as CommandClientImpl).updateCommands()
    }
}
