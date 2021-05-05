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

package com.github.devcordde.devcordbot.config

import com.github.devcordde.devcordbot.core.GameAnimator
import dev.kord.common.entity.Snowflake
import io.ktor.http.*
import com.uchuhimo.konf.Config as Konfig

/**
 * Central configuration of the bot.
 *
 * @see Config
 */
class Config internal constructor(private val config: Konfig) {
    /**
     * A list of channel ids which give XP.
     *
     * Default: Empty
     * Key: `XP_WHITELIST`
     */
    val xpWhitelist: List<Snowflake> by config.property(ConfigSpec.xpWhitelist)

    /**
     * The URL to the hastebin server the bot should use
     *
     * Default: haste.devcord.xyz
     * Key: `HASTE_HOST`
     */
    val hasteHost: Url by config.property(ConfigSpec.hasteHost)

    /**
     * Discord related configuration.
     *
     * @see Discord
     */
    val discord: Discord = Discord()

    /**
     * Database related configuration.
     *
     * @see Database
     */
    val database: Database = Database()

    /**
     * Jdoodle related configuration
     *
     * @see Jdoodle
     */
    val jdoodle: Jdoodle = Jdoodle()

    /**
     * Sentry related configuration.
     *
     * @see Sentry
     */
    val sentry: Sentry = Sentry()

    /**
     * Google Custom Search related configuration
     *
     * @see Cse
     */
    val cse: Cse = Cse()

    /**
     * Redeployment related configuration
     *
     * @see Redeployment
     */
    val redeployment: Redeployment = Redeployment()

    /**
     * Devrat related configuration
     *
     * @see Devrat
     */
    val devrat: Devrat = Devrat()

    /**
     * Devmarkt related configuration
     *
     * @see Devrat
     */
    val devmarkt: Devmarkt = Devmarkt()

    /**
     * Permission related configuration.
     *
     * @see Permissions
     */
    val permissions: Permissions = Permissions()

    /**
     * Autohelp related config.
     *
     * @see AutoHelp
     */
    val autoHelp: AutoHelp = AutoHelp()

    /**
     * Discord related configuration.
     *
     * @see Config.discord
     */
    inner class Discord internal constructor() {
        /**
         * The Discord bot token
         *
         * Default: **Required**
         * Key: `DISCORD_TOKEN`
         */
        val token: String by config.property(ConfigSpec.Discord.token)

        /**
         * The guild id of the bots main guild (devcord).
         *
         * Default: **required**
         * Key: `GUILD_ID`
         */
        val guildId: Snowflake by config.property(ConfigSpec.Discord.guildId)

        /**
         * A list of strings shown in the game animator
         *
         * Default: Empty
         * Key: `GAMES`
         * @see GameAnimator
         */
        val games: List<GameAnimator.AnimatedGame> by config.property(ConfigSpec.Discord.games)
    }

    /**
     * Database related configuration.
     *
     * @see Config.Database
     */
    inner class Database internal constructor() {
        /**
         * Host of the PostgreSQL server.
         *
         * Default: **required**
         * Key: `DATABASE_HOST`
         */
        val host: String by config.property(ConfigSpec.Database.host)

        /**
         * Name of the PostgreSQL database.
         *
         * Default: **required**
         * Key: `DATABASE`
         */
        val database: String by config.property(ConfigSpec.Database.database)

        /**
         * Name of the PostgreSQL user.
         *
         * Default: **required**
         * Key: `DATABASE_USERNAME`
         */
        val username: String by config.property(ConfigSpec.Database.username)

        /**
         * Password of the PostgreSQL user.
         *
         * Default: **required**
         * Key: `DATABASE_USERNAME`
         */
        val password: String by config.property(ConfigSpec.Database.password)
    }

    /**
     * Jdoodle related configuration
     *
     * @see Jdoodle
     */
    inner class Jdoodle internal constructor() {
        /**
         * JDoodle client id.
         *
         * Default: **required**
         * Key: `JDOODLE_CLIENTID`
         */
        val clientId: String by config.property(ConfigSpec.Jdoodle.clientId)

        /**
         * JDoodle client secret.
         *
         * Default: **required**
         * Key: `JDOODLE_CLIENTSECRET`
         */
        val clientSecret: String by config.property(ConfigSpec.Jdoodle.clientSecret)
    }

    /**
     * Sentry related configuration.
     *
     * @see Config.sentry
     */
    inner class Sentry internal constructor() {
        /**
         * Sentry DSN.
         *
         * Default: Empty
         * Key: `SENTRY_DSN`
         */
        val dsn: String by config.property(ConfigSpec.Sentry.dsn)
    }

