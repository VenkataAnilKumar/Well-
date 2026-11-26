plugins {
  id("wellplus.android.feature")
}

android {
  namespace = "com.example.feature.medications"
}

dependencies {
  implementation(project(":core:firebase"))
  implementation(project(":core:ai"))
  implementation(project(":core:notifications"))
  implementation(project(":core:datastore"))
  implementation(libs.coil.compose)
}
