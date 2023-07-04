/*
 * Copyright 2021 Daniel Scherf & Michael Rittmeister & Julian König
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
    kotlin("jvm") version "1.8.22"
    kotlin("plugin.serialization") version "1.8.22"
    id("org.jlleitschuh.gradle.ktlint") version "11.5.0"
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
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", "1.5.2")

    // Logging
    implementation("io.github.microutils", "kotlin-logging", "2.0.11")
    implementation("org.slf4j", "slf4j-api", "2.0.0alpha5")
    implementation("ch.qos.logback", "logback-classic", "1.4.7")
    implementation(platform("io.sentry:sentry-bom:6.24.0"))
    implementation(platform("io.sentry:sentry-bom:6.24.0"))
    implementation("io.sentry", "sentry")
    implementation("io.sentry", "sentry-logback")

    // Database
    implementation(platform("org.jetbrains.exposed:exposed-bom:0.41.1"))
    implementation("org.jetbrains.exposed", "exposed-core")
    implementation("org.jetbrains.exposed", "exposed-dao")
    implementation("org.jetbrains.exposed", "exposed-jdbc")
    implementation("org.jetbrains.exposed", "exposed-java-time")
    implementation("org.postgresql", "postgresql", "42.3.1")
    implementation("com.zaxxer", "HikariCP", "5.0.0")

    // Discord
    implementation("dev.kord", "kord-core", "0.8.0-M7")
    implementation("dev.kord.x", "emoji", "0.5.0")

    // Util
    implementation("io.github.cdimascio", "dotenv-kotlin", "6.2.2")
    implementation("org.jetbrains.kotlinx", "kotlinx-cli", "0.3.3")
    implementation("com.google.apis", "google-api-services-customsearch", "v1-rev20210918-1.32.1")
    implementation("net.sf.trove4j", "trove4j", "3.0.3")
    implementation("ru.homyakin", "iuliia-java", "1.7")
    implementation("net.gcardone.junidecode", "junidecode", "0.4.1")

    // Http
    implementation(platform("io.ktor:ktor-bom:1.6.4"))
    implementation("io.ktor", "ktor-client")
    implementation("io.ktor", "ktor-client-okhttp")
    implementation("io.ktor", "ktor-client-serialization-jvm")
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.3.0")

    // Config
    implementation("com.github.uchuhimo.konf", "konf", "master-SNAPSHOT")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.13.+")

    // Testing
    testImplementation("org.mockito", "mockito-core", "4.0.0")
    testImplementation("com.nhaarman.mockitokotlin2", "mockito-kotlin", "2.2.0")
    testImplementation(platform("org.junit:junit-bom:5.9.3"))
    testImplementation("org.junit.jupiter", "junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine")
}

application {
    mainClass.set("com.github.devcordde.devcordbot.LauncherKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs =
                freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn" + "-Xopt-in=dev.kord.common.annotation.KordPreview" + "-Xopt-in=dev.kord.common.annotation.KordExperimental" + "-Xopt-in=kotlin.time.ExperimentalTime" + "-Xopt-in=kotlin.contracts.ExperimentalContracts"
        }
    }

    test {
        useJUnitPlatform()
    }

    installDist {
        destinationDir = buildDir.resolve("libs/install")
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    disabledRules.set(setOf("no-wildcard-imports"))
}

/**
 * Represents the mutable value of a [Property].
 */
var <T> Property<T>.value: T?
    get() = orNull
    set(value) = set(value)
