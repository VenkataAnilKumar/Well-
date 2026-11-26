plugins {
  id("wellplus.android.feature")
}

android {
  namespace = "com.example.feature.auth"
}

dependencies {
  implementation(project(":core:firebase"))
  implementation(project(":core:notifications"))
  implementation(project(":core:safety"))
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.biometric)
  implementation(libs.coil.compose)
}
