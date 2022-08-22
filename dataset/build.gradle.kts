plugins {
    `ucdm-library-convention`
    kotlin("jvm") version "1.7.10"
}

description = "The CDM (next generation) coordinate system module."

dependencies {
    api(project(":array"))
    api(project(":core"))
    compileOnly("org.jetbrains:annotations:23.0.0")

    implementation(libs.guava)
    implementation(libs.jdom2)
    implementation(libs.slf4j)
    implementation(libs.uomImpl)

    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.15")
    implementation(kotlin("stdlib-common", "1.6.20"))
    implementation(kotlin("stdlib", "1.6.20"))
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")

    testImplementation(project(":test-core"))
    testImplementation(project(":test-dataset"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testImplementation(libs.truth)
    testImplementation(libs.truthJava8Extension)
    testImplementation(libs.logbackClassic)

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testRuntimeOnly(project(":grib"))
    testRuntimeOnly(project(":grid"))
}

tasks.jar {
    manifest {
        attributes(mapOf(
            "Main-Class" to "dev.ucdm.dataset.main",
            "Implementation-Title" to "CDM (next generation) dataset library",
            "Implementation-Version" to project.version))
    }
    archiveBaseName.set("ucdm-dataset")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}