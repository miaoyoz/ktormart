package com.ktormart.notificationservice.rabbitmq

import com.ktormart.notificationservice.events.UserRegisteredEvent
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import io.ktor.server.config.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets

object EventConsumer {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun start(config: ApplicationConfig) {
        val factory = ConnectionFactory().apply {
            host = config.property("rabbitmq.host").getString()
            port = config.property("rabbitmq.port").getString().toInt()
            username = config.property("rabbitmq.username").getString()
            password = config.property("rabbitmq.password").getString()
        }
        val queueName = config.property("rabbitmq.queueName").getString()

        try {
            val connection = factory.newConnection()
            val channel = connection.createChannel()

            channel.queueDeclare(queueName, true, false, false, null)
            logger.info("Waiting for messages on queue '$queueName'. To exit press CTRL+C")

            val deliverCallback = DeliverCallback { _, delivery ->
                val message = String(delivery.body, StandardCharsets.UTF_8)
                logger.info("Received message: '$message'")
                try {
                    val event = Json.decodeFromString<UserRegisteredEvent>(message)
                    // Simulate sending an email
                    logger.info("Simulating sending welcome email to ${event.email} for user ${event.username} (ID: ${event.userId})")
                    channel.basicAck(delivery.envelope.deliveryTag, false)
                } catch (e: Exception) {
                    logger.error("Failed to process message. Error: ${e.message}", e)
                    channel.basicNack(delivery.envelope.deliveryTag, false, true) // Re-queue
                }
            }
            channel.basicConsume(queueName, false, deliverCallback) { _ -> }
        } catch (e: Exception) {
            logger.error("RabbitMQ connection failed: ${e.message}", e)
        }
    }
}