package com.isaacshub.essentialscore.models

import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.Serializable

@Serializable
data class Chore(
    val id: Long,
    val adminId: Long,
    val name: String,
    val description: String,
    val photoRequirement: String?, // Description of what photo should show (null if no photo required)
    val daysOfWeek: List<DayOfWeek>, // Days this chore is assigned
    val assignedChildIds: List<Long>, // Which children are assigned this chore
    val createdAt: Long // Unix timestamp
)

@Serializable
data class CreateChoreRequest(
    val name: String,
    val description: String,
    val photoRequirement: String?,
    val daysOfWeek: List<DayOfWeek>,
    val assignedChildIds: List<Long>
)

@Serializable
data class UpdateChoreRequest(
    val name: String?,
    val description: String?,
    val photoRequirement: String?,
    val daysOfWeek: List<DayOfWeek>?,
    val assignedChildIds: List<Long>?
)
