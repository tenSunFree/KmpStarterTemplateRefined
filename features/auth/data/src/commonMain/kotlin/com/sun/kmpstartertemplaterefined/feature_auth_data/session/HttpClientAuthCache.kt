package com.sun.kmpstartertemplaterefined.feature_auth_data.session

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider

/**
 * Ktor's Auth(bearer) plugin caches the result returned by `loadTokens()` in memory,
 * and does not proactively re-read `AuthSessionStorage` afterward.
 *
 * So after each successful login or logout, call this function to force Ktor
 * to invoke `loadTokens()` again on the next request.
 * Otherwise, there is a risk of reusing stale tokens from the previous user.
 */
fun HttpClient.invalidateBearerTokenCache() {
    authProvider<BearerAuthProvider>()?.clearToken()
}