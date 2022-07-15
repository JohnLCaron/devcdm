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
description = "The CDM (next generation) grid module."

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api(project(":array"))
    api(project(":core"))
    api(project(":dataset"))

    implementation(libs.guava)
    compileOnly("org.jetbrains:annotations:23.0.0")
    implementation(libs.slf4j)
    implementation(libs.uomImpl)

    testImplementation(project(":test-core"))
    testImplementation(project(":test-dataset"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation(libs.truth)
    testImplementation(libs.truthJava8Extension)
    testRuntimeOnly(project(":grib"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(mapOf(
            "Main-Class" to "dev.ucdm.grid.main",
            "Implementation-Title" to "UCDM (next generation) grid library",
            "Implementation-Version" to project.version))
    }
    archiveBaseName.set("ucdm-grid")
}