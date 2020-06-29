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

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * A modified version of [java.util.concurrent.Executors] default thread factory with custom name support.
 * Format `name-pool-poolNumber-thread-threadNumber`
 *
 * @see java.util.concurrent.Executors
 */
class DefaultThreadFactory(poolName: String?) : ThreadFactory {
    private val group: ThreadGroup
    private val threadNumber = AtomicInteger(1)
    private val namePrefix: String

    init {
        val s = System.getSecurityManager()
        group = if (s != null) s.threadGroup else Thread.currentThread().threadGroup
        namePrefix = (if (poolName != null) "$poolName-" else "") + "pool-" +
                poolNumber.getAndIncrement() +
                "-thread-"
    }

    /**
     * Creates a new [Thread] and returns it
     * @return The newly created [Thread]
     * @see Thread
     */
    override fun newThread(runnable: Runnable): Thread {
        val t = Thread(
            group, runnable,
            namePrefix + threadNumber.getAndIncrement(),
            0
        )
        if (t.isDaemon) t.isDaemon = false
        if (t.priority != Thread.NORM_PRIORITY) t.priority = Thread.NORM_PRIORITY
        return t
    }

    companion object {
        private val poolNumber = AtomicInteger(1)

        /**
         * Creates a new [Executors.newSingleThreadExecutor] using [poolName].
         */
        fun newSingleThreadExecutor(poolName: String?): ExecutorService =
            Executors.newSingleThreadExecutor(DefaultThreadFactory(poolName))
    }
}
