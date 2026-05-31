import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.mavenPublish)
}

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

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    jvm()
    
    js {
        browser()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    androidLibrary {
       namespace = "top.ghhccghk.multiplatform.kugouapi.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.okio) // 添加 Okio 用于 Base64 和 压缩处理
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.slf4j.simple)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

// ── Maven Publishing ─────────────────────────────────────────
// Uses vanniktech/gradle-maven-publish-plugin
// POM metadata (group, version, etc.) is in gradle.properties
//
// Publish locally:     ./gradlew :shared:publishToMavenLocal
// Publish to Central:  ./gradlew :shared:publishAndReleaseToMavenCentral
mavenPublishing {

    pom {
        developers {
            developer {
                id.set("ghhccghk")
                name.set("李太白")
                email.set("2137610394@qq.com")
                organization.set("Independent Developer")
                organizationUrl.set("https://github.com/ghhccghk")
            }
        }
    }

    publishToMavenCentral()
    signAllPublications()
}
