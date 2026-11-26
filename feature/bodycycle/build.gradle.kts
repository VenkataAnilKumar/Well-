plugins {
  id("wellplus.android.feature")
}

android {
  namespace = "com.example.feature.bodycycle"
}

dependencies {
  implementation(project(":core:firebase"))
  implementation(project(":core:ai"))
  implementation(project(":core:analytics"))
  implementation(project(":core:datastore"))
}
