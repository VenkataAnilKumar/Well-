import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidFeatureConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    with(target) {
      pluginManager.apply("wellplus.android.library")
      pluginManager.apply("wellplus.android.compose")

      val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
      dependencies {
        add("implementation", project(":core:common"))
        add("implementation", project(":core:model"))
        add("implementation", project(":core:designsystem"))

        add("implementation", libs.findLibrary("androidx-core-ktx").get())
        add("implementation", libs.findLibrary("androidx-activity-compose").get())
        add("implementation", libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
        add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
        add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
        add("implementation", libs.findLibrary("kotlinx-coroutines-android").get())
        add("implementation", libs.findLibrary("kotlinx-coroutines-core").get())
      }
    }
  }
}
