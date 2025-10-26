package com.ktormart.gateway.resilience

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import io.ktor.server.config.*
import java.time.Duration

class CircuitBreakerManager(config: ApplicationConfig) {
    private val registry: CircuitBreakerRegistry

    init {
        val cbConfig = config.config("resilience4j.circuitbreaker")
        val serviceNames = cbConfig.keys().filter { !it.contains(".") }
        val circuitBreakerConfigs = serviceNames.associateWith { serviceName ->
            val serviceCbConfig = cbConfig.config(serviceName)
            buildCircuitBreakerConfig(serviceCbConfig)
        }

        this.registry = CircuitBreakerRegistry.of(circuitBreakerConfigs)
    }

    private fun buildCircuitBreakerConfig(config: ApplicationConfig): CircuitBreakerConfig {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(config.property("failureRateThreshold").getString().toFloat())
            .slowCallDurationThreshold(Duration.parse("PT${config.property("slowCallDurationThreshold").getString().uppercase()}"))
            .waitDurationInOpenState(Duration.parse("PT${config.property("waitDurationInOpenState").getString().uppercase()}"))
            .permittedNumberOfCallsInHalfOpenState(config.property("permittedNumberOfCallsInHalfOpenState").getString().toInt())
            .slidingWindowSize(config.property("slidingWindowSize").getString().toInt())
            .minimumNumberOfCalls(config.property("minimumNumberOfCalls").getString().toInt())
            .build()
    }

    fun getCircuitBreaker(name: String): CircuitBreaker {
        return registry.circuitBreaker(name)
    }

    // A helper to execute suspend functions with circuit breaker and a fallback
    suspend fun <T> executeWithFallback(
        circuitBreakerName: String,
        block: suspend () -> T,
        fallback: suspend (Throwable) -> T
    ): T {
        return try {
            getCircuitBreaker(circuitBreakerName).executeSuspendFunction {
                block()
            }
        } catch (e: Exception) {
            // This catches exceptions like CallNotPermittedException, etc.
            fallback(e)
        }
    }
}