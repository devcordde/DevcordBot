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

import com.uchuhimo.konf.Config as Konfig

class Config(private val config: Konfig) {
    val xpWhielist = config[ConfigSpec.xpWhielist]
    val hasteHost = config[ConfigSpec.hasteHost]

    val discord = Discord()
    val database = Database()
    val jdoodle = Jdoodle()
    val sentry = Sentry()
    val cse = Cse()
    val redeployment = Redeployment()
    val devrat = Devrat()
    val devmarkt = Devmarkt()
    val permissions = Permissions()

    inner class Discord internal constructor() {
        val token = config[ConfigSpec.Discord.token]
        val guildId = config[ConfigSpec.Discord.guildId]
        val games = config[ConfigSpec.Discord.games]
    }

    inner class Database internal constructor() {
        val host = config[ConfigSpec.Database.host]
        val database = config[ConfigSpec.Database.database]
        val username = config[ConfigSpec.Database.username]
        val password = config[ConfigSpec.Database.password]
    }

    inner class Jdoodle internal constructor() {
        val clientId = config[ConfigSpec.Jdoodle.clientId]
        val clientSecret = config[ConfigSpec.Jdoodle.clientSecret]
    }

    inner class Sentry internal constructor() {
        val dsn = config[ConfigSpec.Sentry.dsn]
    }

    inner class Cse internal constructor() {
        val key = config[ConfigSpec.Cse.key]
        val id = config[ConfigSpec.Cse.id]
    }

    inner class Redeployment internal constructor() {
        val host = config[ConfigSpec.Redeployment.host]
        val token = config[ConfigSpec.Redeployment.token]
    }

    inner class Devrat internal constructor() {
        val channelId = config[ConfigSpec.Devrat.channelId]
        val roleId = config[ConfigSpec.Devrat.roleId]
    }

    inner class Devmarkt internal constructor() {
        val requestChannel = config[ConfigSpec.Devmarkt.requestChannel]
        val moderatorId = config[ConfigSpec.Devmarkt.moderatorId]
        val baseUrl = config[ConfigSpec.Devmarkt.baseUrl]
        val accessToken = config[ConfigSpec.Devmarkt.accessToken]
        val checkEmote = config[ConfigSpec.Devmarkt.checkEmote]
        val blockEmote = config[ConfigSpec.Devmarkt.blockEmote]
    }

    inner class Permissions internal constructor() {
        val botOwners = config[ConfigSpec.Permissions.botOwners]
        val modId = config[ConfigSpec.Permissions.modId]
        val adminId = config[ConfigSpec.Permissions.adminId]
    }
}
