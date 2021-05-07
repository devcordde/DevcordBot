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
    kotlin("jvm") version "1.5.0"
    kotlin("plugin.serialization") version "1.5.0"
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
}

group = "com.github.devcord.devcordbot"
version = "2.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://jitpack.io")
    maven("https://schlaubi.jfrog.io/artifactory/forp")
}

dependencies {
    // Kotlin
    implementation(kotlin("reflect"))

    // Scripting Support (For bot owner eval)
    runtimeOnly(kotlin("scripting-jsr223"))

    // Coroutines
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", "1.5.0-RC")

    // Logging
    implementation("io.github.microutils", "kotlin-logging", "1.12.5")
    implementation("org.slf4j", "slf4j-api", "2.0.0alpha1")
    implementation("ch.qos.logback", "logback-classic", "1.3.0-alpha5")
    implementation("io.sentry", "sentry", "4.3.0")
    implementation("io.sentry", "sentry-logback", "4.3.0")

    // Database
    implementation("org.jetbrains.exposed", "exposed-core", "0.30.2")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.30.2")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.30.2")
    implementation("org.jetbrains.exposed", "exposed-java-time", "0.30.2")
    implementation("org.postgresql", "postgresql", "42.2.19")
    implementation("com.zaxxer", "HikariCP", "4.0.3")

    // Discord
    implementation("dev.kord", "kord-core", "kotlin-1.5-20210505.195343-2") {
        version {
            strictly("kotlin-1.5-20210505.195343-2")
        }
    }

    // Util
    implementation("io.github.cdimascio", "java-dotenv", "5.2.2")
    implementation("org.jetbrains.kotlinx", "kotlinx-cli", "0.2.1")
    implementation("com.google.apis", "google-api-services-customsearch", "v1-rev20200917-1.31.0")

    // Http
    implementation(platform("io.ktor:ktor-bom:1.5.3"))
    implementation("io.ktor", "ktor-client")
    implementation("io.ktor", "ktor-client-okhttp")
    implementation("io.ktor", "ktor-client-serialization-jvm") {
        exclude("org.jetbrains.kotlinx", "kotlinx-serialization-json")
    }
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.0.0") {
        version {
            strictly("1.0.0")
        }
    }

    // Config
    implementation("com.github.uchuhimo.konf", "konf", "master-SNAPSHOT")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.12.+")

    // Autohelp
    implementation("me.schlaubi.autohelp", "kord", "1.2.0")
    implementation("dev.schlaubi.forp", "forp-analyze-client", "1.0-SNAPSHOT")
    implementation("com.vladsch.flexmark", "flexmark-html2md-converter", "0.60.2")

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
            jvmTarget = "16"
            freeCompilerArgs =
                freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn" + "-Xopt-in=dev.kord.common.annotation.KordPreview" + "-Xopt-in=dev.kord.common.annotation.KordExperimental"
        }
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        disabledRules.set(setOf("no-wildcard-imports"))
    }

    test {
        useJUnitPlatform()
    }

    installDist {
        destinationDir = buildDir.resolve("libs/install")
    }
}

/**
 * Represents the mutable value of a [Property].
 */
var <T> Property<T>.value: T?
    get() = orNull
    set(value) = set(value)
