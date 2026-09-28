import app.cash.licensee.LicenseeExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Fails the build if any shipped dependency has a license outside this list (see CLAUDE.md). */
class LicensesConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("app.cash.licensee")
        extensions.configure<LicenseeExtension> {
            allow("Apache-2.0")
            allow("MIT")
            allow("BSD-2-Clause")
            allow("BSD-3-Clause")
            allow("MPL-2.0")
        }
    }
}
