// bug in IntelliJ in which `libs` shows up as not being accessible
// see https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION")

plugins {
    id("java")
    id("maven-publish")
}

group = "dev.cdm"
version = "1.0-SNAPSHOT"
description = "The CDM (next generation) array module."

repositories {
    mavenCentral()
}

java {
    modularity.inferModulePath.set(true)
}

dependencies {
    compileOnly("org.jetbrains:annotations:23.0.0")

    implementation(libs.guava)
    implementation(libs.slf4j)

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
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
            "Implementation-Title" to "UCDM (next generation) array library",
            "Implementation-Version" to project.version))
    }
    archiveBaseName.set("ucdm-array")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/JohnLCaron/devcdm")
            credentials {
                username = project.findProperty("devcdm.pub.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("devcdm.pub.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}