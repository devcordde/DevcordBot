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

import com.github.devcordde.devcordbot.command.context.Context
import kotlin.coroutines.CoroutineContext

/**
 * Interface for handling errors during command execution
 */
interface ErrorHandler {

    /**
     * Handles the [exception] in [context].
     */
    fun handleException(
        exception: Throwable,
        context: Context,
        thread: Thread,
        coroutineContext: CoroutineContext? = null
    )
}
