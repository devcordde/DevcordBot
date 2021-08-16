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

package com.github.devcordde.devcordbot.core.autohelp

import com.github.devcordde.devcordbot.database.Tag
import dev.schlaubi.forp.parser.stacktrace.StackTrace
import me.schlaubi.autohelp.tags.TagSupplier
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

/**
 * Implementation of [TagSupplier] acting like the original autohelp.
 */
object DevCordTagSupplier : TagSupplier {
    override fun findTagForException(exception: StackTrace): String? {
        val exceptionName =
            (exception.exception.innerClassName ?: exception.exception.className).lowercase(Locale.getDefault())
        val message = exception.message
        val tag = when {
            exceptionName == "nullpointerexception" -> "nullpointerexception"
            exceptionName == "unsupportedclassversionerror" -> "class-version"
            exceptionName == "classcastexception" -> "casting"
            message == "Plugin already initialized!" -> "plugin-already-initialized"
            exceptionName == "invaliddescriptionexception" -> "plugin.yml"
            exceptionName == "invalidpluginexception" && message?.lowercase(Locale.getDefault())
                ?.contains("cannot find main class") == true -> "main-class-not-found"
            exceptionName == "arrayindexoutofboundsexception" -> "ArrayIndexOutOfBoundsException"
            else -> null
        } ?: return null

        return transaction {
            Tag.findByIdentifier(tag)?.content
        }
    }
}
