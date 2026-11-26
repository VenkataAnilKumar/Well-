plugins {
  id("wellplus.android.library")
}

android {
  namespace = "com.example.core.safety"
}

dependencies {
  implementation(project(":core:common"))
  implementation(project(":core:model"))
  testImplementation(project(":core:testing"))
}
