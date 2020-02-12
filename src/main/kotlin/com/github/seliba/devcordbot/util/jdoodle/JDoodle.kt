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

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.lang.IllegalArgumentException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


/**
 * JDoodle Api wrapper
 */
object JDoodle {
    private var clientId = ""
    private var clientSecret = ""
    private val url = URL("https://api.jdoodle.com/v1/execute")
    private const val error = "Ein Fehler ist Aufgetreten"

    /**
     * Init the values for execution.
     */
    fun init(clientId: String, clientSecret: String) {
        JDoodle.clientId = clientId
        JDoodle.clientSecret = clientSecret
    }

    /**
     * Executes the given Script in the given Language
     */
    fun execute(lang: String, script: String): String {
        val language: Language

        try {
            language = Language.valueOf(lang.toUpperCase())
        } catch (e: IllegalArgumentException) {
            return "Sprache $lang nicht verfügbar."
        }
        try {
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.doOutput = true
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            val input =
                "{\"clientId\": \"$clientId\",\"clientSecret\":\"$clientSecret\",\"script\":\"${script.replace(
                    "\n",
                    "\\n"
                ).replace(
                    " ",
                    ""
                )}\",\"language\":\"${language.langString}\",\"versionIndex\":\"${language.codeInt}\"} "
            println(input)

            connection.outputStream.write(input.toByteArray())
            connection.outputStream.flush()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return "Fehlerhafte Anfrage:\nFehlercode:${connection.responseCode}\nFehlermeldung: ${inputStreamToString(
                    connection.errorStream
                )}}"
            }

            val output =
                inputStreamToString(connection.inputStream)

            connection.disconnect()
            return output
        } catch (e: MalformedURLException) {
            return error
        } catch (e: IOException) {
            return error
        }


    }

    /**
     * Transform an input stream to a string.
     */
    private fun inputStreamToString(inputStream: InputStream): String {
        val reader = BufferedReader(inputStream.reader())
        val content = StringBuilder()
        reader.use {
            var line = it.readLine()
            while (line != null) {
                content.append(line)
                line = it.readLine()
            }
            return content.toString()
        }
    }
}