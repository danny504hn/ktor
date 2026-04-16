val exposed_version: String by project
val sqlite_jdbc_version: String by project
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.example"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.cio.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // exposed
    implementation("org.jetbrains.exposed:exposed-core:${exposed_version}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${exposed_version}")
    implementation("org.jetbrains.exposed:exposed-java-time:${exposed_version}")
    //driver sqlite
    implementation("org.xerial:sqlite-jdbc:${sqlite_jdbc_version}")
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation("io.ktor:ktor-server-html-builder")
    implementation(libs.ktor.server.cio)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)

    //testing
//    testImplementation("io.ktor:ktor-server-test-host:${ktor_version}")
//    testImplementation("org.jetbrains.kotlin:kotlin-test:${kotlin_version}")
}
