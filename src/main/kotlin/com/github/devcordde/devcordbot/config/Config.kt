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
    val xpWhielist: List<Snowflake> = config[ConfigSpec.xpWhielist]

    /**
     * The URL to the hastebin server the bot should use
     *
     * Default: haste.devcord.xyz
     * Key: `HASTE_HOST`
     */
    val hasteHost: Url = config[ConfigSpec.hasteHost]

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
        val token: String = config[ConfigSpec.Discord.token]

        /**
         * The guild id of the bots main guild (devcord).
         *
         * Default: **required**
         * Key: `GUILD_ID`
         */
        val guildId: Snowflake = config[ConfigSpec.Discord.guildId]

        /**
         * A list of strings shown in the game animator
         *
         * Default: Empty
         * Key: `GAMES`
         * @see GameAnimator
         */
        val games: List<GameAnimator.AnimatedGame> = config[ConfigSpec.Discord.games]
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
        val host: String = config[ConfigSpec.Database.host]

        /**
         * Name of the PostgreSQL database.
         *
         * Default: **required**
         * Key: `DATABASE`
         */
        val database: String = config[ConfigSpec.Database.database]

        /**
         * Name of the PostgreSQL user.
         *
         * Default: **required**
         * Key: `DATABASE_USERNAME`
         */
        val username: String = config[ConfigSpec.Database.username]

        /**
         * Password of the PostgreSQL user.
         *
         * Default: **required**
         * Key: `DATABASE_USERNAME`
         */
        val password: String = config[ConfigSpec.Database.password]
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
        val clientId: String = config[ConfigSpec.Jdoodle.clientId]

        /**
         * JDoodle client secret.
         *
         * Default: **required**
         * Key: `JDOODLE_CLIENTSECRET`
         */
        val clientSecret: String = config[ConfigSpec.Jdoodle.clientSecret]
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
        val dsn: String = config[ConfigSpec.Sentry.dsn]
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
        val key: String? = config[ConfigSpec.Cse.key]

        /**
         * Id of CSE.
         *
         * Default: Empty
         * Key: `CSE_ID`
         */
        val id: String? = config[ConfigSpec.Cse.id]
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
        val host: String? = config[ConfigSpec.Redeployment.host]

        /**
         * Token to authenticate with the redeploy hook.
         *
         * Default: Empty
         * Key: `REDEPLOY_TOKEN`
         */
        val token: String? = config[ConfigSpec.Redeployment.token]
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
        val channelId: Snowflake = config[ConfigSpec.Devrat.channelId]

        /**
         *The id of the dev rat role.
         *
         * Default: **required**
         * Key: `RAT_ROLE_ID`
         */
        val roleId: Snowflake = config[ConfigSpec.Devrat.roleId]
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
        val requestChannel: Snowflake = config[ConfigSpec.Devmarkt.requestChannel]

        /**
         * The id of the mod role.
         *
         * Default: **required**
         * Key: `DEVMARKT_MODERATOR_ID`
         */
        val moderatorId: Snowflake = config[ConfigSpec.Devmarkt.moderatorId]

        /**
         * The base url of the devmarkt api.
         *
         * Default: **required**
         * Key: `DEVMARKT_BASE_URL`
         */
        val baseUrl: Url = config[ConfigSpec.Devmarkt.baseUrl]

        /**
         * The authentication token for the api.
         *
         * Default: **required**
         * Key: `BOT_ACCESS_TOKEN`
         */
        val accessToken: String = config[ConfigSpec.Devmarkt.accessToken]

        /**
         * THe id of the check emote.
         *
         * Default: **required**
         * Key: `EMOTE_CHECK_ID`
         */
        val checkEmote: Snowflake = config[ConfigSpec.Devmarkt.checkEmote]

        /**
         * The block emote id.
         *
         * Default: **required**
         * Key: `EMOTE_BLOCK_ID`
         */
        val blockEmote: Snowflake = config[ConfigSpec.Devmarkt.blockEmote]
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
        val botOwners: List<Snowflake> = config[ConfigSpec.Permissions.botOwners]

        /**
         * API key for CSE API.
         *
         * Default: **required**
         * Key: `MOD_ROLE_ID`
         */
        val modId: Snowflake = config[ConfigSpec.Permissions.modId]

        /**
         * API key for CSE API.
         *
         * Default: **required**
         * Key: `ADMIN_ROLE_ID`
         */
        val adminId: Snowflake = config[ConfigSpec.Permissions.adminId]
    }
}
