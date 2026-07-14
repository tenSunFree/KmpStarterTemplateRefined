package com.sun.kmpstartertemplaterefined.feature_auth_domain.models

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val id: String,
    val username: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val gender: String,
    val roleId: Int,
    val token: String,
    val refreshToken: String,
    val createdAt: String,
    val updatedAt: String?,  // This field is explicitly allowed to be empty; it is semantically defined as optional.
)