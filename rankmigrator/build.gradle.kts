plugins {
    java
}

group = "com.github.seliba"
version = "2.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://kotlin.bintray.com/kotlinx")
}

dependencies {
    implementation(project(":"))

    // Logging
    implementation("org.slf4j", "slf4j-api", "2.0.0-alpha1")
    implementation("org.slf4j", "slf4j-simple", "2.0.0-alpha1")

    // Database
    implementation("org.jetbrains.exposed", "exposed-core", "0.21.1")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.21.1")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.21.1")
    implementation("org.jetbrains.exposed", "exposed-java-time", "0.21.1")
    implementation("org.postgresql", "postgresql", "42.2.10")

    // util

    implementation("io.github.cdimascio", "java-dotenv", "5.1.3")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_12
}