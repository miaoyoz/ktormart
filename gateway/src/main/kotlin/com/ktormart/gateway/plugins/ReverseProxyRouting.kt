package com.ktormart.gateway.plugins

import com.ktormart.gateway.discovery.ServiceDiscovery
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.toByteArray

fun Application.configureReverseProxyRouting() {
    val serviceDiscovery = ServiceDiscovery(environment.config)

    val httpClient = HttpClient(CIO)

    routing {
        // We define a catch-all route to proxy requests
        // This will handle GET, POST, PUT, DELETE, etc.
        route("/{...}") {
            handle {
                val path = call.request.path()
                println("Gateway received request for path: $path")

                // Find which service this request should be routed to
                // Example: /api/v1/users/register -> user-service
                val serviceName = findServiceNameForPath(path)

                if (serviceName == null) {
                    call.respond(HttpStatusCode.NotFound, "Service not found for path: $path")
                    return@handle
                }

                val serviceInstance = serviceDiscovery.getHealthyServiceInstance(serviceName)
                if (serviceInstance == null) {
                    call.respond(HttpStatusCode.ServiceUnavailable, "No healthy instances available for service: $serviceName")
                    return@handle
                }

                // Rewrite the path and forward the request
                val rewrittenPath = rewritePath(path, serviceName)
                val targetUrl = "http://${serviceInstance.service.address}:${serviceInstance.service.port}$rewrittenPath"

                println("Forwarding request to: $targetUrl")

                val response = httpClient.request(targetUrl) {
                    method = call.request.httpMethod
                    headers.appendAll(call.request.headers)
                    // Pass the body of the original request
                    if (call.request.contentLength() != null && call.request.contentLength()!! > 0) {
                        setBody(call.receiveChannel())
                    }
                }

                val responseBytes: ByteArray = response.bodyAsChannel().toByteArray()

                // Respond to the client with the response from the downstream service
                call.respond(object : OutgoingContent.ByteArrayContent() {
                    override val contentType: ContentType = response.contentType() ?: ContentType.Application.Json
                    override val status: HttpStatusCode = response.status
                    override val headers: Headers = response.headers
                    override fun bytes(): ByteArray = responseBytes
                })
            }
        }
    }
}

// These are simplified helper functions based on our application.conf
// In a real gateway, this logic would be much more sophisticated.
private fun findServiceNameForPath(path: String): String? {
    return when {
        path.startsWith("/api/v1/users") -> "user-service"
        // Add more rules here for other services in the future
        // path.startsWith("/api/v1/products") -> "product-service"
        else -> null
    }
}

private fun rewritePath(path: String, serviceName: String): String {
    return when (serviceName) {
        "user-service" -> path.replace("/api/v1/users", "/users")
        // "product-service" -> path.replace("/api/v1/products", "/products")
        else -> path
    }
}