    /**
     * Google custom search design.
     *
     * @see Sentry
     */
    inner class Cse internal constructor() {
        /**
         * API key for CSE API.
         *
         * Default: Empty
         * Key: `CSE_KEY`
         */
        val key: String? by config.property(ConfigSpec.Cse.key)

        /**
         * Id of CSE.
         *
         * Default: Empty
         * Key: `CSE_ID`
         */
        val id: String? by config.property(ConfigSpec.Cse.id)
    }

    /**
     * Redeployment related configuration
     *
     * @see Config.redeployment
     */
    inner class Redeployment internal constructor() {
        /**
         * Host of the redeploy hook.
         *
         * Default: Empty
         * Key: `REDEPLOY_HOST`
         */
        val host: String? by config.property(ConfigSpec.Redeployment.host)

        /**
         * Token to authenticate with the redeploy hook.
         *
         * Default: Empty
         * Key: `REDEPLOY_TOKEN`
         */
        val token: String? by config.property(ConfigSpec.Redeployment.token)
    }

    /**
     * Devrat related configuration
     *
     * @see Devrat
     */
    inner class Devrat internal constructor() {
        /**
         * The channel id of the dev rat channel.
         *
         * Default: **required**
         * Key: `RAT_CHANNEL_ID`
         */
        val channelId: Snowflake by config.property(ConfigSpec.Devrat.channelId)

        /**
         *The id of the dev rat role.
         *
         * Default: **required**
         * Key: `RAT_ROLE_ID`
         */
        val roleId: Snowflake by config.property(ConfigSpec.Devrat.roleId)
    }

    /**
     * Devmarkt related configuration
     *
     * @see Devrat
     */
    inner class Devmarkt internal constructor() {
        /**
         * The channel for Devmark requests.
         *
         * Default: **required**
         * Key: `DEVMARKT_REQUEST_CHANNEL`
         */
        val requestChannel: Snowflake by config.property(ConfigSpec.Devmarkt.requestChannel)

        /**
         * The id of the mod role.
         *
         * Default: **required**
         * Key: `DEVMARKT_MODERATOR_ID`
         */
        val moderatorId: Snowflake by config.property(ConfigSpec.Devmarkt.moderatorId)

        /**
         * The base url of the devmarkt api.
         *
         * Default: **required**
         * Key: `DEVMARKT_BASE_URL`
         */
        val baseUrl: Url by config.property(ConfigSpec.Devmarkt.baseUrl)

        /**
         * The authentication token for the api.
         *
         * Default: **required**
         * Key: `BOT_ACCESS_TOKEN`
         */
        val accessToken: String by config.property(ConfigSpec.Devmarkt.accessToken)

        /**
         * THe id of the check emote.
         *
         * Default: **required**
         * Key: `EMOTE_CHECK_ID`
         */
        val checkEmote: Snowflake by config.property(ConfigSpec.Devmarkt.checkEmote)

        /**
         * The block emote id.
         *
         * Default: **required**
         * Key: `EMOTE_BLOCK_ID`
         */
        val blockEmote: Snowflake by config.property(ConfigSpec.Devmarkt.blockEmote)
    }

    /**
     * Permission related configuration.
     *
     * @see Permissions
     */
    inner class Permissions internal constructor() {

        /**
         * API key for CSE API.
         *
         * Default: **required**
         * Key: `BOT_OWNERS`
         */
        val botOwners: List<Snowflake> by config.property(ConfigSpec.Permissions.botOwners)

        /**
         * API key for CSE API.
         *
         * Default: **required**
         * Key: `MOD_ROLE_ID`
         */
        val modId: Snowflake by config.property(ConfigSpec.Permissions.modId)

        /**
         * API key for CSE API.
         *
         * Default: **required**
         * Key: `ADMIN_ROLE_ID`
         */
        val adminId: Snowflake by config.property(ConfigSpec.Permissions.adminId)
    }

    /**
     * AutoHelp related configuration.
     *
     * @see Config.autoHelp
     */
    inner class AutoHelp internal constructor() {
        /**
         * The host of the autohelp server.
         *
         * @see Url
         */
        val host: Url by config.property(ConfigSpec.AutoHelp.host)

        /**
         * The authentication key for the autohelp server.
         */
        val key: String by config.property(ConfigSpec.AutoHelp.key)

        /**
         * A list of channels that are whitelisted for autohelp.
         */
        val channels: List<Long> by config.property(ConfigSpec.AutoHelp.channels)
    }
}
