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
    kotlin("jvm") version "1.3.61"
}

group = "com.github.seliba"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.3.3")

    implementation("io.github.microutils", "kotlin-logging", "1.7.7")
    implementation("org.slf4j", "slf4j-api", "1.7.30")
    implementation("ch.qos.logback", "logback-classic", "1.2.3")
//    implementation("io.sentry", "sentry", "1.7.27")
//    implementation("io.sentry", "sentry-logback", "1.7.27")

    implementation("org.jetbrains.exposed", "exposed-core", "0.20.1")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.20.1")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.20.1")
    implementation("org.postgresql", "postgresql", "42.2.9")
    implementation("com.zaxxer", "HikariCP", "3.4.2")

    implementation("net.dv8tion", "JDA", "4.1.1_105") {
        exclude(module = "opus-java")
    }

    implementation("io.github.cdimascio", "java-dotenv", "5.1.3")

    testImplementation("org.mockito", "mockito-core", "3.2.4")
    testImplementation("com.nhaarman.mockitokotlin2", "mockito-kotlin", "2.2.0")
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.3.1")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.3.1")

}

application {
    mainClassName = "com.github.seliba.devcordbot.LauncherKt"
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "12"
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
