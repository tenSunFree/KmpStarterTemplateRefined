package com.sun.kmpstartertemplaterefined.feature_auth_data.session

import com.sun.kmpstartertemplaterefined.feature_auth_data.local.AuthSessionStorage
import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.dto.LoginResponseDto
import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.dto.RefreshTokenRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TokenRefreshService(
    private val plainHttpClient: HttpClient,
    private val sessionStorage: AuthSessionStorage,
    private val baseUrl: String,
) {
    // Prevent multiple 401s from triggering repeated refresh calls during Refresh Token Rotation.
    private val refreshMutex = Mutex()

    suspend fun refreshTokens(
        failedAccessToken: String?,
    ): RefreshedTokens? = refreshMutex.withLock {
        val currentSession = sessionStorage.getSession()
            ?: return@withLock null
        // Another request may have already refreshed tokens; return the latest values without another API call.
        if (failedAccessToken != null && currentSession.token != failedAccessToken) {
            return@withLock RefreshedTokens(
                accessToken = currentSession.token,
                refreshToken = currentSession.refreshToken,
            )
        }
        val response = runCatching {
            plainHttpClient.post("$baseUrl/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshTokenRequestDto(refreshToken = currentSession.refreshToken))
            }.body<LoginResponseDto>()
        }.getOrElse {
            sessionStorage.clearSession()
            return@withLock null
        }
        if (response.status != true) {
            sessionStorage.clearSession()
            return@withLock null
        }
        val data = response.data ?: run {
            sessionStorage.clearSession()
            return@withLock null
        }
        val newAccessToken = data.token?.takeIf(String::isNotBlank) ?: run {
            sessionStorage.clearSession()
            return@withLock null
        }
        val newRefreshToken = data.refreshToken?.takeIf(String::isNotBlank) ?: run {
            sessionStorage.clearSession()
            return@withLock null
        }
        sessionStorage.updateTokens(token = newAccessToken, refreshToken = newRefreshToken)
        RefreshedTokens(accessToken = newAccessToken, refreshToken = newRefreshToken)
    }

    suspend fun clearSession() {
        sessionStorage.clearSession()
    }
}

data class RefreshedTokens(
    val accessToken: String,
    val refreshToken: String,
)