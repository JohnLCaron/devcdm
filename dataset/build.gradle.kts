// bug in IntelliJ in which `libs` shows up as not being accessible
// see https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION")

plugins {
    id("java")
    id("java-library")
    id("de.jjohannes.extra-java-module-info")
}

group = "dev.cdm"
version = "1.0-SNAPSHOT"
description = "The CDM (next generation) coordinate system module."

repositories {
    mavenCentral()
}

dependencies {
    api(project(":array"))
    api(project(":core"))

    implementation(libs.guava)
    implementation(libs.jdom2)
    compileOnly("org.jetbrains:annotations:23.0.0")
    implementation(libs.slf4j)
    implementation(libs.uomImpl)
    implementation(libs.javaxInject)

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation(libs.truth)
    testImplementation(libs.truthJava8Extension)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(mapOf(
            "Main-Class" to "dev.cdm.main",
            "Implementation-Title" to "CDM (next generation) core library",
            "Implementation-Version" to project.version))
    }
    archiveBaseName.set("cdmng-dataset")
}

extraJavaModuleInfo {
    module("javax.inject-1.jar", "javax.inject", "1") {
        exports("javax.inject")
    }
    // apparently have to add automaticModules, even though these were working fine
    automaticModule("com.google.guava", "guava")

}