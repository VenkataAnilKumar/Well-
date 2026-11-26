plugins {
  id("wellplus.android.feature")
}

android {
  namespace = "com.example.feature.today"
}

dependencies {
  implementation(project(":core:analytics"))
  implementation(project(":core:firebase"))
  implementation(project(":core:ai"))
  implementation(project(":core:healthconnect"))
  implementation(project(":core:safety"))
  implementation(project(":core:datastore"))
}
