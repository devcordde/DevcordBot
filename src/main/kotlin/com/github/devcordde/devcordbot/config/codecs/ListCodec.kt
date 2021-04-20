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
import com.github.devcordde.devcordbot.config.codecs.ListCodec.ListContentMapper

/**
 * Implementation of [JsonDeserializer] to deserialize delimited lists.
 *
 * @param separator the delimiter
 * @param contentMapper the [ListContentMapper] which converts the contents to [T]
 * @param L the wrapper list type like SnowflakeList
 */
abstract class ListCodec<L : List<T>, T>(
    private val separator: String = ";",
    private val contentMapper: ListContentMapper<T>
) : JsonDeserializer<L>() {

    /**
     * Functional interfaces to convert the contents of a list.
     */
    fun interface ListContentMapper<T> {
        /**
         * Creates [T] from [string].
         */
        fun fromString(string: String): T
    }

    /**
     * @see JsonDeserializer.deserialize
     */
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): L =
        p.valueAsString.split(separator).map(contentMapper::fromString).map()

    /**
     * Converts this into the wrapper type [L].
     */
    abstract fun List<T>.map(): L
}