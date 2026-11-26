plugins {
  id("wellplus.android.library")
  id("wellplus.android.compose")
}

android {
  namespace = "com.example.core.designsystem"
}

dependencies {
  implementation(project(":core:common"))
  implementation(project(":core:model"))
  implementation(project(":core:safety"))
  implementation(project(":core:analytics"))
  implementation(libs.androidx.core.ktx)
  implementation(libs.coil.compose)
  implementation(libs.mlkit.text.recognition)
}
