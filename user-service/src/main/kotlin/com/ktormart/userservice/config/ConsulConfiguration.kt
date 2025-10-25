package com.ktormart.userservice.config

import com.orbitz.consul.Consul
import com.orbitz.consul.KeyValueClient
import io.ktor.server.config.*

/**
 * Consul 配置管理类
 * 从 Consul KV 存储中读取配置信息
 *
 * @param config 应用配置对象
 */
class ConsulConfiguration(config: ApplicationConfig) {
    private val kvClient: KeyValueClient
    private val configRootPath: String

    init {
        val consulHost = config.property("consul.host").getString()
        val consulPort = config.property("consul.port").getString().toInt()
        val serviceName = config.property("ktor.application.serviceName").getString()

        // 从环境变量获取激活的配置文件，默认为 dev
        val profile = System.getenv("KTOR_PROFILE") ?: "dev"

        this.configRootPath = "config/$serviceName,$profile"
        this.kvClient = Consul.builder().withUrl("http://$consulHost:$consulPort").build().keyValueClient()
    }

    /**
     * 从 Consul KV 中获取字符串类型的配置值
     *
     * @param key 配置键
     * @return 配置值
     * @throws IllegalStateException 如果配置键不存在
     */
    fun getString(key: String): String {
        return kvClient.getValueAsString("$configRootPath/$key")
            .orElseThrow { IllegalStateException("Missing config value for key: $key") }
    }

    /**
     * 从 Consul KV 中获取整数类型的配置值
     *
     * @param key 配置键
     * @return 配置值（整数）
     * @throws IllegalStateException 如果配置键不存在
     */
    fun getInt(key: String): Int {
        return getString(key).toInt()
    }
}