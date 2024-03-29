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

package com.github.devcordde.devcordbot.command

import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.permission.PermissionState
import com.github.devcordde.devcordbot.database.DevCordUser
import dev.kord.core.entity.Member

/**
 * Handler for command permissions.
 */
interface PermissionHandler {

    /**
     * Checks whether the [executor] covers the [permission] or not.
     */
    suspend fun isCovered(
        permission: Permission,
        executor: Member?,
        devCordUser: DevCordUser?,
        acknowledgeBlacklist: Boolean = true,
        isSlashCommand: Boolean = false
    ): PermissionState
}
