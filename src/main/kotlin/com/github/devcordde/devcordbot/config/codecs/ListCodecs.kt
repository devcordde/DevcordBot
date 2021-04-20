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

package com.github.devcordde.devcordbot.config.codecs

import com.github.devcordde.devcordbot.config.helpers.AnimatedGameList
import com.github.devcordde.devcordbot.config.helpers.SnowflakeList
import com.github.devcordde.devcordbot.core.GameAnimator
import dev.kord.common.entity.ActivityType
import dev.kord.common.entity.Snowflake

/**
 * Codec for [AnimatedGameList].
 */
object AnimatedGameListCodec : ListCodec<AnimatedGameList, GameAnimator.AnimatedGame>(contentMapper = {
    val type = if (it.startsWith("!")) {
        ActivityType.Listening
    } else {
        ActivityType.Game
    }

    GameAnimator.AnimatedGame(it, type)
}) {
    override fun List<GameAnimator.AnimatedGame>.map(): AnimatedGameList = AnimatedGameList(this)
}

/**
 * Codec for [SnowflakeList].
 */
object SnowflakeListCodec : ListCodec<SnowflakeList, Snowflake>(contentMapper = { Snowflake(it) }) {
    override fun List<Snowflake>.map(): SnowflakeList = SnowflakeList(this)
}
