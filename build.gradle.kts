plugins {
    java
    kotlin("jvm") version "1.4.31"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    application
}

group = "ai.pieper"
version = "1.0"

application {
    mainClass.set("ai.pieper.MainKt")
    mainClassName = "ai.pieper.MainKt"  // needed for current ShadowJar - can be removed later
}

repositories {
    mavenCentral()
    maven(url="https://kotlin.bintray.com/kotlinx")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-cli-jvm:0.3.2")
    implementation("io.ktor:ktor-client-core:1.5.2")
    implementation("io.ktor:ktor-client-cio:1.5.2")
}
