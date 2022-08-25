plugins {
    `ucdm-internal-convention`
    kotlin("jvm") version "1.7.10"
}

description = "A collection of reusable classes to be used internally for testing."

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":array"))
    implementation(project(":core"))

    implementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    implementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    implementation("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    implementation(libs.slf4j)
    implementation(libs.truth)
    implementation(libs.truthJava8Extension)

    testRuntimeOnly(libs.logbackClassic)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}