package com.ktormart.userservice

import com.ktormart.userservice.config.ConsulConfiguration
import com.ktormart.userservice.db.DatabaseFactory
import com.ktormart.userservice.discovery.ConsulServiceRegistry
import com.ktormart.userservice.plugins.configureRouting
import com.ktormart.userservice.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    val port = environment.config.property("ktor.deployment.port").getString().toInt()
    // Initialize database connection

    // Initialize Consul registry
    ConsulServiceRegistry.init(environment.config)
    ConsulServiceRegistry.register(port)

    // Add a shutdown hook to deregister the service
    monitor.subscribe(ApplicationStopping) {
        ConsulServiceRegistry.deregister()
    }
    // Create the Consul configuration source
    val consulConfig = ConsulConfiguration(environment.config)
    DatabaseFactory.init(consulConfig)

    // Configure Ktor plugins
    configureSerialization()
    configureRouting()
}
