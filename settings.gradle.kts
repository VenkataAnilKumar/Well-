pluginManagement {
  includeBuild("build-logic")
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "Well+"

include(":app")

// Core foundation modules
include(
  ":core:common",
  ":core:datastore",
  ":core:model",
  ":core:safety",
  ":core:analytics",
  ":core:healthconnect",
  ":core:firebase",
  ":core:ai",
  ":core:notifications",
  ":core:designsystem",
  ":core:testing",
)

// Feature modules (zero cross-feature dependencies)
include(
  ":feature:auth",
  ":feature:today",
  ":feature:consult",
  ":feature:medications",
  ":feature:vitals",
  ":feature:mental",
  ":feature:bodycycle",
  ":feature:healthconnect",
  ":feature:history",
  ":feature:profile",
  ":feature:widgets",
)
