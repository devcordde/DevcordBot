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

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.github.devcordde.devcordbot.core.GameAnimator
import dev.kord.common.entity.ActivityType

/**
 * Implementation of [JsonDeserializer] to deserializer [GameAnimator.AnimatedGame]s.
 *
 * If a game startrs with `!` [ActivityType.Listening] is used, otherwise it's [ActivityType.Game]
 */
object AnimatedGameCodec : JsonDeserializer<GameAnimator.AnimatedGame>() {
    /**
     * @see JsonDeserializer.deserialize
     */
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): GameAnimator.AnimatedGame {
        val content = p.valueAsString
        val type = if (content.startsWith("!")) {
            ActivityType.Listening
        } else {
            ActivityType.Game
        }

        return GameAnimator.AnimatedGame(content, type)
    }
}