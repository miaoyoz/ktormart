package com.ktormart.gateway.discovery

import com.orbitz.consul.Consul
import com.orbitz.consul.HealthClient
import com.orbitz.consul.model.health.ServiceHealth
import io.ktor.server.config.*

/**
 * 服务发现类
 * 从 Consul 中查询健康的服务实例
 *
 * @param config 应用配置对象
 */
class ServiceDiscovery(config: ApplicationConfig) {
    private val healthClient: HealthClient

    init {
        val consulHost = config.property("consul.host").getString()
        val consulPort = config.property("consul.port").getString().toInt()
        this.healthClient = Consul.builder().withUrl("http://$consulHost:$consulPort").build().healthClient()
    }

    /**
     * 获取指定服务的健康实例
     *
     * @param serviceName 服务名称
     * @return 健康的服务实例，如果没有可用实例则返回 null
     *
     * 注意：当前实现只返回第一个健康实例。
     * 在真实场景中，应该实现负载均衡策略（如轮询、随机、最少连接等）
     */
    fun getHealthyServiceInstance(serviceName: String): ServiceHealth? {
        // 获取给定服务名称的所有健康实例
        val healthyInstances = healthClient.getHealthyServiceInstances(serviceName).response

        // 简单实现：返回第一个实例
        // TODO: 实现负载均衡策略
        return healthyInstances.firstOrNull()
    }
}