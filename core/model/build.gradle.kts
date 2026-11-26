plugins {
  id("wellplus.android.library")
}

android {
  namespace = "com.example.core.model"
}

dependencies {
  implementation(project(":core:common"))
  api(platform(libs.firebase.bom))
  api(libs.firebase.firestore)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.junit)
}
