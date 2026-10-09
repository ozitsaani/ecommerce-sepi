package com.sepi.market.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request body untuk pendaftaran akun baru: POST auth/v1/signup
 */
data class SignUpRequest(
    val email: String,
    val password: String,
    val data: Map<String, String> = emptyMap()
)

/**
 * Request body untuk masuk akun: POST auth/v1/token?grant_type=password
 */
data class SignInRequest(
    val email: String,
    val password: String
)

/**
 * Request body untuk refresh token: POST auth/v1/token?grant_type=refresh_token
 */
data class RefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

/**
 * Respon dari endpoint autentikasi Supabase GoTrue.
 */
data class AuthResponse(
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("expires_in") val expiresIn: Long? = null,
    @SerializedName("expires_at") val expiresAt: Long? = null,
    val user: AuthUserDto? = null
)

/**
 * DTO identitas user dalam AuthResponse.
 */
data class AuthUserDto(
    val id: String,
    val email: String? = null,
    @SerializedName("user_metadata") val userMetadata: Map<String, Any>? = null
)
