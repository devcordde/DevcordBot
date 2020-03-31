/*
 * Copyright 2020 Daniel Scherf & Michael Rittmeister
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

package com.github.seliba.devcordbot.commands.general

import com.github.seliba.devcordbot.command.AbstractCommand

private fun URLJavaDocCommand(url: String, aliases: List<String>, displayName: String, description: String) =
    object : JavadocCommand(url) {
        override val aliases: List<String> = aliases
        override val displayName: String = displayName
        override val description: String = description
    }

/**
 * Command for oracle (java 10) doc.
 */
fun OracleJavaDocCommand(): AbstractCommand = URLJavaDocCommand(
    "https://docs.oracle.com/javase/10/docs/api/allclasses-noframe.html",
    listOf("javadoc", "jdoc", "doc", "docs"),
    "javaodc",
    "Lässt dich javadoc benutzen"
)

/**
 * Command for spigot (1.15.2) doc.
 */
fun SpigotJavaDocCommand(): AbstractCommand = URLJavaDocCommand(
    "https://hub.spigotmc.org/javadocs/spigot/allclasses-noframe.html",
    listOf("spigot", "1.15", "115", "sdoc"),
    "javaodc",
    "Lässt dich javadoc benutzen"
)

/**
 * Command for spigot (1.8.8) doc.
 * Because some YT tutorial guy had to make videos for an 5 year old version
 */
fun SpigotLegacyJavaDocCommand(): AbstractCommand = URLJavaDocCommand(
    "https://helpch.at/docs/1.8.8/allclasses-noframe.html",
    listOf("spigot", "1.15", "115", "sdoc"),
    "javaodc",
    "Lässt dich javadoc benutzen"
)
