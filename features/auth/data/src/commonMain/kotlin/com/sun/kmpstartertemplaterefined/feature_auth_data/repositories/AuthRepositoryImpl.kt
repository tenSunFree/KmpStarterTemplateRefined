package com.sun.kmpstartertemplaterefined.feature_auth_data.repositories

import com.sun.kmpstartertemplaterefined.feature_auth_data.local.AuthSessionStorage
import com.sun.kmpstartertemplaterefined.feature_auth_data.mappers.toDomain
import com.sun.kmpstartertemplaterefined.feature_auth_data.mappers.toDto
import com.sun.kmpstartertemplaterefined.feature_auth_data.mappers.toUserSession
import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.AuthRemoteDataSource
import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.dto.LoginRequestDto
import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.dto.RefreshTokenRequestDto
import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.dto.SendOtpRequestDto
import com.sun.kmpstartertemplaterefined.feature_auth_data.remote.dto.VerifyOtpRequestDto
import com.sun.kmpstartertemplaterefined.feature_auth_domain.exception.InvalidLoginResponseException
import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.LoginParams
import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.LoginResult
import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.RegisterParams
import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.RegisterResult
import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.UserSession
import com.sun.kmpstartertemplaterefined.feature_auth_domain.repositories.AuthRepository
import io.ktor.client.plugins.ClientRequestException

class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource,
    private val sessionStorage: AuthSessionStorage,
) : AuthRepository {

    override suspend fun login(params: LoginParams): Result<LoginResult> =
        try {
            val response = remoteDataSource.login(
                LoginRequestDto(email = params.email, password = params.password)
            )
            if (response.status != true) {
                Result.failure(Exception(response.message.orEmpty().ifBlank { "登入失敗" }))
            } else {
                val data = response.data
                    ?: return Result.failure(InvalidLoginResponseException("伺服器未回傳登入資料"))

                val session = data.toUserSession()
                sessionStorage.saveSession(session)
                Result.success(data.toDomain())
            }
        } catch (e: InvalidLoginResponseException) {
            Result.failure(e)
        } catch (e: ClientRequestException) {
            val message = when (e.response.status.value) {
                400 -> "請求格式錯誤"
                401 -> "Email 或密碼錯誤"
                403 -> "帳號尚未啟用，請先完成信箱驗證"
                422 -> "輸入格式不正確"
                else -> "登入失敗，請稍後再試"
            }
            Result.failure(Exception(message))
        } catch (_: Exception) {
            Result.failure(Exception("網路連線異常，請確認網路後再試"))
        }

    override suspend fun getSavedSession(): UserSession? =
        sessionStorage.getSession()

    override suspend fun refreshToken(): Result<UserSession> = runCatching {
        val old = sessionStorage.getSession()
            ?: error("尚未登入，無法刷新 Token")
        val response = remoteDataSource.refreshToken(
            RefreshTokenRequestDto(refreshToken = old.refreshToken)
        )
        if (response.status != true) error(response.message.orEmpty().ifBlank { "Token 刷新失敗" })
        val data = response.data ?: error("刷新回傳無資料")
        val newSession = data.toUserSession()   // toUserSession() 里已经验证过 token/refreshToken 非空
        sessionStorage.updateTokens(
            token = newSession.token,
            refreshToken = newSession.refreshToken,
        )
        newSession
    }

    override suspend fun logout() {
        sessionStorage.clearSession()
    }

    override suspend fun register(params: RegisterParams): Result<RegisterResult> =
        runCatching {
            val response = remoteDataSource.register(params.toDto())
            if (!response.status) error(response.message.ifBlank { "註冊失敗" })
            response.toDomain()
        }

    override suspend fun sendOtp(email: String): Result<Unit> =
        runCatching {
            val response = remoteDataSource.sendOtp(SendOtpRequestDto(email = email))
            if (!response.status) error(response.message.ifBlank { "驗證碼發送失敗" })
        }

    override suspend fun verifyOtp(email: String, code: String): Result<Unit> =
        runCatching {
            val response =
                remoteDataSource.verifyOtp(VerifyOtpRequestDto(email = email, code = code))
            if (!response.status) error(response.message.ifBlank { "驗證碼驗證失敗" })
        }
}