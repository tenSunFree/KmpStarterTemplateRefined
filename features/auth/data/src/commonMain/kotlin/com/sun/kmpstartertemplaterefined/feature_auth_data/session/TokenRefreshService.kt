package com.sun.kmpstartertemplaterefined.feature_auth_data.session

import com.sun.kmpstartertemplaterefined.feature_auth_data.local.AuthSessionStorage
import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.dto.LoginResponseDto
import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.dto.RefreshTokenRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TokenRefreshService(
    private val plainHttpClient: HttpClient,
    private val sessionStorage: AuthSessionStorage,
    private val baseUrl: String,
) {
    private val refreshMutex = Mutex()

    suspend fun refreshTokens(
        failedAccessToken: String?,
    ): RefreshedTokens? = refreshMutex.withLock {
        val currentSession = sessionStorage.getSession()
            ?: return@withLock null
        if (failedAccessToken != null && currentSession.token != failedAccessToken) {
            return@withLock RefreshedTokens(
                accessToken = currentSession.token,
                refreshToken = currentSession.refreshToken,
            )
        }
        val response = try {
            plainHttpClient.post("$baseUrl/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshTokenRequestDto(refreshToken = currentSession.refreshToken))
            }.body<LoginResponseDto>()
        } catch (e: CancellationException) {
            // Coroutine cancellation is not a real error; it must be rethrown and never swallowed.
            throw e
        } catch (e: ClientRequestException) {
            // A backend 401/403 means the refresh token is truly invalid or revoked (already used in rotation).
            // Only in this case do we clear the session and force logout.
            if (e.response.status == HttpStatusCode.Unauthorized ||
                e.response.status == HttpStatusCode.Forbidden
            ) {
                sessionStorage.clearSession()
            }
            return@withLock null
        } catch (e: Exception) {
            // Temporary issues such as timeout, disconnect, or JSON parsing failure.
            // Do not clear the session, so the same refresh token can be retried later.
            // This refresh attempt fails, and the caller (Ktor Auth Plugin) treats it as refresh failure,
            // allowing the original 401 to propagate.
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