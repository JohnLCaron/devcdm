plugins {
    `ucdm-library-convention`
}

description = "The Common Data Model (next generation) AWS S3 support."

dependencies {
    api(project(":array"))
    api(project(":core"))

    implementation(libs.guava)
    compileOnly("org.jetbrains:annotations:23.0.0")
    implementation(libs.slf4j)

    api(platform(libs.awsSdkBom))
    implementation(libs.awsApacheClient)
    implementation(libs.awsS3Sdk) {
        // exclude netty nio client due to open CVEs. See
        // https://github.com/aws/aws-sdk-java-v2/issues/1632
        // we don't use the nio http client in our S3 related code,
        // so we should be ok here (others may need to add it specifically to
        // their code if they are using our S3 stuff, but then it's their
        // explicit decision to run it).
        exclude(group = "software.amazon.awssdk", module = "netty-nio-client")
    }

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
            "Main-Class" to "dev.ucdm.aws.main",
            "Implementation-Title" to "UCDM (next generation) AWS S3 library",
            "Implementation-Version" to project.version))
    }
    archiveBaseName.set("ucdm-aws")
}