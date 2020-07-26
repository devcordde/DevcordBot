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

package com.github.devcordde.devcordbot.core.autohelp

import com.github.devcordde.devcordbot.core.JavaDocManager
import com.github.devcordde.devcordbot.util.Googler
import com.github.johnnyjayjay.javadox.DocumentedType
import com.github.johnnyjayjay.javadox.Javadocs

class JavaDocFinder(private val googler: Googler) {

    fun findJavadocForClass(clazz: String): DocumentedType? {
        val index = clazz.lastIndexOf('.')
        val pakage = clazz.take(index)
        val className = clazz.drop(index + 1)
        val identifier = if (pakage.startsWith("java")) {
            "java"
        } else {
            pakage.take(pakage.indexOf('.', pakage.indexOf('.') + 1))
        }

        val javadoc = JavaDocManager.javadocPool[identifier] ?: googleJavadoc(className)
        return javadoc?.find(pakage, className)?.firstOrNull()

    }

    private fun googleJavadoc(clazz: String): Javadocs? {
        val query = "$clazz javadoc"
        val rawUrl = googler.google(query)
            .firstOrNull { it.htmlTitle.contains("API", ignoreCase = true); true }?.formattedUrl
        return rawUrl?.let {
            val pakage = clazz.take(clazz.indexOf('.'))
            val url = rawUrl.take(rawUrl.indexOf(pakage))
            JavaDocManager.makeJavadoc(url)
        }
    }
}
