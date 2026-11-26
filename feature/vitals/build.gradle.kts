plugins {
  id("wellplus.android.feature")
}

android {
  namespace = "com.example.feature.vitals"
}

dependencies {
  implementation(project(":core:firebase"))
  implementation(project(":core:ai"))
  implementation(project(":core:datastore"))
  implementation(project(":core:safety"))
  implementation(project(":core:healthconnect"))
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.firestore)
  testImplementation(project(":core:testing"))
}
