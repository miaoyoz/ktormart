package com.ktormart.gateway

import com.ktormart.gateway.plugins.configureReverseProxyRouting
import com.ktormart.gateway.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.calllogging.CallLogging

/**
 * 网关服务入口点
 */
fun main(args: Array<String>): Unit = EngineMain.main(args)

/**
 * 网关主模块配置函数
 * 配置反向代理路由
 */
fun Application.module() {
    // 增加日志记录插件
    install(CallLogging)
    configureSerialization()
    configureReverseProxyRouting()
}
