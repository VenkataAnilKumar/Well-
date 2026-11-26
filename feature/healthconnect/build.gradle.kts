plugins {
  id("wellplus.android.feature")
}

android {
  namespace = "com.example.feature.healthconnect"
}

dependencies {
  implementation(project(":core:firebase"))
  implementation(project(":core:ai"))
  implementation(project(":core:healthconnect"))
  implementation(project(":core:safety"))
  implementation(project(":core:datastore"))
  implementation(libs.androidx.health.connect.client)
}
