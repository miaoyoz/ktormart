package com.ktormart.gateway

import com.ktormart.gateway.plugins.configureReverseProxyRouting
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain

/**
 * 网关服务入口点
 */
fun main(args: Array<String>): Unit = EngineMain.main(args)

/**
 * 网关主模块配置函数
 * 配置反向代理路由
 */
fun Application.module() {
    configureReverseProxyRouting()

}
