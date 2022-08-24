plugins {
    id("ucdm-internal-convention")
    `java-library`
    `maven-publish`
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/JohnLCaron/devcdm")
            credentials {
                username = project.findProperty("github.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("github.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}
