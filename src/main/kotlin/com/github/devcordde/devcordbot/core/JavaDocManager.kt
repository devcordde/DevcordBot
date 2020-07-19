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

package com.github.devcordde.devcordbot.core

import com.github.devcordde.devcordbot.commands.general.AbstractJavadocCommand
import com.github.devcordde.devcordbot.commands.general.DocumentedVersion
import com.github.johnnyjayjay.javadox.JavadocParser
import com.github.johnnyjayjay.javadox.Javadocs
import org.jsoup.Jsoup

object JavaDocManager {

    private val parser: JavadocParser = JavadocParser(AbstractJavadocCommand.htmlRenderer::convert)

    val javadocPool: Map<String, Javadocs> = mapOf(
        "java" to makeJavadoc(DocumentedVersion.V_10.url),
        "org.bukkit" to makeJavadoc(DocumentedVersion.V_1_16.url),
        "org.spigotmc" to makeJavadoc(DocumentedVersion.V_1_16.url)
    )

    private fun makeJavadoc(url: String) = Javadocs(tree = url, parser = parser) {
        Jsoup.connect(it).userAgent("Mozilla").get()
    }
}