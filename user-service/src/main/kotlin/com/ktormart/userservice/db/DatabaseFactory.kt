package com.ktormart.userservice.db

import com.ktormart.userservice.config.ConsulConfiguration
import com.ktormart.userservice.models.Users
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {
    fun init(config: ConsulConfiguration) {
        val driverClassName = config.getString("database/driverClassName")
        val jdbcURL = config.getString("database/jdbcURL")
        val username = config.getString("database/username")
        val password = config.getString("database/password")
        val maximumPoolSize = config.getInt("database/maximumPoolSize")

        val dataSource = createDataSource(jdbcURL, driverClassName, username, password, maximumPoolSize)
        Database.connect(dataSource)

        // Create tables
        transaction {
            SchemaUtils.create(Users)
        }
    }

    private fun createDataSource(
        url: String,
        driver: String,
        user: String,
        pass: String,
        poolSize: Int
    ): HikariDataSource {
        val config = HikariConfig().apply {
            driverClassName = driver
            jdbcUrl = url
            username = user
            password = pass
            maximumPoolSize = poolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(config)
    }

    // Helper function for database transactions within coroutines
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}