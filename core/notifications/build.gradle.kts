plugins {
  id("wellplus.android.library")
}

android {
  namespace = "com.example.core.notifications"
}

dependencies {
  implementation(project(":core:common"))
  implementation(project(":core:model"))
  implementation(project(":core:firebase"))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
}
