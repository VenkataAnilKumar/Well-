plugins {
  id("wellplus.android.library")
}

android {
  namespace = "com.example.core.firebase"
}

dependencies {
  implementation(project(":core:common"))
  implementation(project(":core:model"))
  implementation(project(":core:datastore"))
  implementation(project(":core:safety"))
  api(platform(libs.firebase.bom))
  api(libs.firebase.firestore)
  api(libs.firebase.auth)
  implementation(libs.firebase.config)
  implementation(libs.firebase.crashlytics)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
}
