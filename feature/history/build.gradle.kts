plugins {
  id("wellplus.android.feature")
}

android {
  namespace = "com.example.feature.history"
}

dependencies {
  implementation(project(":core:firebase"))
  implementation(project(":core:ai"))
  implementation(project(":core:analytics"))
  implementation(project(":core:datastore"))
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.firestore)
}
