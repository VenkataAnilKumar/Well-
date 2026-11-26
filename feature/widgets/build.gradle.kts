plugins {
  id("wellplus.android.library")
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.example.feature.widgets"

  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation(project(":core:common"))
  implementation(project(":core:datastore"))
  implementation(project(":core:model"))
  implementation(project(":core:firebase"))
  implementation(project(":core:notifications"))

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.glance.material3)

  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
}
