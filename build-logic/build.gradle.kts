plugins {
    `kotlin-dsl`
}

// The plugins themselves are on the root build's classpath (see the root
// build.gradle.kts); these conventions only compile against them.
dependencies {
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.licensee.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kotlinLibrary") {
            id = "screenwriter.kotlin-library"
            implementationClass = "KotlinLibraryConventionPlugin"
        }
        register("licenses") {
            id = "screenwriter.licenses"
            implementationClass = "LicensesConventionPlugin"
        }
    }
}
