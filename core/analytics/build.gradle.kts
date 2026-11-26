plugins {
  id("wellplus.android.library")
}

android {
  namespace = "com.example.core.analytics"
}

dependencies {
  implementation(project(":core:common"))
  implementation(project(":core:model"))
  implementation(project(":core:safety"))
  implementation(libs.androidx.core.ktx)
  implementation(libs.kotlinx.coroutines.core)
  testImplementation(project(":core:testing"))
}
