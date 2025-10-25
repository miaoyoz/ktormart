package com.ktormart.userservice.config

import com.orbitz.consul.Consul
import com.orbitz.consul.KeyValueClient
import io.ktor.server.config.*

class ConsulConfiguration(config: ApplicationConfig) {
    private val kvClient: KeyValueClient
    private val configRootPath: String

    init {
        val consulHost = config.property("consul.host").getString()
        val consulPort = config.property("consul.port").getString().toInt()
        val serviceName = config.property("ktor.application.serviceName").getString()

        // Determine active profile from environment variable or default to dev
        val profile = System.getenv("KTOR_PROFILE") ?: "dev"

        this.configRootPath = "config/$serviceName,$profile"
        this.kvClient = Consul.builder().withUrl("http://$consulHost:$consulPort").build().keyValueClient()
    }

    fun getString(key: String): String {
        return kvClient.getValueAsString("$configRootPath/$key")
            .orElseThrow { IllegalStateException("Missing config value for key: $key") }
    }

    fun getInt(key: String): Int {
        return getString(key).toInt()
    }
}