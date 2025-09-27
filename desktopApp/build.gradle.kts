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
    // Dependencias para manejo de documentos Word
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    implementation("org.apache.poi:poi-scratchpad:5.2.5")
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

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

tasks.withType<Test> {
    systemProperty("file.encoding", "UTF-8")
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

tasks.withType<JavaExec> {
    systemProperty("file.encoding", "UTF-8")
    jvmArgs = listOf("-Dfile.encoding=UTF-8")
}
