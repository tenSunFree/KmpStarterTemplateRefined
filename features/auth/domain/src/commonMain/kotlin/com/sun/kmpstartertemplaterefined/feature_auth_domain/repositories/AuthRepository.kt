package com.sun.kmpstartertemplaterefined.feature_auth_domain.repositories

import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.LoginParams
import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.LoginResult
import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.RegisterParams
import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.RegisterResult
import com.sun.kmpstartertemplaterefined.feature_auth_domain.models.UserSession

interface AuthRepository {
    suspend fun login(params: LoginParams): Result<LoginResult>
    suspend fun register(params: RegisterParams): Result<RegisterResult>
    suspend fun sendOtp(email: String): Result<Unit>
    suspend fun verifyOtp(email: String, code: String): Result<Unit>
    suspend fun getSavedSession(): UserSession?
    suspend fun refreshToken(): Result<UserSession>
    suspend fun logout(): Result<Unit>
}