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

@file:Suppress("FunctionName")

package com.github.devcordde.devcordbot.commands.general

import com.github.johnnyjayjay.javadox.JavadocParser
import com.github.johnnyjayjay.javadox.Javadocs
import com.github.devcordde.devcordbot.command.AbstractCommand
import com.github.devcordde.devcordbot.command.context.Context
import org.jsoup.Jsoup

private fun URLJavaDocCommand(url: String, aliases: List<String>, displayName: String, description: String) =
    object : AbstractJavadocCommand() {
        override val aliases: List<String> = aliases
        override val displayName: String = displayName
        override val description: String = description

        private val parser: JavadocParser = JavadocParser(htmlRenderer::convert)

        private val docs: Javadocs = Javadocs(tree = url, parser = parser) {
            Jsoup.connect(it).userAgent("Mozilla").get()
        }

        override suspend fun execute(context: Context) = execute(context, url, docs)
    }

/**
 * Command for oracle (java 10) doc.
 */
fun OracleJavaDocCommand(): AbstractCommand = URLJavaDocCommand(
    DocumentedVersion.V_10.url,
    listOf("doc", "docs"),
    "javaodc",
    "Lässt dich javadoc benutzen"
)

/**
 * Command for spigot (1.15.2) doc.
 */
fun SpigotJavaDocCommand(): AbstractCommand = URLJavaDocCommand(
    DocumentedVersion.V_1_16.url,
    listOf("spigot", "1.16", "116", "sdoc"),
    "javaodc",
    "Lässt dich javadoc benutzen"
)

/**
 * Command for spigot (1.8.8) doc.
 * Because some YT tutorial guy had to make videos for an 5 year old version
 */
fun SpigotLegacyJavaDocCommand(): AbstractCommand = URLJavaDocCommand(
    DocumentedVersion.V_1_8_8.url,
    listOf("spigotlegacy", "1.8", "118", "sldoc"),
    "javaodc",
    "Lässt dich javadoc benutzen"
)
