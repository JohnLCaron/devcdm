plugins {
    `ucdm-library-convention`
    kotlin("jvm") version "1.7.10"
}

description = "The CDM (next generation) grid module."

dependencies {
    api(project(":array"))
    api(project(":core"))
    api(project(":dataset"))
    compileOnly("org.jetbrains:annotations:23.0.0")

    implementation(libs.guava)
    implementation(libs.slf4j)
    implementation(libs.uomImpl)

    testImplementation(project(":test-core"))
    testImplementation(project(":test-dataset"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testImplementation(libs.truth)
    testImplementation(libs.truthJava8Extension)
    testImplementation(libs.logbackClassic)

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testRuntimeOnly(project(":grib"))
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}