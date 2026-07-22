package com.isaacshub.essentialscore.models

import kotlinx.serialization.Serializable

@Serializable
data class PhotoVerificationResult(
    val matches: Boolean,
    val confidence: Int, // 0-100
    val description: String, // What Claude saw in the image
    val expectedDescription: String, // What was expected
    val verifiedAt: Long // Unix timestamp
)

@Serializable
data class VerifyPhotoRequest(
    val choreId: Long,
    val photoBase64: String
)

@Serializable
data class VerifyPhotoResponse(
    val result: PhotoVerificationResult,
    val approved: Boolean // true if confidence is high enough
)
