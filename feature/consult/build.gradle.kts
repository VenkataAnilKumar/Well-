plugins {
  id("wellplus.android.feature")
}

android {
  namespace = "com.example.feature.consult"
}

dependencies {
  implementation(project(":core:firebase"))
  implementation(project(":core:ai"))
  implementation(project(":core:safety"))
  implementation(project(":core:notifications"))
  implementation(project(":core:datastore"))
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.coil.compose)
  testImplementation(project(":core:testing"))
}
