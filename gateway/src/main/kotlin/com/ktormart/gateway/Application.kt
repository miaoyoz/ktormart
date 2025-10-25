package com.ktormart.gateway

import com.ktormart.gateway.plugins.configureReverseProxyRouting
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    configureReverseProxyRouting()

}
