import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

val generateVersion = tasks.register("generateVersion") {
    group = "build"
    description = "Generates Version.kt with the version from gradle.properties."
    val appVersion = project.version.toString()
    val outDir = layout.buildDirectory.dir("generated/version/kotlin")
    inputs.property("version", appVersion)
    outputs.dir(outDir)
    doLast {
        outDir.get().file("Version.kt").asFile.apply {
            parentFile.mkdirs()
            writeText("const val VERSION = \"$appVersion\"\n")
        }
    }
}

kotlin {
    macosArm64("native") {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }

    sourceSets {
        nativeMain {
            kotlin.srcDir(generateVersion)
            dependencies {
                implementation(libs.clikt)
                implementation(libs.okio)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        nativeTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.okio.fakefilesystem)
        }
    }
}

tasks.register<Zip>("packageRelease") {
    group = "distribution"
    description = "Builds the release binary and zips it as build/dist/dots-<version>-macos-arm64.zip."
    val link = kotlin.targets.getByName<KotlinNativeTarget>("native")
        .binaries.getExecutable(NativeBuildType.RELEASE).linkTaskProvider
    from(link) {
        include("*.kexe")
        rename { "dots" }
        filePermissions { unix("rwxr-xr-x") }
    }
    archiveFileName = "dots-${project.version}-macos-arm64.zip"
    destinationDirectory = layout.buildDirectory.dir("dist")
}
