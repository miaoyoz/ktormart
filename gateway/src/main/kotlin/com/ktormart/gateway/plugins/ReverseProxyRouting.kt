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

/**
 * 配置反向代理路由
 * 将客户端请求转发到相应的微服务
 */
fun Application.configureReverseProxyRouting() {
    val serviceDiscovery = ServiceDiscovery(environment.config)

    val httpClient = HttpClient(CIO)

    routing {
        // 定义一个捕获所有请求的路由
        // 可以处理 GET、POST、PUT、DELETE 等所有 HTTP 方法
        route("/{...}") {
            handle {
                val path = call.request.path()
                println("网关收到请求路径: $path")

                // 根据请求路径查找应该路由到哪个服务
                // 示例: /api/v1/users/register -> user-service
                val serviceName = findServiceNameForPath(path)

                if (serviceName == null) {
                    call.respond(HttpStatusCode.NotFound, "未找到路径对应的服务: $path")
                    return@handle
                }

                // 从服务发现中获取健康的服务实例
                val serviceInstance = serviceDiscovery.getHealthyServiceInstance(serviceName)
                if (serviceInstance == null) {
                    call.respond(HttpStatusCode.ServiceUnavailable, "服务 $serviceName 没有健康的实例可用")
                    return@handle
                }

                // 重写路径并转发请求
                val rewrittenPath = rewritePath(path, serviceName)
                val targetUrl = "http://${serviceInstance.service.address}:${serviceInstance.service.port}$rewrittenPath"

                println("转发请求至: $targetUrl")

                // 向下游服务发送 HTTP 请求
                val response = httpClient.request(targetUrl) {
                    method = call.request.httpMethod
                    headers.appendAll(call.request.headers)
                    // 传递原始请求的请求体
                    if (call.request.contentLength() != null && call.request.contentLength()!! > 0) {
                        setBody(call.receiveChannel())
                    }
                }

                // 在协程中读取响应体为字节数组（这是挂起函数）
                val responseBytes: ByteArray = response.bodyAsChannel().toByteArray()

                // 将下游服务的响应返回给客户端
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

/**
 * 根据请求路径查找对应的服务名称
 * 这是一个简化的实现，基于 application.conf 中的配置
 * 在真实的网关中，这个逻辑会更加复杂
 *
 * @param path 请求路径
 * @return 服务名称，如果没有匹配的服务则返回 null
 */
private fun findServiceNameForPath(path: String): String? {
    return when {
        path.startsWith("/api/v1/users") -> "user-service"
        // 未来可以在这里添加更多服务的路由规则
        // path.startsWith("/api/v1/products") -> "product-service"
        // path.startsWith("/api/v1/orders") -> "order-service"
        else -> null
    }
}

/**
 * 重写请求路径
 * 将网关路径转换为服务内部路径
 *
 * @param path 原始请求路径
 * @param serviceName 目标服务名称
 * @return 重写后的路径
 */
private fun rewritePath(path: String, serviceName: String): String {
    return when (serviceName) {
        "user-service" -> path.replace("/api/v1/users", "/users")
        // 未来可以在这里添加更多服务的路径重写规则
        // "product-service" -> path.replace("/api/v1/products", "/products")
        // "order-service" -> path.replace("/api/v1/orders", "/orders")
        else -> path
    }
}