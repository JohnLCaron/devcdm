repositories {
    gradlePluginPortal()
}
// libs.versions will show an error in IntelliJ, but it does not impact the ability to import
// or build the project (false positive)
// https://youtrack.jetbrains.com/issue/KTIJ-19370
dependencies {
    implementation("de.jjohannes.gradle:extra-java-module-info:0.14")
}

plugins {
    `kotlin-dsl`
}
