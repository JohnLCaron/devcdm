import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

// bug in IntelliJ in which `libs` shows up as not being accessible
// see https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION")

plugins {
    id("java")
    id("java-library")
    alias(libs.plugins.protobufPlugin)
    alias(libs.plugins.execforkPlugin)
}

group = "dev.cdm"
version = "1.0-SNAPSHOT"
description = "gRPC client and server implementation of CDM Remote Procedure Calls (gCDM)."

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api(project(":array"))
    api(project(":core"))
    api(project(":dataset"))
    api(project(":grid"))

    compileOnly("org.jetbrains:annotations:23.0.0")
    implementation(platform(libs.grpcBom))
    implementation(libs.grpcProtobuf)
    implementation(libs.grpcStub)
    implementation(libs.guava)
    implementation(libs.protobufJava)
    implementation(libs.slf4j)
    compileOnly(libs.tomcatAnnotationsApi)
    implementation(libs.uomImpl)

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation(libs.truth)
    testImplementation(libs.truthJava8Extension)
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}

// handle proto generated source and class files
sourceSets {
    main {
        java {
            srcDir("build/generated/source/proto/main/java")
            srcDir("build/generated/source/proto/main/grpc")
        }
    }
}

tasks.jar {
    manifest {
        attributes(mapOf(
            "Main-Class" to "dev.cdm.gcdm",
            "Implementation-Title" to "UCDM (next generation) remote access library",
            "Implementation-Version" to project.version))
    }
    archiveBaseName.set("ucdm-gcdm")
}