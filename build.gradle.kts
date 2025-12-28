plugins {
    alias(libs.plugins.kotlinMultiplatform)
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
        nativeMain.dependencies {
            implementation(libs.clikt)
            implementation(libs.clikt)
        }
    }
}
