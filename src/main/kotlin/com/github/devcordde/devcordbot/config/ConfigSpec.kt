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
import com.uchuhimo.konf.ConfigSpec as KonfigSpec

internal object ConfigSpec : KonfigSpec() {
    val xpWhielist by optional<List<String>>(emptyList(), "XP_WHITELIST")
    val hasteHost by optional(Url("https://haste.devcord.xyz"), "HASTE_HOST")

    object Discord : KonfigSpec() {
        val token by required<String>("DISCORD_TOKEN")

        val guildId by required<Snowflake>("GUILD_ID")
        val games by required<List<GameAnimator.AnimatedGame>>("GAMES")
    }

    object Database : KonfigSpec() {
        val host by required<String>("DATABASE_HOST")
        val database by required<String>("DATABASE")
        val username by required<String>("DATABASE_USERNAME")
        val password by required<String>("DATABASE_PASSWORD")
    }

    object Jdoodle : KonfigSpec() {
        val clientId by required<String>("JDOODLE_CLIENTID")
        val clientSecret by required<String>("JDOODLE_CLIENTSECRET")
    }

    object Sentry : KonfigSpec() {
        val dsn by optional("", "SENTRY_DSN")
    }

    object Cse : KonfigSpec() {
        val key by optional<String?>(null, "CSE_KEY")
        val id by optional<String?>(null, "CSE_ID")
    }

    object Redeployment {
        val host by optional<String?>(null, "REDEPLOY_HOST")
        val token by optional<String?>(null, "REDEPLOY_TOKEN")
    }

    object Devrat {
        val channelId by required<Snowflake>("RAT_CHANNEL_ID")
        val roleId by required<Snowflake>("RAT_ROLE_ID")
    }

    object Devmarkt {
        val requestChannel by required<Snowflake>("DEVMARKT_REQUEST_CHANNEL")
        val moderatorId by required<Snowflake>("DEVMARKT_MODERATOR_ID")
        val baseUrl by required<Url>("DEVMARKT_BASE_URL")
        val accessToken by required<String>("BOT_ACCESS_TOKEN")
        val checkEmote by required<Snowflake>("EMOTE_CHECK_ID")
        val blockEmote by required<Snowflake>("EMOTE_BLOCK_ID")
    }

    object Permissions {
        val botOwners by optional<List<Snowflake>>(emptyList(), "BOT_OWNERS")
        val modId by required<Snowflake>("BOT_OWNERS")
        val adminId by required<Snowflake>("BOT_OWNERS")
    }
}
