plugins {
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
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
    implementation("com.github.ajalt.clikt:clikt:4.2.2")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.github.tnoalex.MainKt")
}