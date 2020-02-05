/*
 * Copyright 2020 Daniel Scherf & Michael Rittmeister
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

package com.github.seliba.devcordbot.listeners

import com.github.seliba.devcordbot.database.DevCordUser
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent

/**
 * Updates the Database based on Discord events.
 */
class DatabaseUpdater {

    /**
     * Adds a user to the database when a user joins the guild.
     */
    @SubscribeEvent
    fun onMemberJoin(event: GuildMemberJoinEvent): Unit = createUserInDatabase(event.member.idLong)

    /**
     * Removes a user from the database when the user leaves the guild.
     */
    @SubscribeEvent
    fun onMemberLeave(event: GuildMemberLeaveEvent): Unit = deleteUser(event.member.idLong)

    private fun createUserInDatabase(id: Long) {
        DevCordUser.new(id) {}
    }

    private fun deleteUser(id: Long) = DevCordUser.findById(id)?.delete() ?: Unit

}