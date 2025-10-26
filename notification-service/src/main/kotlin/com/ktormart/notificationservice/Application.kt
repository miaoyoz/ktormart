package com.ktormart.notificationservice

import com.ktormart.notificationservice.rabbitmq.EventConsumer
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import kotlin.concurrent.thread

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    // Start the event consumer in a separate thread to not block the main application thread
    thread {
        EventConsumer.start(environment.config)
    }
}
