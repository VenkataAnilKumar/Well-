plugins {
  id("wellplus.android.library")
}

android {
  namespace = "com.example.core.ai"
}

dependencies {
  implementation(project(":core:common"))
  implementation(project(":core:model"))
  implementation(project(":core:safety"))
  implementation(project(":core:datastore"))
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
}
