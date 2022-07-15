rootProject.name = "devcdm"

// allow the use of version_catalogs to handle defining all dependencies and versions
// in one location (e.g. gradle/libs.versions.toml)
enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
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
