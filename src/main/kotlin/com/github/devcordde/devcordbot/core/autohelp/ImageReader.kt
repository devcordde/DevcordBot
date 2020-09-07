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

import com.google.cloud.vision.v1.AnnotateImageRequest
import com.google.cloud.vision.v1.Feature
import com.google.cloud.vision.v1.Image
import com.google.cloud.vision.v1.ImageAnnotatorClient
import com.google.protobuf.ByteString
import mu.KotlinLogging
import net.dv8tion.jda.api.utils.data.DataObject
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.Instant


private val LOG = KotlinLogging.logger { }

/**
 * Utility to read content of images.
 */
object ImageReader {

    /**
     * Whether ratelimit has been hit or not
     */
    val available: Boolean
        get() = Ratelimiter.isAvailable && client != null
    private val client = runCatching {
        ImageAnnotatorClient.create()
    }.getOrNull()

    /**
     * Reads the image from [inputStream].
     */
    fun readImage(inputStream: InputStream): String? = try {
        sendRequest(inputStream)
    } catch (e: Exception) {
        LOG.error(e) { "Could not read image" }
        null
    }

    private fun sendRequest(inputStream: InputStream): String? {
        if (!Ratelimiter.isAvailable || client == null) return null
        Ratelimiter.register()
        val imageBytes = ByteString.readFrom(inputStream)
        val image = Image.newBuilder().setContent(imageBytes).build()
        val findTextFeature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build()
        val request = AnnotateImageRequest.newBuilder().addFeatures(findTextFeature).setImage(image).build()
        val response = client.batchAnnotateImages(listOf(request)).responsesList.first()
        if (response.hasError()) {
            LOG.error { "Could not read image: ${response.error.message}" }
            return null
        }
        return response.textAnnotationsList.firstOrNull()?.description
    }
}

private object Ratelimiter {

    private const val MAX_REQUESTS = 1000
    private val file = Path.of("ocr_usages.json")
    private var stats: Stats =
        Stats(Long.MAX_VALUE, Instant.MAX) // initialize with to high value before reading real data
        set(value) {
            field = value
            stats.save()
        }

    val isAvailable: Boolean
        get() {
            return if (Duration.between(Instant.now(), stats.lastUpdate) < Duration.ofDays(31).negated()) {
                stats = Stats(0, Instant.now())
                true
            } else {
                stats.usages < MAX_REQUESTS
            }
        }

    init {
        stats = if (Files.exists(file)) {
            val bytes = Files.readAllBytes(file)
            Stats.fromJson(bytes)
        } else {
            Files.createFile(file)
            val newStats = Stats(0, Instant.now())
            newStats.save()
            newStats // return
        }
    }

    fun register() {
        stats = stats.copy(usages = stats.usages + 1)
        stats.save()
    }

    private fun Stats.save() {
        FileChannel.open(file, StandardOpenOption.WRITE).use {
            it.write(ByteBuffer.wrap(toJson()))
        }
    }

    private data class Stats(val usages: Long, val lastUpdate: Instant) {
        fun toJson(): ByteArray = DataObject.empty()
            .put("usages", usages)
            .put("last_update", lastUpdate.toEpochMilli())
            .toJson()

        companion object {
            fun fromJson(bytes: ByteArray): Stats {
                val json = DataObject.fromJson(bytes)
                return Stats(json.getLong("usages"), Instant.ofEpochMilli(json.getLong("last_update")))
            }
        }
    }
}
