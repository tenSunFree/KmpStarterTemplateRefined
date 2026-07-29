package com.sun.kmpstartertemplaterefined.feature_auth_data.di

import com.sun.kmpstartertemplaterefined.feature_auth_data.config.AuthConfig
import com.sun.kmpstartertemplaterefined.feature_auth_data.local.AuthSessionStorage
import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.AuthRemoteDataSource
import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.AuthRemoteDataSourceImpl
import com.sun.kmpstartertemplaterefined.feature_auth_data.repositories.AuthRepositoryImpl
import com.sun.kmpstartertemplaterefined.feature_auth_data.session.AuthSessionChecker
import com.sun.kmpstartertemplaterefined.feature_auth_data.session.TokenRefreshService
import com.sun.kmpstartertemplaterefined.feature_auth_data.util.HttpLogger
import com.sun.kmpstartertemplaterefined.feature_auth_domain.repositories.AuthRepository
import com.sun.kmpstartertemplaterefined.feature_core_domain.session.SessionChecker
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

val PlainHttpClientQualifier = named("plainHttpClient")
val AuthHttpClientQualifier = named("authHttpClient")

fun authDataModule(authConfig: AuthConfig) = module {

    includes(secureStoragePlatformModule())

    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }

    single { AuthSessionStorage(secureStorage = get(), json = get()) }

    // Client without Authorization: login / register / otp / refresh
    single(PlainHttpClientQualifier) {
        createBaseHttpClient(json = get())
    }

    single {
        TokenRefreshService(
            plainHttpClient = get(PlainHttpClientQualifier),
            sessionStorage = get(),
            baseUrl = authConfig.baseUrl,
        )
    }

    // Client that automatically attaches Bearer tokens: all protected APIs
    single(AuthHttpClientQualifier) {
        val sessionStorage: AuthSessionStorage = get()
        val tokenRefreshService: TokenRefreshService = get()

        createBaseHttpClient(json = get()).config {
            install(Auth) {
                bearer {
                    loadTokens {
                        sessionStorage.getSession()?.let {
                            BearerTokens(accessToken = it.token, refreshToken = it.refreshToken)
                        }
                    }

                    sendWithoutRequest { request ->
                        !request.url.encodedPath.contains("/auth/")
                    }

                    refreshTokens {
                        val refreshed = runCatching {
                            tokenRefreshService.refreshTokens(
                                failedAccessToken = oldTokens?.accessToken,
                            )
                        }.getOrNull()

                        refreshed?.let {
                            BearerTokens(
                                accessToken = it.accessToken,
                                refreshToken = it.refreshToken
                            )
                        }
                        // When null is returned, Ktor rethrows the original 401.
                        // Let upper layers (ViewModel / UseCase) decide whether to navigate to login.
                        // Do not call tokenRefreshService.clearSession() here;
                        // TokenRefreshService.refreshTokens() already handles this internally.
                    }
                }
            }
        }
    }

    single<AuthRemoteDataSource> {
        AuthRemoteDataSourceImpl(
            httpClient = get(PlainHttpClientQualifier),
            baseUrl = authConfig.baseUrl,
        )
    }

    single<AuthRepository> {
        AuthRepositoryImpl(
            remoteDataSource = get(),
            sessionStorage = get(),
            tokenRefreshService = get(),
            authHttpClient = get(AuthHttpClientQualifier), // Used only to call invalidateBearerTokenCache()
        )
    }

    single<SessionChecker> {
        AuthSessionChecker(sessionStorage = get(), tokenRefreshService = get())
    }
}

private fun createBaseHttpClient(json: Json): HttpClient = HttpClient {
    expectSuccess = true

    install(ContentNegotiation) { json(json) }

    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 15_000
    }

    install(Logging) {
        logger = HttpLogger
        level = LogLevel.HEADERS
        sanitizeHeader { header -> header == HttpHeaders.Authorization }
    }
}