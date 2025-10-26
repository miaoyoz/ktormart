package com.ktormart.userservice.rabbitmq

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import io.ktor.server.config.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import kotlin.io.use

object EventProducer {
     val logger = LoggerFactory.getLogger(javaClass)
     var connection: Connection? = null
     lateinit var queueName: String

    fun init(config: ApplicationConfig) {
        val factory = ConnectionFactory().apply {
            host = config.property("rabbitmq.host").getString()
            port = config.property("rabbitmq.port").getString().toInt()
            username = config.property("rabbitmq.username").getString()
            password = config.property("rabbitmq.password").getString()
        }
        this.queueName = config.property("rabbitmq.queueName").getString()
        try {
            this.connection = factory.newConnection()
        } catch (e: Exception) {
            logger.error("Failed to connect to RabbitMQ: ${e.message}", e)
        }
    }

     inline fun <reified T> publish(event: T) {
        if (connection == null) {
            logger.error("RabbitMQ connection is not available. Cannot publish event.")
            return
        }
        try {
            connection!!.createChannel().use { channel ->
                channel.queueDeclare(queueName, true, false, false, null)
                val message = Json.encodeToString(event)
                channel.basicPublish("", queueName, null, message.toByteArray(StandardCharsets.UTF_8))
                logger.info("Published message to queue '$queueName': $message")
            }
        } catch (e: Exception) {
            logger.error("Failed to publish message: ${e.message}", e)
        }
    }


    fun close() {
        connection?.close()
    }
}