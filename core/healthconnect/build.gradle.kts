plugins {
  id("wellplus.android.library")
}

android {
  namespace = "com.example.core.healthconnect"
}

dependencies {
  implementation(project(":core:common"))
  implementation(project(":core:datastore"))
  implementation(project(":core:model"))
  api(libs.androidx.health.connect.client)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
}
