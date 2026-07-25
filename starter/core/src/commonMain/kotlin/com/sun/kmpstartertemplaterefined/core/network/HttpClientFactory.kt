package com.sun.kmpstartertemplaterefined.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

interface TokenStore {
    suspend fun accessToken(): String?
    suspend fun refreshToken(): String?
    suspend fun saveTokens(access: String, refresh: String)
    suspend fun clear()
}

fun createHttpClient(
    tokenStore: TokenStore,
    baseHost: String = "localhost:8080",
    onUnauthorized: suspend () -> Unit = {},
): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
    }
    install(Auth) {
        bearer {
            loadTokens {
                val access = tokenStore.accessToken() ?: return@loadTokens null
                val refresh = tokenStore.refreshToken() ?: ""
                BearerTokens(access, refresh)
            }
            refreshTokens {
                // TODO: 呼叫你的 /auth/refresh API 拿新 token
                // 這裡先示意流程，實際用 client 呼叫 refresh endpoint
                val newAccess = tokenStore.accessToken()
                if (newAccess == null) {
                    onUnauthorized()
                    null
                } else {
                    BearerTokens(newAccess, tokenStore.refreshToken() ?: "")
                }
            }
        }
    }
    defaultRequest {
        url {
            protocol = URLProtocol.HTTP
            host = baseHost
            path("api/v1/")
        }
    }
}