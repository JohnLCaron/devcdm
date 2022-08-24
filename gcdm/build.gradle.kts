import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

// bug in IntelliJ in which `libs` shows up as not being accessible
// see https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION")

plugins {
    `ucdm-library-convention`
    id("application")
    alias(libs.plugins.protobufPlugin)
    alias(libs.plugins.execforkPlugin)
}

description = "gRPC client and server implementation of CDM Remote Procedure Calls (gCDM)."

dependencies {
    api(project(":array"))
    api(project(":core"))
    api(project(":dataset"))
    api(project(":grid"))
    api(project(":grib"))
    compileOnly(libs.tomcatAnnotationsApi)
    compileOnly("org.jetbrains:annotations:23.0.0")

    implementation(platform(libs.grpcBom))
    implementation(libs.grpcProtobuf)
    implementation(libs.grpcStub)
    implementation(libs.guava)
    implementation(libs.jj2000)
    implementation(libs.protobufJava)
    implementation(libs.slf4j)
    implementation(libs.uomImpl)

    runtimeOnly(libs.grpcNettyShaded)
    runtimeOnly(libs.slf4jJdk14)

    testImplementation(project(":test-core"))
    testImplementation(project(":test-dataset"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation(libs.truth)
    testImplementation(libs.truthJava8Extension)
    testImplementation(libs.logbackClassic)
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
            "Main-Class" to "dev.ucdm.gcdm.GcdmServer",
            "Implementation-Title" to "UCDM Remote Procedure Calls",
            "Implementation-Version" to project.version))
    }
    archiveBaseName.set("ucdm-gcdm")
}

application {
    mainClass.set("dev.ucdm.gcdm.GcdmServer")
}

val startDaemonTask = tasks.register<com.github.psxpaul.task.JavaExecFork>("startDaemon") {
    classpath = sourceSets.main.get().runtimeClasspath
    main = "ucar.gcdm.server.GcdmServer"
    jvmArgs = listOf("-Xmx512m", "-Djava.awt.headless=true")
    standardOutput = "$buildDir/gcdm_logs/gcdm.log"
    errorOutput = "$buildDir/gcdm_logs/gcdm-error.log"
    stopAfter = tasks.test.get()
    waitForPort = 16111
    waitForOutput = "Server started, listening on 16111"
}
