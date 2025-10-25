plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
}

application {
    mainClass.set("com.ktormart.gateway.ApplicationKt")
}

dependencies {
    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)

    // Ktor Client
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Content Negotiation for JSON
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.serialization.json)

    // Logging
    implementation(libs.logback.classic)

    // Consul
    implementation(libs.consul.client)
}
