plugins {
    kotlin("jvm") version "2.1.20-RC3"
    kotlin("kapt") version "2.1.20-RC3"
}

group = "net.starlexpvp"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}