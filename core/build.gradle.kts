// bug in IntelliJ in which `libs` shows up as not being accessible
// see https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION")

plugins {
    id("java")
    id("java-library")
}

group = "dev.cdm"
version = "1.0-SNAPSHOT"
description = "The CDM (next generation) core module."

repositories {
    mavenCentral()
}

dependencies {
    api(project(":array"))
    compileOnly("org.jetbrains:annotations:23.0.0")

    implementation(libs.guava)
    implementation(libs.jdom2)
    implementation(libs.slf4j)

    testImplementation(project(":test-core"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation(libs.truth)
    testImplementation(libs.truthJava8Extension)
    testImplementation(libs.logbackClassic)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(mapOf(
            "Main-Class" to "dev.ucdm.main",
            "Implementation-Title" to "UCDM (next generation) core library",
            "Implementation-Version" to project.version))
    }
    archiveBaseName.set("ucdm-core")
}