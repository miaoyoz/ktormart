plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

dependencies {
    // Ktor Core
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)

    // Content Negotiation for JSON
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.serialization.json)

    // Logging
    implementation(libs.logback.classic)

    // Database with Exposed
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.hikari.cp)
    implementation(libs.postgresql)

    // Consul
    implementation(libs.consul.client)

    // Testing
    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test.junit)

}
