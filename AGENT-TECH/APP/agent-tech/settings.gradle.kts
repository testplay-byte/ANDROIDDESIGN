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

rootProject.name = "agent-tech"

include(":app")
include(":core:designsystem")
include(":core:common")
include(":core:database")
include(":agent:core")
include(":agent:llm")
include(":agent:tools")
include(":agent:storage")
