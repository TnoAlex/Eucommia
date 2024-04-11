plugins {
    kotlin("jvm") version "1.9.22"
}

group = "com.github.tnoalex"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("com.github.gumtreediff:core:4.0.0-beta2-SNAPSHOT")
    implementation("com.github.gumtreediff:client:4.0.0-beta2-SNAPSHOT")
    implementation("com.github.gumtreediff:gen.treesitter-ng:4.0.0-beta2-SNAPSHOT")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}