package com.isaacshub.essentialscore.api

import com.isaacshub.essentialscore.models.*

/**
 * Interface defining all Essentials API endpoints.
 * This is implemented by HTTP clients in both Isaac's Hub and the standalone app.
 */
interface EssentialsApi {

    // Authentication
    suspend fun login(request: LoginRequest): Result<LoginResponse>
    suspend fun registerChild(request: RegisterChildRequest): Result<UserAccount>

    // Chore Management (Admin)
    suspend fun getChores(token: String): Result<List<Chore>>
    suspend fun createChore(token: String, request: CreateChoreRequest): Result<Chore>
    suspend fun updateChore(token: String, choreId: Long, request: UpdateChoreRequest): Result<Chore>
    suspend fun deleteChore(token: String, choreId: Long): Result<Unit>

    // Child Accounts (Admin)
    suspend fun getChildAccounts(token: String): Result<List<UserAccount>>

    // Chore Completion (Child)
    suspend fun getTodayChores(token: String, userId: Long): Result<TodayChoresResponse>
    suspend fun completeChore(token: String, request: CompleteChoreRequest): Result<ChoreCompletion>
    suspend fun verifyPhoto(token: String, request: VerifyPhotoRequest): Result<VerifyPhotoResponse>

    // Admin Review
    suspend fun getPendingCompletions(token: String): Result<List<ChoreCompletion>>
    suspend fun adminOverride(token: String, request: AdminOverrideRequest): Result<ChoreCompletion>
}

/**
 * API endpoint paths as constants.
 */
object EssentialsEndpointPaths {
    const val BASE_PATH = "/api/essentials"

    // Auth
    const val LOGIN = "$BASE_PATH/auth/login"
    const val REGISTER_CHILD = "$BASE_PATH/auth/register-child"

    // Chores
    const val CHORES = "$BASE_PATH/chores"
    fun choreById(id: Long) = "$CHORES/$id"
    fun completeChore(id: Long) = "$CHORES/$id/complete"
    fun verifyPhoto(id: Long) = "$CHORES/$id/verify-photo"
    fun adminOverride(id: Long) = "$CHORES/$id/admin-override"

    // Family
    const val CHILDREN = "$BASE_PATH/children"
    fun todayChores(userId: Long) = "$BASE_PATH/family/$userId/today"

    // Admin
    const val PENDING_COMPLETIONS = "$BASE_PATH/admin/pending"
}
