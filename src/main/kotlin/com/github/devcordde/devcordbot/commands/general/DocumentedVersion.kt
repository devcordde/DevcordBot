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

package com.github.devcordde.devcordbot.commands.general

@Suppress("KDocMissingDocumentation")
enum class DocumentedVersion(
    val docType: String, // I tried using an enum for this but DocType.JAVA was always null, dk why
    val url: String,
    val humanName: String
) {

    // JAVA
    V_6("JAVA", "https://docs.oracle.com/javase/6/docs/api/overview-tree.html", "6"),
    V_7("JAVA", "https://docs.oracle.com/javase/7/docs/api/overview-tree.html", "7"),
    V_8("JAVA", "https://docs.oracle.com/javase/8/docs/api/overview-tree.html", "8"),
    V_9("JAVA", "https://docs.oracle.com/javase/9/docs/api/overview-tree.html", "9"),
    V_10("JAVA", "https://docs.oracle.com/javase/10/docs/api/overview-tree.html", "10"),
    V_11("JAVA", "https://docs.oracle.com/en/java/javase/11/docs/api/overview-tree.html", "11"),
    V_12("JAVA", "https://docs.oracle.com/en/java/javase/12/docs/api/overview-tree.html", "12"),
    V_13("JAVA", "https://docs.oracle.com/en/java/javase/13/docs/api/overview-tree.html", "13"),
    V_14("JAVA", "https://download.java.net/java/GA/jdk14/docs/api/overview-tree.html", "14"),

    // Spigot
    V_1_7_10("SPIGOT", "https://helpch.at/docs/1.7.10/overview-tree.html", "1.7.10"),

    V_1_8("SPIGOT", "https://helpch.at/docs/1.8/overview-tree.html", "1.8"),
    V_1_8_1("SPIGOT", "https://helpch.at/docs/1.8.1/overview-tree.html", "1.8.1"),
    V_1_8_2("SPIGOT", "https://helpch.at/docs/1.8.2/overview-tree.html", "1.8.2"),
    V_1_8_3("SPIGOT", "https://helpch.at/docs/1.8.3/overview-tree.html", "1.8.3"),
    V_1_8_4("SPIGOT", "https://helpch.at/docs/1.8.4/overview-tree.html", "1.8.4"),
    V_1_8_5("SPIGOT", "https://helpch.at/docs/1.8.5/overview-tree.html", "1.8.5"),
    V_1_8_6("SPIGOT", "https://helpch.at/docs/1.8.6/overview-tree.html", "1.8.6"),
    V_1_8_7("SPIGOT", "https://helpch.at/docs/1.8.7/overview-tree.html", "1.8.7"),
    V_1_8_8("SPIGOT", "https://helpch.at/docs/1.8.8/overview-tree.html", "1.8.8"),

    V_1_9("SPIGOT", "https://helpch.at/docs/1.9/overview-tree.html", "1.9"),
    V_1_9_2("SPIGOT", "https://helpch.at/docs/1.9.2/overview-tree.html", "1.9.2"),
    V_1_9_4("SPIGOT", "https://helpch.at/docs/1.9.4/overview-tree.html", "1.9.4"),

    V_1_10("SPIGOT", "https://helpch.at/docs/1.10/overview-tree.html", "1.10"),
    V_1_10_2("SPIGOT", "https://helpch.at/docs/1.10.2/overview-tree.html", "1.10.2"),

    V_1_11("SPIGOT", "https://helpch.at/docs/1.11/overview-tree.html", "1.11"),
    V_1_11_1("SPIGOT", "https://helpch.at/docs/1.11.1/overview-tree.html", "1.11.1"),
    V_1_11_2("SPIGOT", "https://helpch.at/docs/1.11.2/overview-tree.html", "1.11.2"),

    V_1_12("SPIGOT", "https://helpch.at/docs/1.12/overview-tree.html", "1.12"),
    V_1_12_1("SPIGOT", "https://helpch.at/docs/1.12.1/overview-tree.html", "1.12.1"),
    V_1_12_2("SPIGOT", "https://helpch.at/docs/1.12.2/overview-tree.html", "1.12.2"),

    V_1_13("SPIGOT", "https://helpch.at/docs/1.13/overview-tree.html", "1.13"),
    V_1_13_1("SPIGOT", "https://helpch.at/docs/1.13.1/overview-tree.html", "1.13.1"),
    V_1_13_2("SPIGOT", "https://helpch.at/docs/1.13.2/overview-tree.html", "1.13.2"),

    V_1_14("SPIGOT", "https://helpch.at/docs/1.14/overview-tree.html", "1.14"),
    V_1_14_1("SPIGOT", "https://helpch.at/docs/1.14.1/overview-tree.html", "1.14.1"),
    V_1_14_2("SPIGOT", "https://helpch.at/docs/1.14.2/overview-tree.html", "1.14.2"),
    V_1_14_3("SPIGOT", "https://helpch.at/docs/1.14.3/overview-tree.html", "1.14.3"),
    V_1_14_4("SPIGOT", "https://helpch.at/docs/1.14.4/overview-tree.html", "1.14.4"),

    V_1_15("SPIGOT", "https://helpch.at/docs/1.15/overview-tree.html", "1.15"),
    V_1_15_1("SPIGOT", "https://helpch.at/docs/1.15.1/overview-tree.html", "1.15.1"),
    V_1_15_2("SPIGOT", "https://helpch.at/docs/1.15.2/overview-tree.html", "1.15.2"),

    V_1_16("SPIGOT", "https://hub.spigotmc.org/javadocs/spigot/overview-tree.html", "1.15.2"),

}
