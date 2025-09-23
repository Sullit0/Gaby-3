plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
    implementation("io.insert-koin:koin-core:3.5.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    implementation("com.benasher44:uuid:0.8.2")
    implementation("org.apache.pdfbox:pdfbox:3.0.2")
    }

kotlin {
    jvmToolchain(17)
    sourceSets {
        named("main") {
            kotlin.srcDir("src/jvmMain/kotlin")
            resources.srcDir("src/jvmMain/resources")
        }
        named("test") {
            kotlin.srcDir("src/jvmTest/kotlin")
            resources.srcDir("src/jvmTest/resources")
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.clinica.desktop.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi)
            packageName = "PsychNotes"
            packageVersion = "1.0.0"
        }
    }
}
