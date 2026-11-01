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
