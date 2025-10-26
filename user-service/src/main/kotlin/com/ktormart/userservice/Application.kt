package com.ktormart.userservice

import com.ktormart.userservice.config.ConsulConfiguration
import com.ktormart.userservice.db.DatabaseFactory
import com.ktormart.userservice.discovery.ConsulServiceRegistry
import com.ktormart.userservice.plugins.configureRouting
import com.ktormart.userservice.plugins.configureSerialization
import com.ktormart.userservice.rabbitmq.EventProducer
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain

/**
 * 应用程序入口点
 */
fun main(args: Array<String>): Unit = EngineMain.main(args)

/**
 * 主模块配置函数
 * 负责初始化所有必要的插件和服务
 */
fun Application.module() {

    // 配置 Ktor 插件
    configureSerialization()
    configureRouting()

    // 获取服务端口
    val port = environment.config.property("ktor.deployment.port").getString().toInt()

    // 初始化 Consul 服务注册
    ConsulServiceRegistry.init(environment.config)
    ConsulServiceRegistry.register(port)

    // 添加关闭钩子，在应用停止时注销服务
    monitor.subscribe(ApplicationStopping) {
        ConsulServiceRegistry.deregister()
    }

    // Initialize RabbitMQ producer
    EventProducer.init(environment.config)

    monitor.subscribe(ApplicationStopping) {
        ConsulServiceRegistry.deregister()
        EventProducer.close() // Close connection on shutdown
    }

    // 创建 Consul 配置源并初始化数据库连接
    val consulConfig = ConsulConfiguration(environment.config)
    DatabaseFactory.init(consulConfig)

}
