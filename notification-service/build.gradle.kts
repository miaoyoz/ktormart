plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
}

application {
    mainClass.set("com.ktormart.notificationservice.ApplicationKt")
}

dependencies {
    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)

    // Logging
    implementation(libs.logback.classic)

    //  RabbitMQ Client
    implementation(libs.rabbitmq.client)

    // Kotlinx Serialization for parsing event messages
    implementation(libs.kotlinx.serialization.json)
}
