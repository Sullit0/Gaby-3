plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("app.cash.sqldelight")
}

kotlin {
    jvm("desktop")
    androidTarget()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                implementation("io.insert-koin:koin-core:3.5.3")
                implementation("app.cash.sqldelight:runtime:2.0.2")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")
                implementation("com.squareup.okio:okio:3.9.0")
                implementation("com.benasher44:uuid:0.8.2")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
                implementation("com.github.librepdf:openpdf:1.3.41")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:android-driver:2.0.2")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("app.cash.turbine:turbine:1.0.0")
            }
        }
    }
}

android {
    namespace = "com.clinica.shared"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        encoding = "UTF-8"
    }
}

sqldelight {
    databases {
        create("PsychNotesDb") {
            packageName.set("com.clinica.data")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}
