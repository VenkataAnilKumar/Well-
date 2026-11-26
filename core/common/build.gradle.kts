plugins {
  id("wellplus.android.library")
}

android {
  namespace = "com.example.core.common"
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.biometric)
  implementation(libs.kotlinx.coroutines.core)
}
