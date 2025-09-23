pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "psych-notes"
include(":shared", ":desktopApp", ":androidApp")
