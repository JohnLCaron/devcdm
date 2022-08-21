import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

// bug in IntelliJ in which `libs` shows up as not being accessible
// see https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION")

plugins {
    kotlin("jvm") version "1.7.10"
    id("java")
    id("java-library")
    alias(libs.plugins.protobufPlugin)
    alias(libs.plugins.execforkPlugin)
}

group = "dev.cdm"
version = "1.0-SNAPSHOT"
description = "The CDM (next generation) GRIB module."

repositories {
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    mavenCentral()
    mavenLocal()
    exclusiveContent {
        forRepository {
            maven {
                url = uri("https://artifacts.unidata.ucar.edu/repository/unidata-releases/")
            }
        }
        filter {
            includeModule("edu.ucar", "jj2000")
        }
    }
}

dependencies {
    api(project(":array"))
    api(project(":core"))
    api(project(":dataset"))
    api(project(":grid"))
    api(platform(libs.protobufBom))
    compileOnly("org.jetbrains:annotations:23.0.0")

    implementation(libs.guava)
    implementation(libs.jdom2)
    implementation(libs.jj2000)
    implementation(libs.protobufJava)
    implementation(libs.slf4j)

    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.15")
    implementation(kotlin("stdlib-common", "1.6.20"))
    implementation(kotlin("stdlib", "1.6.20"))
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")

    testImplementation(project(":test-core"))
    testImplementation(project(":test-dataset"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation(libs.mockk)
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
            "Main-Class" to "dev.ucdm.grib.main",
            "Implementation-Title" to "UCDM (next generation) GRIB library",
            "Implementation-Version" to project.version))
    }
    archiveBaseName.set("ucdm-grib")
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
}

// handle proto generated source and class files
sourceSets {
    main {
        java {
            srcDir("build/generated/source/proto/main/java")
        }
    }
}