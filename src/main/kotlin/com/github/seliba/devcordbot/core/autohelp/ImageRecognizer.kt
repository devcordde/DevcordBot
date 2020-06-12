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

package com.github.seliba.devcordbot.core.autohelp

import com.google.cloud.vision.v1.AnnotateImageRequest
import com.google.cloud.vision.v1.Feature
import com.google.cloud.vision.v1.Image
import com.google.cloud.vision.v1.ImageAnnotatorClient
import com.google.protobuf.ByteString
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.utils.data.DataObject
import java.io.*
import java.time.Duration
import java.time.Instant

/**
 * Utility to use OCR (Optical Character Recognition).
 */
object ImageRecognizer {

    private const val MAX_LIMIT = 1000 // Free contigent see: https://cloud.google.com/vision/pricing#prices
    private const val FILE_NAME = "vision_usage.json"

    /**
     * Indicates whether the ImageRecognizer is ready or it exceeded it's api.
     */
    val ready: Boolean
        get() = uses < MAX_LIMIT

    private var lastRefresh = Instant.now()

    private val client = ImageAnnotatorClient.create()
    private var uses =
        1001 // Initialize max to 1001 before reading actual number to prevent usage of api after exceeding free contigent

    init {
        val file = File(FILE_NAME)
        val content = if (!file.exists()) {
            file.createNewFile()
            DataObject.empty()
                .put("uses", 0)
                .put("last_refresh", Instant.now().toString())
                .toString()
        } else {
            BufferedReader(FileReader(file)).readText()
        }
        val json = DataObject.fromJson(content)

        var lastRefresh = Instant.parse(json.getString("last_refresh"))
        var uses = json.getInt("uses")
        if (Duration.between(lastRefresh, Instant.now()).abs() > Duration.ofDays(31)) {
            lastRefresh = Instant.now()
            uses = 0
        }
        writeAPIUsage(uses, lastRefresh)
        this.uses = uses
        this.lastRefresh = lastRefresh
        println("read $uses")
        println("read $lastRefresh")
    }

    private fun writeAPIUsage(uses: Int, lastRefresh: Instant = this.lastRefresh) {
        val file = File(FILE_NAME)
        if (!file.exists()) {
            file.createNewFile()
        }
        val json = DataObject.empty()
            .put("last_refresh", lastRefresh.toString())
            .put("uses", uses)
            .toString()
        println("writing $json")
        BufferedWriter(FileWriter(file)).use {
            it.write(json)
        }
    }

    /**
     * Tries to read the text seen in [inputStream].
     */
    fun readImageText(inputStream: InputStream): String {
        require(ready) { "API limit must not be exceeded" }
        uses++
        GlobalScope.launch {
            writeAPIUsage(uses)
        }
        val imageBytes = ByteString.readFrom(inputStream)
        val image = Image.newBuilder().setContent(imageBytes).build()
        val findTextFeature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build()
        val request = AnnotateImageRequest.newBuilder().addFeatures(findTextFeature).setImage(image).build()
        val response = client.batchAnnotateImages(listOf(request)).responsesList.first()
        if (response.hasError()) {
            throw IllegalStateException(response.error.message)
        }
        return response.textAnnotationsList.first().description
    }

    /**
     * Saves API usage and closes client.
     */
    fun close() {
        writeAPIUsage(uses)
        client.close()
    }
}
