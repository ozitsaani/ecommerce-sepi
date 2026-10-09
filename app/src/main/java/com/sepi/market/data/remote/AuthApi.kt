package com.sepi.market.data.remote

import com.sepi.market.data.remote.dto.AuthResponse
import com.sepi.market.data.remote.dto.RefreshTokenRequest
import com.sepi.market.data.remote.dto.SignInRequest
import com.sepi.market.data.remote.dto.SignUpRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Interface Retrofit untuk endpoint autentikasi Supabase GoTrue (auth/v1/...).
 */
interface AuthApi {

    /**
     * Mendaftar akun baru dengan email, kata sandi, dan metadata full_name.
     */
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Body request: SignUpRequest
    ): AuthResponse

    /**
     * Masuk akun dengan email dan kata sandi.
     */
    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Body request: SignInRequest
    ): AuthResponse

    /**
     * Memperbarui access token menggunakan refresh token.
     */
    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): AuthResponse

    /**
     * Keluar dari sesi di server (best-effort).
     */
    @POST("auth/v1/logout")
    suspend fun logout(): Response<Unit>
}
