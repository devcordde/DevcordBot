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

package com.github.seliba.devcordbot.core

import com.github.seliba.devcordbot.command.CommandClient
import com.github.seliba.devcordbot.command.impl.CommandClientImpl
import com.github.seliba.devcordbot.command.impl.RolePermissionHandler
import com.github.seliba.devcordbot.commands.`fun`.SourceCommand
import com.github.seliba.devcordbot.commands.general.*
import com.github.seliba.devcordbot.commands.general.jdoodle.EvalCommand
import com.github.seliba.devcordbot.commands.moderation.BlacklistCommand
import com.github.seliba.devcordbot.commands.moderation.StarboardCommand
import com.github.seliba.devcordbot.commands.owners.RedeployCommand
import com.github.seliba.devcordbot.constants.Constants
import com.github.seliba.devcordbot.core.autohelp.AutoHelp
import com.github.seliba.devcordbot.database.*
import com.github.seliba.devcordbot.event.AnnotatedEventManager
import com.github.seliba.devcordbot.event.EventSubscriber
import com.github.seliba.devcordbot.event.MessageListener
import com.github.seliba.devcordbot.listeners.DatabaseUpdater
import com.github.seliba.devcordbot.listeners.SelfMentionListener
import com.zaxxer.hikari.HikariDataSource
import io.github.cdimascio.dotenv.Dotenv
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.DisconnectEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.ReconnectedEvent
import net.dv8tion.jda.api.events.ResumedEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import okhttp3.OkHttpClient
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import com.github.seliba.devcordbot.commands.owners.EvalCommand as OwnerEvalCommand

/**
 * General class to manage the Discord bot.
 */
internal class DevCordBotImpl(
    token: String,
    games: List<GameAnimator.AnimatedGame>,
    var env: Dotenv,
    override val debugMode: Boolean
) : DevCordBot {

    private val logger = KotlinLogging.logger { }
    private val restActionLogger = KotlinLogging.logger("RestAction")
    private lateinit var dataSource: HikariDataSource

    override val commandClient: CommandClient =
        CommandClientImpl(this, Constants.prefix, RolePermissionHandler(env["BOT_OWNERS"]!!.split(',')))
    override val httpClient: OkHttpClient = OkHttpClient()
    override val starboard: Starboard =
        Starboard(env["STARBOARD_CHANNEL_ID"]?.toLong() ?: error("STARBOARD_CHANNEL_ID is required in .env"))

    override val jda: JDA = JDABuilder.create(
        token,
        GatewayIntent.getIntents(
            GatewayIntent.ALL_INTENTS and GatewayIntent.getRaw(
                GatewayIntent.GUILD_MESSAGE_TYPING,
                GatewayIntent.DIRECT_MESSAGE_TYPING
            ).inv()
        )
    )
        .setEventManager(AnnotatedEventManager())
        .setDisabledCacheFlags(EnumSet.of(CacheFlag.VOICE_STATE, CacheFlag.CLIENT_STATUS))
        .setMemberCachePolicy(MemberCachePolicy.ALL)
        .setActivity(Activity.playing("Starting ..."))
        .setStatus(OnlineStatus.DO_NOT_DISTURB)
        .setHttpClient(httpClient)
        .addEventListeners(
            MessageListener(),
            this@DevCordBotImpl,
            SelfMentionListener(),
            DatabaseUpdater(),
            commandClient,
            starboard,
            AutoHelp(
                this,
                env["AUTO_HELP_WHITELIST"]!!.split(','),
                env["AUTO_HELP_BLACKLIST"]!!.split(','),
                env["AUTO_HELP_KNOWN_LANGUAGES"]!!.split(','),
                env["AUTO_HELP_BYPASS"]!!,
                Integer.parseInt(env["AUTO_HELP_MAX_LINES"])
            )
        )
        .build()
    override val gameAnimator = GameAnimator(jda, games)

    override lateinit var guild: Guild

    /**
     * Whether the bot received the [ReadyEvent] or not.
     */
    override var isInitialized: Boolean = false
        private set

    init {
        Runtime.getRuntime().addShutdownHook(Thread(this::shutdown))
        RestAction.setDefaultFailure {
            restActionLogger.error(it) { "An error occurred while executing restaction" }
        }
        registerCommands(env)
        logger.info { "Establishing connection to the database …" }
        connectToDatabase(env)
    }

    /**
     * Fired when the Discord bot has started successfully.
     */
    @EventSubscriber
    fun whenReady(event: ReadyEvent) {
        logger.info { "Received Ready event initializing bot internals …" }
        isInitialized = true
        event.jda.presence.setStatus(OnlineStatus.ONLINE)
        gameAnimator.start()

        @Suppress("ReplaceNotNullAssertionWithElvisReturn")
        guild = event.jda.getGuildById(env["GUILD_ID"]!!)!!
    }

    /**
     * Fired when the Discord connection gets interrupted
     */
    @EventSubscriber
    fun whenDisconnected(event: DisconnectEvent) {
        logger.warn { "Bot got disconnected (code: ${event.closeCode}) disabling Discord specific internals" }
        isInitialized = false
        gameAnimator.stop()
    }

    /**
     * Fired when the bot can resume its previous connections when reconnecting.
     */
    @EventSubscriber
    fun whenResumed(@Suppress("UNUSED_PARAMETER") event: ResumedEvent) = reinitialize()

    /**
     * Fired when the bot reconnects.
     */
    @EventSubscriber
    fun whenReconnect(@Suppress("UNUSED_PARAMETER") event: ReconnectedEvent) = reinitialize()

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
            SchemaUtils.createMissingTablesAndColumns(Users, Tags, TagAliases, StarboardEntries, Starrers)
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
            TagCommand(),
            EvalCommand(),
            OwnerEvalCommand(),
            StarboardCommand(),
            SourceCommand(),
            RankCommand(),
            RanksCommand(),
            BlacklistCommand(),
            InfoCommand()
        )

        val cseKey = env["CSE_KEY"]
        val cseId = env["CSE_ID"]
        if (cseKey != null && cseId != null) {
            commandClient.registerCommands(GoogleCommand(cseKey, cseId))
        }

        val redeployHost = env["REDEPLOY_HOST"]
        val redeployToken = env["REDEPLOY_TOKEN"]
        if (redeployHost != null && redeployToken != null) {
            commandClient.registerCommands(RedeployCommand(redeployHost, redeployToken))
        }
    }
}
