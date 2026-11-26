plugins {
  id("wellplus.android.library")
}

android {
  namespace = "com.example.core.datastore"
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

dependencies {
  implementation(project(":core:common"))
  implementation(project(":core:model"))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.androidx.core)
  testImplementation(libs.robolectric)
}
