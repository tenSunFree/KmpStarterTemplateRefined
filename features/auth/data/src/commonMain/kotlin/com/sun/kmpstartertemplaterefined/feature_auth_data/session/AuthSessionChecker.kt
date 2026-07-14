package com.sun.kmpstartertemplaterefined.feature_auth_data.session

import com.sun.kmpstartertemplaterefined.feature_auth_data.local.AuthSessionStorage
import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.AuthRemoteDataSource
import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.dto.RefreshTokenRequestDto
import com.sun.kmpstartertemplaterefined.feature_core_domain.session.SessionChecker

class AuthSessionChecker(
    private val sessionStorage: AuthSessionStorage,
    private val remoteDataSource: AuthRemoteDataSource,
) : SessionChecker {

    override suspend fun isLoggedIn(): Boolean =
        sessionStorage.isLoggedIn()

    override suspend fun tryRefreshToken(): Boolean {
        val session = sessionStorage.getSession() ?: return false
        return runCatching {
            val response = remoteDataSource.refreshToken(
                RefreshTokenRequestDto(refreshToken = session.refreshToken)
            )
            if (response.status != true) error("refresh failed")
            val data = response.data ?: error("no data")
            val newToken = data.token?.takeIf { it.isNotBlank() } ?: error("missing token")
            val newRefreshToken =
                data.refreshToken?.takeIf { it.isNotBlank() } ?: error("missing refresh_token")
            // Only the token pair is updated; user information remains unchanged.
            sessionStorage.updateTokens(
                token = newToken,
                refreshToken = newRefreshToken,
            )
        }.isSuccess
    }

    override suspend fun clearSession() {
        sessionStorage.clearSession()
    }
}