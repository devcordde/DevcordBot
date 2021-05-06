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

package com.github.devcordde.devcordbot.command.impl

import com.github.devcordde.devcordbot.command.ErrorHandler
import com.github.devcordde.devcordbot.command.context.Context
import kotlinx.coroutines.launch
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext

/**
 * Implementation of [ErrorHandler] that only logs the error.
 */
class DebugErrorHandler : ErrorHandler {

    private val logger = KotlinLogging.logger { }

    override fun handleException(
        exception: Throwable,
        context: Context,
        thread: Thread,
        coroutineContext: CoroutineContext?
    ) {
        logger.error(exception) { "An error occurred while executing a command in $context." }
        context.bot.launch {
            context.respond("Es ist ein Fehler aufgetreten! Der ErrorHandler wurde wegen des Debug modus deaktiviert")
        }
    }
}
