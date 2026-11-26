plugins {
  id("wellplus.android.library")
}

android {
  namespace = "com.example.core.testing"
}

dependencies {
  api(libs.junit)
  api(libs.kotlinx.coroutines.test)
  implementation(project(":core:common"))
  implementation(project(":core:datastore"))
  implementation(project(":core:model"))
}
