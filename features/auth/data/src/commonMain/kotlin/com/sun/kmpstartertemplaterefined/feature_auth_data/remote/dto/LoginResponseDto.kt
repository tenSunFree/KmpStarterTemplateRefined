package com.sun.kmpstartertemplaterefined.feature_auth_data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponseDto(
    val status: Boolean? = null,
    val message: String? = null,
    val data: LoginDataDto? = null,
)

@Serializable
data class LoginDataDto(
    val id: String? = null,
    val username: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val gender: String? = null,
    @SerialName("role_id") val roleId: Int? = null,
    val token: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)