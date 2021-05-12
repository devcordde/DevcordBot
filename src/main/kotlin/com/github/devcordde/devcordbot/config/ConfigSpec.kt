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

import com.github.devcordde.devcordbot.config.helpers.SnowflakeList
import com.github.devcordde.devcordbot.core.GameAnimator
import dev.kord.common.entity.Snowflake
import io.ktor.http.*
import com.uchuhimo.konf.ConfigSpec as KonfigSpec

internal object ConfigSpec : KonfigSpec("") {

    object General : KonfigSpec() {
        val xpWhitelist by optional(SnowflakeList(), "XP_WHITELIST")
        val hasteHost by optional(Url("https://haste.devcord.xyz"), "HASTE_HOST")
    }

    object Discord : KonfigSpec() {
        val token by required<String>("TOKEN")

        val guildId by required<Snowflake>("GUILD_ID")
        val games by required<List<GameAnimator.AnimatedGame>>("GAMES")
    }

    object Database : KonfigSpec() {
        val host by required<String>("HOST")
        val database by required<String>("NAME")
        val username by required<String>("USERNAME")
        val password by required<String>("PASSWORD")
    }

    object Jdoodle : KonfigSpec() {
        val clientId by required<String>("CLIENTID")
        val clientSecret by required<String>("CLIENTSECRET")
    }

    object Sentry : KonfigSpec() {
        val dsn by optional("", "DSN")
    }

    object Cse : KonfigSpec() {
        val key by optional<String?>(null, "KEY")
        val id by optional<String?>(null, "ID")
    }

    object Redeployment : KonfigSpec("REDEPLOY") {
        val host by optional<String?>(null, "HOST")
        val token by optional<String?>(null, "TOKEN")
    }

    object Devrat : KonfigSpec("RAT") {
        val channelId by required<Snowflake>("CHANNEL_ID")
        val roleId by required<Snowflake>("ROLE_ID")
    }

    object Devmarkt : KonfigSpec("DEVMARKT") {
        val requestChannel by required<Snowflake>("REQUEST_CHANNEL")
        val moderatorId by required<Snowflake>("MODERATOR_ID")
        val baseUrl by required<Url>("BASE_URL")
        val accessToken by required<String>("BOT_ACCESS_TOKEN")
        val checkEmote by required<Snowflake>("EMOTE_CHECK_ID")
        val blockEmote by required<Snowflake>("EMOTE_BLOCK_ID")
    }

    object Permissions : KonfigSpec() {
        val botOwners by optional(SnowflakeList(), "BOT_OWNERS")
        val modId by required<Snowflake>("MOD_ROLE_ID")
        val adminId by required<Snowflake>("ADMIN_ROLE_ID")
    }

    object AutoHelp : KonfigSpec() {
        val host by required<Url>()
        val key by required<String>()
        val channels by optional<List<Long>>(emptyList())
    }
}
