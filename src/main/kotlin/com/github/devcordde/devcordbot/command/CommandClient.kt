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

package com.github.devcordde.devcordbot.command

import dev.kord.core.Kord
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * Parser and manager for [AbstractCommand](commands).
 */
interface CommandClient : CommandRegistry<AbstractCommand> {

    /**
     * The [CoroutineContext] used to execute commands.
     */
    val executor: CoroutineContext

    /**
     * The [PermissionHandler] used for handling command permissions.
     * @see PermissionHandler
     */
    val permissionHandler: PermissionHandler

    /**
     * Handles errors during command execution
     * @see ErrorHandler
     */
    val errorHandler: ErrorHandler
    fun Kord.onInteraction(): Job
}
