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
    val user = data?.user ?: error("註冊成功但沒有回傳使用者資料")
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

// reuse toUserSession() directly in toDomain() to avoid writing the validation logic twice.
fun LoginDataDto.toDomain(): LoginResult {
    val session = this.toUserSession()
    return LoginResult(
        userId = session.id,
        username = session.username,
        email = session.email,
        token = session.token,
        refreshToken = session.refreshToken,
        fullName = session.fullName
    )
}

fun LoginDataDto.toUserSession(): UserSession {
    val validId = requireField(id, "id")
    val validToken = requireField(token, "token")
    val validRefreshToken = requireField(refreshToken, "refresh_token")

    return UserSession(
        id = validId,
        username = username.orEmpty(),
        fullName = fullName.orEmpty(),
        email = email.orEmpty(),
        phone = phone.orEmpty(),
        gender = gender.orEmpty(),
        roleId = roleId ?: 0,
        token = validToken,
        refreshToken = validRefreshToken,
        createdAt = createdAt.orEmpty(),
        updatedAt = updatedAt,
    )
}

private fun requireField(value: String?, fieldName: String): String {
    if (value.isNullOrBlank()) {
        throw InvalidLoginResponseException("登入回應缺少必要欄位：$fieldName")
    }
    return value
}