package com.sun.kmpstartertemplaterefined.feature_auth_data.session

import com.sun.kmpstartertemplaterefined.feature_auth_data.local.AuthSessionStorage
import com.sun.kmpstartertemplaterefined.feature_core_domain.session.SessionChecker

class AuthSessionChecker(
    private val sessionStorage: AuthSessionStorage,
    private val tokenRefreshService: TokenRefreshService,
) : SessionChecker {

    override suspend fun isLoggedIn(): Boolean =
        sessionStorage.isLoggedIn()

    override suspend fun tryRefreshToken(): Boolean {
        val currentSession = sessionStorage.getSession() ?: return false
        val refreshed = tokenRefreshService.refreshTokens(
            failedAccessToken = currentSession.token,
        )
        return refreshed != null
    }

    override suspend fun clearSession() {
        sessionStorage.clearSession()
    }
}