package com.sepi.market.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Pengelola sesi pengguna berbasis SharedPreferences.
 * Menyimpan token autentikasi Supabase, identitas user, dan memancarkan event sessionExpired saat token kedaluwarsa.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    val accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)

    val refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)

    val userId: String?
        get() = prefs.getString(KEY_USER_ID, null)

    val userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)

    val userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)

    val userPhone: String?
        get() = prefs.getString(KEY_USER_PHONE, null)

    val isLoggedIn: Boolean
        get() = !accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()

    /**
     * Menyimpan sesi login lengkap dari response GoTrue auth.
     */
    fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        email: String,
        fullName: String
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, fullName)
            .apply()
    }

    /**
     * Memperbarui token akses dan refresh token baru setelah silent refresh.
     */
    fun updateTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    /**
     * Memperbarui profil lokal pengguna saat diubah.
     */
    fun updateProfile(fullName: String, phone: String?) {
        prefs.edit()
            .putString(KEY_USER_NAME, fullName)
            .putString(KEY_USER_PHONE, phone)
            .apply()
    }

    /**
     * Menghapus seluruh data sesi lokal saat keluar.
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }

    /**
     * Memancarkan event sesi berakhir agar UI dapat mengarahkan pengguna kembali ke AuthActivity.
     */
    fun notifySessionExpired() {
        clearSession()
        _sessionExpired.tryEmit(Unit)
    }

    companion object {
        private const val PREF_NAME = "sepi_session_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHONE = "user_phone"
    }
}
