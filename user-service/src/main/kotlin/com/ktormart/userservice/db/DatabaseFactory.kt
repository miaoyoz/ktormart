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

/**
 * 数据库工厂对象
 * 负责数据库连接池的初始化和事务管理
 */
object DatabaseFactory {
    /**
     * 初始化数据库连接
     *
     * @param config Consul 配置对象，用于获取数据库连接参数
     */
    fun init(config: ConsulConfiguration) {
        val driverClassName = config.getString("database/driverClassName")
        val jdbcURL = config.getString("database/jdbcURL")
        val username = config.getString("database/username")
        val password = config.getString("database/password")
        val maximumPoolSize = config.getInt("database/maximumPoolSize")

        val dataSource = createDataSource(jdbcURL, driverClassName, username, password, maximumPoolSize)
        Database.connect(dataSource)

        // 创建数据库表
        transaction {
            SchemaUtils.create(Users)
        }
    }

    /**
     * 创建 HikariCP 数据源
     *
     * @param url JDBC 连接 URL
     * @param driver JDBC 驱动类名
     * @param user 数据库用户名
     * @param pass 数据库密码
     * @param poolSize 连接池最大大小
     * @return HikariDataSource 实例
     */
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

    /**
     * 在协程上下文中执行数据库查询的辅助函数
     *
     * @param T 返回值类型
     * @param block 要执行的数据库操作
     * @return 操作结果
     */
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}