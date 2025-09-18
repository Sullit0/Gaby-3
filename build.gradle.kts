plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.23" apply false
    id("org.jetbrains.kotlin.multiplatform") version "1.9.23" apply false
    id("org.jetbrains.compose") version "1.6.10" apply false
    id("app.cash.sqldelight") version "2.0.2" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

