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

import kotlin.math.pow

/**
 * XP utilities.
 */
object XPUtil {
    private val xpMap = HashMap<Int, Long>()

    /**
     * Calculates or gets the needed amount of xp to [level].
     *
     * http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIoMip4KV4xLjgrMjAwIiwiY29sb3IiOiIjMDAwMDAwIn0seyJ0eXBlIjoxMDAwLCJ3aW5kb3ciOlsiLTAiLCIyMDAiLCItMCIsIjUwMDAwIl0sInNpemUiOlsxMTAwLDQwMF19XQ--
     */
    fun getXpToLevelup(level: Int): Long = xpMap[level] ?: {
        val requiredXp = (2.0 * level).pow(1.6).toLong() + 100
        xpMap[level] = requiredXp
        requiredXp
    }()
}