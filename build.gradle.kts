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

plugins {
    application
    kotlin("jvm") version "1.4.32"
}

group = "com.github.devcord.devcordbot"
version = "2.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://m2.dv8tion.net/releases")
    maven("https://jitpack.io")
}

dependencies {
    // Kotlin
    implementation(kotlin("reflect"))

    // Scripting Support (For bot owner eval)
    runtimeOnly(kotlin("scripting-jsr223"))

    // Coroutines
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", "1.4.3")

    // Logging
    implementation("io.github.microutils", "kotlin-logging", "2.0.4")
    implementation("org.slf4j", "slf4j-api", "2.0.0alpha1")
    implementation("ch.qos.logback", "logback-classic", "1.3.0-alpha5")
    implementation("io.sentry", "sentry", "4.2.0")
    implementation("io.sentry", "sentry-logback", "4.2.0")

    // Database
    implementation("org.jetbrains.exposed", "exposed-core", "0.29.1")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.29.1")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.29.1")
    implementation("org.jetbrains.exposed", "exposed-java-time", "0.29.1")
    implementation("org.postgresql", "postgresql", "42.2.19")
    implementation("com.zaxxer", "HikariCP", "4.0.3")

    // Discord
    implementation("com.github.dv8fromtheworld", "jda", "e3d2bd7398")

    // Util
    implementation("io.github.cdimascio", "java-dotenv", "5.2.2")
    implementation("com.squareup.okhttp3", "okhttp", "4.9.1")
    implementation("org.jetbrains.kotlinx", "kotlinx-cli", "0.2.1")
    implementation("com.codewaves.codehighlight", "codehighlight", "1.0.2")
    implementation("com.github.johnnyjayjay", "javadox", "adb3613484")
    implementation("com.vladsch.flexmark", "flexmark-html2md-converter", "0.62.2")
    implementation("com.google.apis", "google-api-services-customsearch", "v1-rev20200408-1.30.9")
    implementation("com.google.cloud", "google-cloud-vision", "1.101.1")


    // Testing
    testImplementation("org.mockito", "mockito-core", "3.8.0")
    testImplementation("com.nhaarman.mockitokotlin2", "mockito-kotlin", "2.2.0")
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.7.1")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.7.1")

}

application {
    mainClass.set("com.github.devcordde.devcordbot.LauncherKt")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "14"
            useIR = true
        }
    }

    test {
        useJUnitPlatform()
    }
}

/**
 * Represents the mutable value of a [Property].
 */
var <T> Property<T>.value: T?
    get() = orNull
    set(value) = set(value)
