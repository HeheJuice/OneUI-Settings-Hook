pluginManagement {
    repositories {
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // The correct, live repository server for Xposed APIs
        maven { url = java.net.URI("https://api.xposed.info/") }
    }
}

rootProject.name = "PropSpoofer"
include(":app")
