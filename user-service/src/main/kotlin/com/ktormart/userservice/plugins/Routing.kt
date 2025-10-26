package com.ktormart.userservice.plugins

import com.ktormart.userservice.db.DatabaseFactory.dbQuery
import com.ktormart.userservice.events.UserRegisteredEvent
import com.ktormart.userservice.models.*
import com.ktormart.userservice.rabbitmq.EventProducer
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insert

/**
 * 配置应用路由
 * 定义所有 API 端点
 */
fun Application.configureRouting() {
    routing {
        // Consul 健康检查端点
        get("/health") {
            call.respond(HttpStatusCode.OK, "Healthy")
        }

        // 用户相关路由
        route("/users") {

            // 用户注册端点
            post("/register") {
//                delay(100000) // 模拟延迟，用于测试网关超时和熔断器功能
                val request = call.receive<UserRegisterRequest>()

                // 简单的密码验证
                if (request.password.length < 6) {
                    call.respond(HttpStatusCode.BadRequest, "Password should be at least 6 characters long")
                    return@post
                }

                try {
                    // 在数据库中插入新用户
                    val newUserId = dbQuery {
                        Users.insert {
                            it[username] = request.username
                            it[email] = request.email
                            // 注意：在真实应用中，必须对密码进行哈希处理！
                            it[passwordHash] = "hashed_${request.password}"
                        } get Users.id
                    }

                    // 构建响应对象
                    val response = UserResponse(
                        id = newUserId.toString(),
                        username = request.username,
                        email = request.email
                    )
                    val event = UserRegisteredEvent(response.id, response.username, response.email)
                    EventProducer.publish(event)
                    call.respond(HttpStatusCode.Created, response)

                } catch (e: ExposedSQLException) {
                    // 处理唯一约束冲突(例如用户名或邮箱已存在)
                    // PostgresSQL 唯一约束冲突的 SQL 状态码为 23505
                    when (e.getSQLState()) {
                        "23505" -> call.respond(HttpStatusCode.Conflict, "User with this username or email already exists.")
                        else -> {
                            // 其他数据库异常
                            call.respond(HttpStatusCode.InternalServerError, "Database error occurred.")
                        }
                    }
                }
            }
        }
    }
}