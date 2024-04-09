plugins {
    kotlin("jvm") version "1.9.22"
}

group = "com.github.tnoalex"
version = "1.0-SNAPSHOT"

repositories {
    flatDir {
        dirs("./lib")
    }
    mavenCentral()
}

dependencies {
    implementation(group = "com.github.gumtreediff", version = "4.0.0", name = "gumtree")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}