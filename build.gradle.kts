import java.util.Properties
import kotlin.apply

val localProperties = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}

extra["mavenCentralUsername"] = localProperties.getProperty("mavenCentralUsername")
extra["mavenCentralPassword"] = localProperties.getProperty("mavenCentralPassword")

extra["signing.keyId"] =
    localProperties.getProperty("signing.keyId")

extra["signing.password"] =
    localProperties.getProperty("signing.password")

extra["signing.secretKeyRingFile"] =
    localProperties.getProperty("signing.secretKeyRingFile")


plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}