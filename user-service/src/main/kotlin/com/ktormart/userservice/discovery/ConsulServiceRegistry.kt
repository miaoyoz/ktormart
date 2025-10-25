package com.ktormart.userservice.discovery

import com.orbitz.consul.Consul
import com.orbitz.consul.model.agent.ImmutableRegistration
import com.orbitz.consul.model.agent.Registration
import io.ktor.server.config.*
import java.util.*

/**
 * Consul 服务注册管理对象
 * 负责服务的注册和注销
 */
object ConsulServiceRegistry {
    private lateinit var consulClient: Consul
    private lateinit var serviceId: String
    private lateinit var serviceName: String

    /**
     * 初始化 Consul 客户端
     *
     * @param config 应用配置对象
     */
    fun init(config: ApplicationConfig) {
        val consulHost = config.property("consul.host").getString()
        val consulPort = config.property("consul.port").getString().toInt()

        this.serviceName = config.property("ktor.application.serviceName").getString()
        this.serviceId = "$serviceName-${UUID.randomUUID()}"
        this.consulClient = Consul.builder().withUrl("http://$consulHost:$consulPort").build()
    }

    /**
     * 向 Consul 注册服务实例
     *
     * @param port 服务监听的端口
     */
    fun register(port: Int) {
        val registration = ImmutableRegistration.builder()
            .id(serviceId)
            .name(serviceName)
            .port(port)
            .putMeta("version", "0.1")
            // 注意：对于 Docker 环境，使用容器名称或 IP，而非 localhost
            // 对于本地开发，使用 localhost 即可，后续会处理 Docker 网络问题
            .address("127.0.0.1")
            // 健康检查配置：URL, 检查间隔（10秒）, 超时时间（1秒）
            .check(Registration.RegCheck.http("http://127.0.0.1:$port/health", 10L, 1L))
            .build()

        consulClient.agentClient().register(registration)
        println("已在 Consul 中注册服务 '$serviceName'，服务 ID 为 '$serviceId'")
    }

    /**
     * 从 Consul 注销服务实例
     */
    fun deregister() {
        consulClient.agentClient().deregister(serviceId)
        println("已从 Consul 注销服务 '$serviceName'，服务 ID 为 '$serviceId'")
    }
}