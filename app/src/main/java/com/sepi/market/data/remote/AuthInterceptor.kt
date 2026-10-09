package com.sepi.market.data.remote

import com.sepi.market.BuildConfig
import com.sepi.market.core.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor OkHttp untuk menyematkan header apikey dan Authorization Supabase ke setiap request.
 */
class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        val builder = original.newBuilder()

        // 1. Selalu sematkan apikey
        builder.header("apikey", BuildConfig.SUPABASE_KEY)

        // 2. Khusus endpoint auth token dan signup: JANGAN kirim Authorization pengguna
        val isAuthEndpoint = path.contains("auth/v1/token") || path.contains("auth/v1/signup")

        if (!isAuthEndpoint) {
            val userToken = sessionManager.accessToken
            if (!userToken.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $userToken")
            } else {
                // Fallback sebelum login: jika SUPABASE_KEY berformat JWT (diawali eyJ), kirim sebagai Bearer
                val key = BuildConfig.SUPABASE_KEY
                if (key.startsWith("eyJ")) {
                    builder.header("Authorization", "Bearer $key")
                }
                // Jika diawali sb_publishable_, jangan kirim sebagai Bearer
            }
        }

        return chain.proceed(builder.build())
    }
}
