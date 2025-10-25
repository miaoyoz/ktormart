package com.ktormart.userservice.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.Table

/**
 * 用户表定义（Exposed ORM）
 */
object Users : Table() {
    val id = uuid("id").autoGenerate()
    val username = varchar("username", 255).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)

    override val primaryKey = PrimaryKey(id)
}

/**
 * 用户注册请求 DTO（数据传输对象）
 *
 * @property username 用户名
 * @property email 邮箱
 * @property password 密码（明文，仅用于传输）
 */
@Serializable
data class UserRegisterRequest(val username: String, val email: String, val password: String)

/**
 * 用户响应 DTO（数据传输对象）
 *
 * @property id 用户 ID
 * @property username 用户名
 * @property email 邮箱
 */
@Serializable
data class UserResponse(val id: String, val username: String, val email: String)