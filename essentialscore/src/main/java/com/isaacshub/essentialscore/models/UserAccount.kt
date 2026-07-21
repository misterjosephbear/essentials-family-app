package com.isaacshub.essentialscore.models

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    ADMIN,
    CHILD
}

@Serializable
data class UserAccount(
    val id: Long,
    val username: String,
    val role: UserRole,
    val adminId: Long?, // FK to admin user (null if this user is admin)
    val createdAt: Long // Unix timestamp
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: UserAccount
)

@Serializable
data class RegisterChildRequest(
    val username: String,
    val password: String,
    val adminToken: String
)
