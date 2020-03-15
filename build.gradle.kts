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

plugins {
    id("com.github.johnrengelman.shadow") version "5.2.0"
    application
    kotlin("jvm") version "1.3.70"
}

group = "com.github.seliba"
version = "2.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://kotlin.bintray.com/kotlinx")
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    // Scripting Support (For bot owner eval)
    runtimeOnly(kotlin("scripting-jsr223"))

    // Coroutines
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.3.4")

    // Logging
    implementation("io.github.microutils", "kotlin-logging", "1.7.8")
    implementation("org.slf4j", "slf4j-api", "2.0.0alpha1")
    implementation("ch.qos.logback", "logback-classic", "1.3.0-alpha5")
    implementation("io.sentry", "sentry", "1.7.30")
    implementation("io.sentry", "sentry-logback", "1.7.30")

    // Database
    implementation("org.jetbrains.exposed", "exposed-core", "0.21.1")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.21.1")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.21.1")
    implementation("org.jetbrains.exposed", "exposed-java-time", "0.21.1")
    implementation("org.postgresql", "postgresql", "42.2.10")
    implementation("com.zaxxer", "HikariCP", "3.4.2")

    // Discord
    implementation("net.dv8tion", "JDA", "4.1.1_113") {
        exclude(module = "opus-java")
    }

    // Util
    implementation("io.github.cdimascio", "java-dotenv", "5.1.4")
    implementation("com.squareup.okhttp3", "okhttp", "4.4.0")
    implementation("org.jetbrains.kotlinx", "kotlinx-cli", "0.2.1")
    implementation("com.codewaves.codehighlight", "codehighlight", "1.0.2")


    // Testing
    testImplementation("org.mockito", "mockito-core", "3.3.3")
    testImplementation("com.nhaarman.mockitokotlin2", "mockito-kotlin", "2.2.0")
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.6.0")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.6.0")

}

application {
    mainClassName = "com.github.seliba.devcordbot.LauncherKt"
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "12"
            freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
        }
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = "12"
    }

    jar {
        archiveClassifier.value = "original"
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
