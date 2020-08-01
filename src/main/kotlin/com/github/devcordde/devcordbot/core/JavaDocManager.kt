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
import mu.KotlinLogging
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Manager for javadocs.
 */
object JavaDocManager {

    private val LOG = KotlinLogging.logger { }
    private val parser: JavadocParser = JavadocParser(AbstractJavadocCommand.htmlRenderer::convert)

    /**
     * Pool of javadocs
     */
    val javadocPool: Map<String, Javadocs> = mutableMapOf()

    init {
        makeJavadoc("java", DocumentedVersion.V_10.url)
        val spigot = makeJavadoc("org.bukkit", DocumentedVersion.V_1_16.url)
        makeJavadoc("spigot-legacy", DocumentedVersion.V_1_8_8.url)
        if (spigot != null) {
            (javadocPool as MutableMap<String, Javadocs>)["org.spigotmc"] = spigot
        }
    }

    internal fun makeJavadoc(pakage: String, url: String, register: Boolean = true): Javadocs? {
        return try {
            val docs = Javadocs(
                tree = url,
                parser = parser,
                scrape = ::scrape
            )
            if (register) (javadocPool as MutableMap<String, Javadocs>)[pakage] = docs
            docs
        } catch (ignored: IllegalStateException) {
            null
        }
    }

    private fun scrape(url: String) = scrape(url, 1)

    private fun scrape(url: String, tried: Int): Document =
        try {
            Jsoup.connect(url).userAgent("Mozilla").get()
        } catch (e: HttpStatusException) {
            LOG.warn(e) { "Could not fetch $url doc trying again" }
            if (tried >= 5) {
                throw IllegalStateException("Could not fetch doc!", e)
            }
            scrape(url, tried + 1)
        }
}
