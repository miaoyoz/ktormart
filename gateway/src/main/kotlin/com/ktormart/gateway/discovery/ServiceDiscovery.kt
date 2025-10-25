package com.ktormart.gateway.discovery

import com.orbitz.consul.Consul
import com.orbitz.consul.HealthClient
import com.orbitz.consul.model.health.ServiceHealth
import io.ktor.server.config.*

class ServiceDiscovery(config: ApplicationConfig) {
    private val healthClient: HealthClient

    init {
        val consulHost = config.property("consul.host").getString()
        val consulPort = config.property("consul.port").getString().toInt()
        this.healthClient = Consul.builder().withUrl("http://$consulHost:$consulPort").build().healthClient()
    }

    fun getHealthyServiceInstance(serviceName: String): ServiceHealth? {
        // Get all healthy instances for the given service name
        val healthyInstances = healthClient.getHealthyServiceInstances(serviceName).response

        // Simple round-robin is not implemented here, we just take the first one.
        // In a real-world scenario, you'd implement a load balancing strategy.
        return healthyInstances.firstOrNull()
    }
}