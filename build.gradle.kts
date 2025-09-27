plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.22" apply false
    id("org.jetbrains.kotlin.multiplatform") version "1.9.22" apply false
    id("org.jetbrains.compose") version "1.6.2" apply false
    id("app.cash.sqldelight") version "2.0.2" apply false
    id("com.android.application") version "8.2.2" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
