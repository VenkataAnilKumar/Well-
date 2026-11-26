plugins {
  id("wellplus.android.feature")
}

android {
  namespace = "com.example.feature.profile"
}

dependencies {
  implementation(project(":core:firebase"))
  implementation(project(":core:analytics"))
  implementation(project(":core:notifications"))
  implementation(project(":core:safety"))
  implementation(project(":core:datastore"))
  implementation(libs.coil.compose)
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.auth)
}
