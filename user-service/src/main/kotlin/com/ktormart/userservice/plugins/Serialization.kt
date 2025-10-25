package com.ktormart.userservice.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

/**
 * 配置 JSON 序列化插件
 * 使用 Kotlinx Serialization 进行内容协商
 */
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}