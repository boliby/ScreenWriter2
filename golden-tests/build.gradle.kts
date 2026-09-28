// Compares layouts of the fountain.io samples with Final Draft's PDFs (§9).
plugins {
    id("screenwriter.kotlin-library")
}

dependencies {
    testImplementation(project(":core-fountain"))
    testImplementation(project(":core-fdx"))
    testImplementation(project(":core-layout"))
    testImplementation(project(":testing"))
    testImplementation(libs.pdfbox)
}
