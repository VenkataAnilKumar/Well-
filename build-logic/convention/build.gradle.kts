plugins {
  `kotlin-dsl`
}

dependencies {
  compileOnly(libs.android.gradlePlugin)
}

gradlePlugin {
  plugins {
    register("androidLibrary") {
      id = "wellplus.android.library"
      implementationClass = "AndroidLibraryConventionPlugin"
    }
    register("androidCompose") {
      id = "wellplus.android.compose"
      implementationClass = "AndroidComposeConventionPlugin"
    }
    register("androidFeature") {
      id = "wellplus.android.feature"
      implementationClass = "AndroidFeatureConventionPlugin"
    }
  }
}
