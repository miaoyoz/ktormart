package com.ktormart.gateway.plugins

import com.ktormart.gateway.discovery.ServiceDiscovery
import com.ktormart.gateway.resilience.CircuitBreakerManager
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.toByteArray
import org.slf4j.LoggerFactory

/**
 * 配置反向代理路由
 * 将客户端请求转发到相应的微服务，并集成熔断器保护机制
 */
fun Application.configureReverseProxyRouting() {
    // 初始化日志记录器
    val log = LoggerFactory.getLogger("ReverseProxyRouting")

    // 初始化服务发现组件，用于从 Consul 获取健康的服务实例
    val serviceDiscovery = ServiceDiscovery(environment.config)

    // 初始化熔断器管理器，用于保护下游服务调用
    val circuitBreakerManager = CircuitBreakerManager(environment.config)

    // 配置 HTTP 客户端，用于向下游服务发送请求
    val httpClient = createHttpClient()

    routing {
        // 定义一个捕获所有请求的路由
        // 可以处理 GET、POST、PUT、DELETE 等所有 HTTP 方法
        route("/{...}") {
            handle {
                handleProxyRequest(
                    log = log,
                    serviceDiscovery = serviceDiscovery,
                    circuitBreakerManager = circuitBreakerManager,
                    httpClient = httpClient
                )
            }
        }
    }
}

/**
 * 创建配置好的 HTTP 客户端
 *
 * @return 配置了超时参数的 HttpClient 实例
 */
private fun createHttpClient(): HttpClient {
    return HttpClient(CIO) {
        // 设置请求超时时间，防止无限等待
        install(HttpTimeout) {
            requestTimeoutMillis = 3000  // 请求总超时 3 秒
//            connectTimeoutMillis = 2000  // 连接超时 2 秒
//            socketTimeoutMillis = 3000   // Socket 超时 3 秒
        }
    }
}

/**
 * 处理代理请求的主要逻辑
 *
 * @param log 日志记录器
 * @param serviceDiscovery 服务发现组件
 * @param circuitBreakerManager 熔断器管理器
 * @param httpClient HTTP 客户端
 */
private suspend fun RoutingContext.handleProxyRequest(
    log: org.slf4j.Logger,
    serviceDiscovery: ServiceDiscovery,
    circuitBreakerManager: CircuitBreakerManager,
    httpClient: HttpClient
) {
    val path = call.request.path()
    log.info("网关收到请求路径: $path")

    // 根据请求路径查找应该路由到哪个服务
    val serviceName = findServiceNameForPath(path)

    if (serviceName == null) {
        log.warn("未找到路径对应的服务: $path")
        call.respond(HttpStatusCode.NotFound, mapOf("error" to "未找到路径对应的服务: $path"))
        return
    }

    // 使用熔断器执行代理逻辑

    try {
        val response = circuitBreakerManager.executeWithFallback(
            circuitBreakerName = serviceName,
            block = {
                forwardRequestToService(
                    log = log,
                    serviceDiscovery = serviceDiscovery,
                    httpClient = httpClient,
                    serviceName = serviceName,
                    path = path
                )
            },
            fallback = { throwable ->
                handleServiceFallback(log, serviceName, throwable)
            }
        )

        // 复制响应...
    } catch (e: CallNotPermittedException) {
        // ✅ 熔断器已打开,快速失败
        log.warn("熔断器已打开,快速拒绝请求: ${e.message}")
        handleServiceFallback(log, "user-service", e)

    } catch (e: Exception) {
        // ❌ 其他未知错误
        log.error("请求转发失败", e)
        call.respond(
            HttpStatusCode.InternalServerError,
            mapOf(
                "error" to "网关内部错误",
                "message" to (e.message ?: "Unknown error")
            )
        )
    }
}

/**
 * 将请求转发到目标服务
 *
 * @param log 日志记录器
 * @param serviceDiscovery 服务发现组件
 * @param httpClient HTTP 客户端
 * @param serviceName 目标服务名称
 * @param path 原始请求路径
 */
private suspend fun RoutingContext.forwardRequestToService(
    log: org.slf4j.Logger,
    serviceDiscovery: ServiceDiscovery,
    httpClient: HttpClient,
    serviceName: String,
    path: String
) {
    // 从服务发现中获取健康的服务实例
    val serviceInstance = serviceDiscovery.getHealthyServiceInstance(serviceName)
    if (serviceInstance == null) {
        log.error("服务 $serviceName 没有健康的实例可用")
        call.respond(
            HttpStatusCode.ServiceUnavailable,
            mapOf("error" to "服务 $serviceName 没有健康的实例可用")
        )
        return
    }

    // 重写路径并构建目标 URL
    val rewrittenPath = rewritePath(path, serviceName)
    val targetUrl = "http://${serviceInstance.service.address}:${serviceInstance.service.port}$rewrittenPath"

    log.info("转发请求至: $targetUrl")

    // 向下游服务发送 HTTP 请求
    val response = sendRequestToDownstream(httpClient, targetUrl)

    // 在协程中读取响应体为字节数组（这是挂起函数）
    val responseBytes: ByteArray = response.bodyAsChannel().toByteArray()

    // 将下游服务的响应返回给客户端
    respondWithDownstreamResponse(response, responseBytes)
}

/**
 * 向下游服务发送 HTTP 请求
 *
 * @param httpClient HTTP 客户端
 * @param targetUrl 目标服务 URL
 * @return HTTP 响应
 */
private suspend fun RoutingContext.sendRequestToDownstream(
    httpClient: HttpClient,
    targetUrl: String
): HttpResponse {
    return httpClient.request(targetUrl) {
        method = call.request.httpMethod
        // 转发原始请求的所有 headers
        headers.appendAll(call.request.headers)
        // 传递原始请求的请求体（如果有）
        if (call.request.contentLength() != null && call.request.contentLength()!! > 0) {
            setBody(call.receiveChannel())
        }
    }
}

/**
 * 将下游服务的响应返回给客户端
 *
 * @param response 下游服务的 HTTP 响应
 * @param responseBytes 响应体字节数组
 */
private suspend fun RoutingContext.respondWithDownstreamResponse(
    response: HttpResponse,
    responseBytes: ByteArray
) {
    call.respond(object : OutgoingContent.ByteArrayContent() {
        override val contentType: ContentType = response.contentType() ?: ContentType.Application.Json
        override val status: HttpStatusCode = response.status
        override val headers: Headers = response.headers
        override fun bytes(): ByteArray = responseBytes
    })
}

/**
 * 处理服务降级逻辑
 * 当服务异常或熔断器开启时执行
 *
 * @param log 日志记录器
 * @param serviceName 服务名称
 * @param throwable 异常信息
 */
private suspend fun RoutingContext.handleServiceFallback(
    log: org.slf4j.Logger,
    serviceName: String,
    throwable: Throwable
) {
    log.error("熔断器降级逻辑已激活，服务: $serviceName，原因: ${throwable.message}", throwable)

    // 返回友好的错误信息给客户端
    val fallbackResponse = mapOf(
        "error" to "服务暂时不可用，请稍后重试",
        "service" to serviceName,
        "timestamp" to System.currentTimeMillis().toString()
    )
    call.respond(HttpStatusCode.ServiceUnavailable, fallbackResponse)
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