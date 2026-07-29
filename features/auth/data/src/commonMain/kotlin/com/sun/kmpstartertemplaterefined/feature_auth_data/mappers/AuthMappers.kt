package com.sun.kmpstartertemplaterefined.feature_auth_data.mappers

import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.dto.LoginDataDto
import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.dto.RegisterRequestDto
import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.dto.RegisterResponseDto
import com.sun.kmpstartertemplaterefined.feature_auth_domain.exception.InvalidLoginResponseException
import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.LoginResult
import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.RegisterParams
import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.RegisterResult
import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.UserSession

fun RegisterParams.toDto() = RegisterRequestDto(
    email = email,
    fullName = fullName,
    gender = gender,
    password = password,
    phone = phone,
    username = username,
)

fun RegisterResponseDto.toDomain(): RegisterResult {
    val user = data?.user ?: throw InvalidLoginResponseException("註冊成功但沒有回傳使用者資料")
    return RegisterResult(
        userId = user.id,
        username = user.username,
        email = user.email,
        fullName = user.fullName,
        phone = user.phone,
        gender = user.gender,
        message = message,
    )
}

/**
 * Convert to a full UserSession for persistence in AuthSessionStorage.
 * Field mapping must match UserSession exactly:
 * id, username, fullName, email, phone, gender, roleId, token, refreshToken, createdAt, updatedAt
 */
fun LoginDataDto.toUserSession(): UserSession = UserSession(
    id = id.orEmpty(),
    username = username.orEmpty(),
    fullName = fullName.orEmpty(),
    email = email.orEmpty(),
    phone = phone.orEmpty(),
    gender = gender.orEmpty(),
    roleId = roleId ?: 0,
    token = requireField(token, "token"),
    refreshToken = requireField(refreshToken, "refresh_token"),
    createdAt = createdAt.orEmpty(),
    updatedAt = updatedAt,
)

/**
 * Compact result used by LoginLogic / LoginViewModel, converted directly
 * from a validated UserSession to avoid duplicating token validation logic.
 */
fun UserSession.toLoginResult(): LoginResult = LoginResult(
    userId = id,
    username = username,
    fullName = fullName,
    email = email,
    token = token,
    refreshToken = refreshToken,
)

private fun requireField(value: String?, fieldName: String): String =
    value?.takeIf(String::isNotBlank)
        ?: throw InvalidLoginResponseException("登入回應缺少必要欄位：$fieldName")