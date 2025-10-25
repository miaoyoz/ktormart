package com.ktormart.userservice.discovery

import com.orbitz.consul.Consul
import com.orbitz.consul.model.agent.ImmutableRegistration
import com.orbitz.consul.model.agent.Registration
import io.ktor.server.config.*
import java.util.*

object ConsulServiceRegistry {
    private lateinit var consulClient: Consul
    private lateinit var serviceId: String
    private lateinit var serviceName: String

    fun init(config: ApplicationConfig) {
        val consulHost = config.property("consul.host").getString()
        val consulPort = config.property("consul.port").getString().toInt()

        this.serviceName = config.property("ktor.application.serviceName").getString()
        this.serviceId = "$serviceName-${UUID.randomUUID()}"
        this.consulClient = Consul.builder().withUrl("http://$consulHost:$consulPort").build()
    }

    fun register(port: Int) {
        val registration = ImmutableRegistration.builder()
            .id(serviceId)
            .name(serviceName)
            .port(port)
            .putMeta("version", "0.1")
            // Note: For Docker, use the service's container name or IP, not localhost.
            // For local dev, localhost is fine. We will address Docker networking later.
            .address("127.0.0.1")
            .check(Registration.RegCheck.http("http://127.0.0.1:$port/health", 10L, 1L))
            .build()

        consulClient.agentClient().register(registration)
        println("Registered service '$serviceName' with ID '$serviceId' in Consul.")
    }

    fun deregister() {
        consulClient.agentClient().deregister(serviceId)
        println("Deregistered service '$serviceName' with ID '$serviceId' from Consul.")
    }
}