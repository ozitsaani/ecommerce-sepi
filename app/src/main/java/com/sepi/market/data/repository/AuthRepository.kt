package com.sepi.market.data.repository

import com.sepi.market.core.ApiResult
import com.sepi.market.core.SessionManager
import com.sepi.market.core.safeCall
import com.sepi.market.data.remote.AuthApi
import com.sepi.market.data.remote.dto.AuthResponse
import com.sepi.market.data.remote.dto.SignInRequest
import com.sepi.market.data.remote.dto.SignUpRequest

/**
 * Repository untuk alur autentikasi pengguna (Daftar, Masuk, Keluar, dan Sesi).
 */
class AuthRepository(
    private val authApi: AuthApi,
    val sessionManager: SessionManager
) {

    /**
     * Mendaftar akun baru dan menyimpan sesi jika access_token langsung dikembalikan.
     */
    suspend fun signUp(name: String, email: String, pass: String): ApiResult<AuthResponse> {
        return safeCall {
            val response = authApi.signUp(
                SignUpRequest(
                    email = email.trim(),
                    password = pass,
                    data = mapOf("full_name" to name.trim())
                )
            )
            // Jika token langsung dikembalikan (confirm email nonaktif), simpan sesi
            val token = response.accessToken
            val refresh = response.refreshToken
            val user = response.user
            if (!token.isNullOrBlank() && !refresh.isNullOrBlank() && user != null) {
                sessionManager.saveSession(
                    accessToken = token,
                    refreshToken = refresh,
                    userId = user.id,
                    email = user.email ?: email,
                    fullName = name.trim()
                )
            }
            response
        }
    }

    /**
     * Masuk ke akun dan menyimpan token serta data profil lokal.
     */
    suspend fun signIn(email: String, pass: String): ApiResult<AuthResponse> {
        return safeCall {
            val response = authApi.signIn(
                SignInRequest(
                    email = email.trim(),
                    password = pass
                )
            )
            val token = response.accessToken
            val refresh = response.refreshToken
            val user = response.user
            if (!token.isNullOrBlank() && !refresh.isNullOrBlank() && user != null) {
                val fullName = user.userMetadata?.get("full_name")?.toString()
                    ?: email.substringBefore("@")
                sessionManager.saveSession(
                    accessToken = token,
                    refreshToken = refresh,
                    userId = user.id,
                    email = user.email ?: email,
                    fullName = fullName
                )
            }
            response
        }
    }

    /**
     * Keluar dari akun: panggil API logout (best-effort) dan selalu bersihkan sesi lokal.
     */
    suspend fun logout(): ApiResult<Unit> {
        return try {
            authApi.logout()
            sessionManager.clearSession()
            ApiResult.Success(Unit)
        } catch (_: Exception) {
            sessionManager.clearSession()
            ApiResult.Success(Unit)
        }
    }
}
