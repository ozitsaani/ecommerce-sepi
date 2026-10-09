package com.sepi.market.data.remote

import com.google.gson.Gson
import com.sepi.market.BuildConfig
import com.sepi.market.core.SessionManager
import com.sepi.market.data.remote.dto.AuthResponse
import com.sepi.market.data.remote.dto.RefreshTokenRequest
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Otomatis memperbarui access token ketika menerima respons HTTP 401 (PRD Bagian 7.2).
 */
class TokenAuthenticator(
    private val sessionManager: SessionManager,
    private val gson: Gson
) : Authenticator {

    // OkHttpClient terpisah tanpa authenticator untuk mencegah infinite loop
    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun authenticate(route: Route?, response: Response): Request? {
        // 1. Cegah loop tak berujung (maksimal 2 kali percobaan ulang)
        if (responseCount(response) >= 3) {
            Timber.w("TokenAuthenticator: Loop terdeteksi, batalkan retry")
            sessionManager.notifySessionExpired()
            return null
        }

        val storedRefreshToken = sessionManager.refreshToken
        if (storedRefreshToken.isNullOrBlank()) {
            Timber.w("TokenAuthenticator: Tidak ada refresh token tersimpan")
            sessionManager.notifySessionExpired()
            return null
        }

        // Ambil token dari request yang gagal
        val failedAuthorization = response.request.header("Authorization")
        val failedToken = failedAuthorization?.removePrefix("Bearer ")?.trim()

        synchronized(this) {
            val currentToken = sessionManager.accessToken

            // Bila token tersimpan sudah diperbarui oleh thread lain, langsung ulangi dengan token baru
            if (!currentToken.isNullOrBlank() && currentToken != failedToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            // Lakukan panggilan refresh token ke server GoTrue
            val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/') + "/"
            val refreshUrl = "${baseUrl}auth/v1/token?grant_type=refresh_token"
            val requestBodyJson = gson.toJson(RefreshTokenRequest(storedRefreshToken))

            val refreshRequest = Request.Builder()
                .url(refreshUrl)
                .header("apikey", BuildConfig.SUPABASE_KEY)
                .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            return try {
                val refreshResponse = refreshClient.newCall(refreshRequest).execute()
                if (refreshResponse.isSuccessful) {
                    val responseBody = refreshResponse.body?.string()
                    val authData = gson.fromJson(responseBody, AuthResponse::class.java)

                    val newAccessToken = authData?.accessToken
                    val newRefreshToken = authData?.refreshToken ?: storedRefreshToken

                    if (!newAccessToken.isNullOrBlank()) {
                        sessionManager.updateTokens(newAccessToken, newRefreshToken)
                        Timber.d("TokenAuthenticator: Refresh token sukses")

                        response.request.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .build()
                    } else {
                        Timber.e("TokenAuthenticator: Token baru kosong")
                        sessionManager.notifySessionExpired()
                        null
                    }
                } else {
                    Timber.e("TokenAuthenticator: Refresh gagal dengan HTTP ${refreshResponse.code}")
                    sessionManager.notifySessionExpired()
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "TokenAuthenticator: Gagal mengeksekusi refresh token")
                sessionManager.notifySessionExpired()
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
