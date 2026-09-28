plugins {
    id("screenwriter.kotlin-library")
}

dependencies {
    api(project(":core-model"))
    api(project(":core-layout"))
    testImplementation(project(":core-fountain"))
    testImplementation(project(":testing"))
}
