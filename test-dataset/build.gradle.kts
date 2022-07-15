// bug in IntelliJ in which `libs` shows up as not being accessible
// see https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION")

plugins {
    kotlin("jvm") version "1.6.21"
    id("java")
    id("java-library")
}

group = "dev.cdm"
version = "1.0-SNAPSHOT"
description = "A collection of reusable classes to be used internally for testing."

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api(project(":array"))
    api(project(":core"))
    api(project(":dataset"))

    implementation(project(":test-core"))
    implementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    implementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    implementation("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    implementation(libs.slf4j)
    implementation(libs.truth)
    implementation(libs.truthJava8Extension)
    implementation(libs.uomImpl)

    testRuntimeOnly(libs.logbackClassic)
}