package com.sun.kmpstartertemplaterefined.feature_auth_data.repositories

import com.sun.kmpstartertemplaterefined.feature_auth_data.local.AuthSessionStorage
import com.sun.kmpstartertemplaterefined.feature_auth_data.mappers.toDomain
import com.sun.kmpstartertemplaterefined.feature_auth_data.mappers.toDto
import com.sun.kmpstartertemplaterefined.feature_auth_data.mappers.toLoginResult
import com.sun.kmpstartertemplaterefined.feature_auth_data.mappers.toUserSession
import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.AuthRemoteDataSource
import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.dto.LoginRequestDto
import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.dto.SendOtpRequestDto
import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.dto.VerifyOtpRequestDto
import com.sun.kmpstartertemplaterefined.feature_auth_data.session.TokenRefreshService
import com.sun.kmpstartertemplaterefined.feature_auth_data.session.invalidateBearerTokenCache
import com.sun.kmpstartertemplaterefined.feature_auth_domain.exception.InvalidLoginResponseException
import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.LoginParams
import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.LoginResult
import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.RegisterParams
import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.RegisterResult
import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.UserSession
import com.sun.kmpstartertemplaterefined.feature_auth_domain.repositories.AuthRepository
import io.ktor.client.HttpClient

class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource,
    private val sessionStorage: AuthSessionStorage,
    private val tokenRefreshService: TokenRefreshService,
    private val authHttpClient: HttpClient,
) : AuthRepository {

    override suspend fun login(params: LoginParams): Result<LoginResult> = runCatching {
        val response = remoteDataSource.login(
            LoginRequestDto(email = params.email, password = params.password)
        )
        if (response.status != true) {
            throw InvalidLoginResponseException(
                response.message.orEmpty().ifBlank { "登入失敗" }
            )
        }
        val data = response.data
            ?: throw InvalidLoginResponseException("伺服器未回傳登入資料")
        val session = data.toUserSession()
        sessionStorage.saveSession(session)
        // Clear stale tokens cached inside authHttpClient to avoid reusing the previous user's token.
        authHttpClient.invalidateBearerTokenCache()
        session.toLoginResult()
    }

    override suspend fun register(params: RegisterParams): Result<RegisterResult> = runCatching {
        remoteDataSource.register(params.toDto()).toDomain()
    }

    override suspend fun sendOtp(email: String): Result<Unit> = runCatching {
        val response = remoteDataSource.sendOtp(SendOtpRequestDto(email = email))
        if (!response.status) error(response.message.ifBlank { "驗證碼發送失敗" })
    }

    override suspend fun verifyOtp(email: String, code: String): Result<Unit> = runCatching {
        val response = remoteDataSource.verifyOtp(VerifyOtpRequestDto(email = email, code = code))
        if (!response.status) error(response.message.ifBlank { "驗證碼錯誤" })
    }

    override suspend fun getSavedSession(): UserSession? = sessionStorage.getSession()

    override suspend fun logout(): Result<Unit> = runCatching {
        sessionStorage.clearSession()
        authHttpClient.invalidateBearerTokenCache()
    }

    // Manual refresh also uses the same TokenRefreshService.
    // This shares the same mutex and avoids racing with automatic refresh on 401.
    override suspend fun refreshToken(): Result<UserSession> = runCatching {
        tokenRefreshService.refreshTokens(failedAccessToken = null)
            ?: error("Token 刷新失敗，請重新登入")
        sessionStorage.getSession()
            ?: error("尚未登入，無法刷新 Token")
    }
}