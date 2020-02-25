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
    implementation("org.slf4j", "slf4j-api", "2.0.0alpha1")
    implementation("org.slf4j", "slf4j-simple", "2.0.0alpha1")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_12
}