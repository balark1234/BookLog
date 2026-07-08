pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BookLog"
include(":app")

// Keep build outputs outside OneDrive to avoid sync/file-lock issues.
val externalBuildRoot = java.io.File(
    System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"),
    "BookLog-build",
)
gradle.beforeProject {
    val segment = if (path == ":") name else path.removePrefix(":").replace(":", java.io.File.separator)
    layout.buildDirectory.set(externalBuildRoot.resolve(segment))
}