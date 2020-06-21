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

package com.github.devcordde.devcordbot.util

import com.github.devcordde.devcordbot.core.DevCordBot
import com.github.devcordde.devcordbot.database.Punishment
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.Period
import org.joda.time.format.PeriodFormatterBuilder
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 *
 */
class Punisher(private val bot: DevCordBot, private val mutedRoleId: String) {
    private val scheduler = Executors.newScheduledThreadPool(1)

    /**
     * Closes the PunishmentRemover.
     */
    fun shutdown() {
        scheduler.shutdown()
    }

    /**
     * Start the Punishment Timers saved in the database.
     */
    fun startOldPunishments() {
        transaction {
            Punishment.all().forEach {
                startPunishmentTimer(it)
            }
        }
    }

    /**
     * Adds a mute to db and start scheduler.
     */
    fun addMute(userId: String, executionTime: Period) {
        addPunishment("mute", userId, executionTime)
    }

    /**
     * Adds a mute to db and start scheduler.
     */
    fun addBan(userId: String, executionTime: Period) {
        addPunishment("ban", userId, executionTime)
    }

    private fun addPunishment(kind: String, userId: String, executionTime: Period) {


        val punishment = transaction {
            Punishment.new {
                this.kind = kind
                this.userId = userId
                this.executionTime = Instant.now().plusMillis(executionTime.toStandardDuration().millis)
            }
        }

        startPunishmentTimer(punishment)
    }

    private fun startPunishmentTimer(punishment: Punishment) {
        val executionTime = punishment.executionTime.toEpochMilli() - System.currentTimeMillis()

        if (executionTime <= 0) return executePunishmentRemoval(punishment)

        scheduler.schedule({ executePunishmentRemoval(punishment) }, executionTime, TimeUnit.MILLISECONDS)
    }

    private fun executePunishmentRemoval(punishment: Punishment) {
        when (punishment.kind) {
            "mute" -> {
                val mutedRole = bot.guild.getRoleById(mutedRoleId)
                if (mutedRole != null) bot.guild.removeRoleFromMember(punishment.userId, mutedRole).queue()
            }
            "ban" -> bot.guild.unban(punishment.userId).queue()
        }

        transaction {
            punishment.delete()
        }
    }

    companion object {
        private val formatter =
            PeriodFormatterBuilder()
                .appendYears().appendSuffix("y")
                .appendMonths().appendSuffix("m")
                .appendWeeks().appendSuffix("w")
                .appendDays().appendSuffix("d ")
                .appendHours().appendSuffix("h")
                .toFormatter()

        /**
         * Format the given Period.
         */
        fun parsePeriod(period: String): Period? {
            return runCatching {
                formatter.parsePeriod(period)
            }.getOrNull()
        }
    }
}