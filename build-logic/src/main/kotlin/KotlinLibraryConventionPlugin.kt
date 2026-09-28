import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** A pure-Kotlin `core-*` module: JVM 17 bytecode, kotlin.test on JUnit 5, license check. */
class KotlinLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        pluginManager.apply("screenwriter.licenses")

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        }
        dependencies {
            add("testImplementation", "org.jetbrains.kotlin:kotlin-test")
        }
        val samples = rootDir.resolve("samples")
        val requireSamples = providers.environmentVariable("REQUIRE_SAMPLES").orElse("")
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            systemProperty("screenwriter.samples", samples.absolutePath)
            systemProperty("screenwriter.requireSamples", requireSamples.get())
            inputs.files(samples).withPropertyName("samples").withPathSensitivity(PathSensitivity.RELATIVE)
        }
    }
}
