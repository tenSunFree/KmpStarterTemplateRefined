package com.sun.kmpstartertemplaterefined.feature_auth_data.local

import com.sun.kmpstartertemplaterefined.feature_auth_data.secure_storage.SecureStorage
import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.UserSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

private const val SESSION_KEY = "auth_session"

class AuthSessionStorage(
    private val secureStorage: SecureStorage,
    private val json: Json,
) {
    private val mutex = Mutex()

    suspend fun saveSession(session: UserSession) = mutex.withLock {
        secureStorage.putString(SESSION_KEY, json.encodeToString(session))
    }

    suspend fun getSession(): UserSession? = mutex.withLock {
        secureStorage.getString(SESSION_KEY)?.let {
            runCatching { json.decodeFromString<UserSession>(it) }.getOrNull()
        }
    }

    suspend fun isLoggedIn(): Boolean = getSession() != null

    suspend fun updateTokens(token: String, refreshToken: String) = mutex.withLock {
        val current = secureStorage.getString(SESSION_KEY)?.let {
            runCatching { json.decodeFromString<UserSession>(it) }.getOrNull()
        } ?: return@withLock
        val updated = current.copy(token = token, refreshToken = refreshToken)
        secureStorage.putString(SESSION_KEY, json.encodeToString(updated))
    }

    suspend fun clearSession() = mutex.withLock {
        secureStorage.remove(SESSION_KEY)
    }
}