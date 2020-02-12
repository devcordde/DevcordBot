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

package com.github.seliba.devcordbot.util.jdoodle

import io.github.cdimascio.dotenv.dotenv
import io.github.rybalkinsd.kohttp.ext.asString

/**
 * Execute a random JDoodle
 */
fun main() {
    JDoodle.init(dotenv())

    execAndLog(
        Language.PY, "x=10;\ny=25;\nz=x+y;\nprint (\"sum of x+y =\", z);".trimIndent()
    )

}

/**
 * Run script and log. POC for Eval command.
 */
fun execAndLog(language: Language, script: String) {
    val response = JDoodle.execute(
        language, script
    )

    if (response == null) {
        println("Fehler")
        return
    }

    if (response.code() != 200) {
        println("Fehlerhafte Anfrage:\nFehlercode:${response.code()}\nFehlermeldung: ${response.asString()}}")
        return
    }

    println(response.asString().orEmpty())

}
