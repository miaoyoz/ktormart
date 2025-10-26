pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "ktormart"

include(":user-service")
include(":gateway")
include("notification-service")