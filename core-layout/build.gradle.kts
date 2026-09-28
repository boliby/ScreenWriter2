plugins {
    id("screenwriter.kotlin-library")
}

dependencies {
    api(project(":core-model"))
    testImplementation(project(":core-fountain"))
}
