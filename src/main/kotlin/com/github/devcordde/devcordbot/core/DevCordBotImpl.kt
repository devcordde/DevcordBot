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
import com.github.devcordde.devcordbot.core.autohelp.AutoHelp
import com.github.devcordde.devcordbot.database.*
import com.github.devcordde.devcordbot.event.AnnotatedEventManager
import com.github.devcordde.devcordbot.event.EventSubscriber
import com.github.devcordde.devcordbot.event.MessageListener
import com.github.devcordde.devcordbot.listeners.DatabaseUpdater
import com.github.devcordde.devcordbot.listeners.DevmarktRequestUpdater
import com.github.devcordde.devcordbot.listeners.SelfMentionListener
import com.github.devcordde.devcordbot.util.GithubUtil
import com.github.devcordde.devcordbot.util.Googler
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
import com.github.devcordde.devcordbot.commands.owners.EvalCommand as OwnerEvalCommand

/**
 * General class to manage the Discord bot.
 */
internal class DevCordBotImpl(
    token: String,
    games: List<GameAnimator.AnimatedGame>,
    env: Dotenv,
    override val debugMode: Boolean
) : DevCordBot {

    private val logger = KotlinLogging.logger { }
    private val restActionLogger = KotlinLogging.logger("RestAction")
    private lateinit var dataSource: HikariDataSource

    private val modRoleId = env["MOD_ROLE"]!!.toLong()
    private val adminRoleId = env["ADMIN_ROLE"]!!.toLong()
    private val botOwners = env["BOT_OWNERS"]!!.split(',').map { it.toLong() }

    override val commandClient: CommandClient =
        CommandClientImpl(this, Constants.prefix, modRoleId, adminRoleId, botOwners, RolePermissionHandler(botOwners))
    override val httpClient: OkHttpClient = OkHttpClient()
    override val github: GithubUtil = GithubUtil(httpClient)

    override val googler: Googler = Googler(env["CSE_KEY"]!!, env["CSE_ID"]!!)

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
        .disableCache(EnumSet.of(CacheFlag.VOICE_STATE, CacheFlag.CLIENT_STATUS))
        .setMemberCachePolicy(MemberCachePolicy.ALL)
        .setActivity(Activity.playing("Starting ..."))
        .setStatus(OnlineStatus.DO_NOT_DISTURB)
        .setHttpClient(httpClient)
        .addEventListeners(
            RatProtector(env["RAT_CHANNEL_ID"]!!.toLong(), env["RAT_ROLE_ID"]!!.toLong(), this),
            MessageListener(),
            this@DevCordBotImpl,
            SelfMentionListener(this),
            DatabaseUpdater(env["XP_WHITELIST"]!!.split(",")),
            commandClient,
            AutoHelp(
                this,
                env["AUTO_HELP_WHITELIST"]!!.split(','),
                env["AUTO_HELP_BLACKLIST"]!!.split(','),
                env["AUTO_HELP_KNOWN_LANGUAGES"]!!.split(','),
                env["AUTO_HELP_BYPASS"]!!,
                Integer.parseInt(env["AUTO_HELP_MAX_LINES"])
            ),
            DevmarktRequestUpdater(
                env["DEVMARKT_REQUEST_CHANNEL"]!!,
                env["BOT_ACCESS_TOKEN"]!!,
                env["DEVMARKT_BASE_URL"]!!,
                env["EMOTE_CHECK_ID"]!!,
                env["EMOTE_BLOCK_ID"]!!,
            ),
        )
        .build()
    override val gameAnimator = GameAnimator(jda, games)

    private val guildId = env["GUILD_ID"]!!
    override val guild: Guild
        get() = jda.getGuildById(guildId)!!

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

        logger.info { "Establishing connection to the database …" }
        connectToDatabase(env)

        logger.info { "Registering commands …" }
        registerCommands(env)
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

        (commandClient as CommandClientImpl).updateCommands().queue()
    }
}
