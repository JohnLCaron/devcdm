rootProject.name = "devcdm"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
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
    // Only allow dependencies from repositories explicitly listed here
    //repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    // Don't let plugins add repositories - this will make sure we know exactly which external
    // repositories are in use by the project.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
}


include("array")
include("core")
include("dataset")
include("aws")
include("grib")
include("grid")
include("gcdm")
include("test-core")
include("test-dataset")