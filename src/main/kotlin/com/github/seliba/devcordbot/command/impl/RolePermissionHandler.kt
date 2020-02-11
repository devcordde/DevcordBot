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

package com.github.seliba.devcordbot.command.impl

import com.github.seliba.devcordbot.command.PermissionHandler
import com.github.seliba.devcordbot.command.perrmission.Permission
import net.dv8tion.jda.api.entities.Member

/**
 * Implementation of [PermissionHandler] that checks the users roles,
 */
class RolePermissionHandler : PermissionHandler {
    private val moderatorPattern = "(?i)moderator|administrator".toRegex()

    override fun isCovered(
        permission: Permission,
        executor: Member
    ): Boolean {
        return when (permission) {
            Permission.ANY -> true
            Permission.MODERATOR -> executor.roles.any { it.name.matches(moderatorPattern) }
            Permission.ADMIN -> executor.roles.any { it.name.equals("administrator", ignoreCase = true) }
        }
    }
}
