package com.sepi.market.core

import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Pengurai pesan error dari respon HTTP PostgREST dan GoTrue Supabase ke pesan berbahasa Indonesia yang ramah pengguna.
 */
object ErrorParser {

    /**
     * Parsing IOException jaringan (koneksi terputus, timeout, host tidak ditemukan).
     */
    fun parseException(e: IOException): String {
        return when (e) {
            is UnknownHostException, is SocketTimeoutException ->
                "Tidak ada koneksi internet. Periksa jaringanmu lalu coba lagi."
            else ->
                "Tidak ada koneksi internet. Periksa jaringanmu lalu coba lagi."
        }
    }

    /**
     * Parsing HttpException menjadi pasangan (Pesan, Kind).
     */
    fun parseHttpException(e: HttpException): Pair<String, ApiResult.Kind> {
        val statusCode = e.code()
        val errorBody = try {
            e.response()?.errorBody()?.string()
        } catch (_: Exception) {
            null
        }

        if (errorBody.isNullOrBlank()) {
            return when {
                statusCode in 500..599 -> "Server sedang bermasalah, coba beberapa saat lagi." to ApiResult.Kind.SERVER
                statusCode == 401 -> "Sesi berakhir, silakan masuk lagi." to ApiResult.Kind.AUTH
                statusCode == 403 -> "Kamu tidak punya izin untuk aksi ini." to ApiResult.Kind.FORBIDDEN
                else -> "Terjadi kesalahan, coba lagi." to ApiResult.Kind.UNKNOWN
            }
        }

        return try {
            val json = JsonParser.parseString(errorBody).asJsonObject

            // 1. Cek Auth GoTrue Supabase (error_code, msg, error_description, message)
            val authErrorCode = json.get("error_code")?.asString
                ?: json.get("error")?.asString
                ?: ""
            val authMsg = json.get("msg")?.asString
                ?: json.get("error_description")?.asString
                ?: json.get("message")?.asString
                ?: ""

            val combinedAuth = "$authErrorCode $authMsg".lowercase()
            if (combinedAuth.contains("invalid_credentials") || combinedAuth.contains("invalid login credentials")) {
                return "Email atau kata sandi salah" to ApiResult.Kind.AUTH
            }
            if (combinedAuth.contains("user_already_exists") || combinedAuth.contains("already registered")) {
                return "Email sudah terdaftar" to ApiResult.Kind.VALIDATION
            }
            if (combinedAuth.contains("weak_password") || combinedAuth.contains("password should be at least")) {
                return "Kata sandi minimal 6 karakter" to ApiResult.Kind.VALIDATION
            }
            if (combinedAuth.contains("rate_limit") || combinedAuth.contains("over_email_send_rate_limit")) {
                return "Terlalu banyak percobaan, coba lagi beberapa saat" to ApiResult.Kind.SERVER
            }

            // 2. Cek PostgREST (code, message, details, hint)
            val code = json.get("code")?.asString ?: ""
            val message = json.get("message")?.asString ?: ""
            val details = json.get("details")?.asString ?: ""
            val combined = "$message $details".lowercase()

            when {
                // HTTP 400 dengan code = P0001 (pesan dari RAISE EXCEPTION PostgreSQL)
                statusCode == 400 && code == "P0001" -> {
                    message.ifBlank { "Data yang dimasukkan tidak valid." } to ApiResult.Kind.VALIDATION
                }
                // HTTP 403 atau code 42501 (RLS violation)
                statusCode == 403 || code == "42501" -> {
                    if (combined.contains("review") || combined.contains("ulasan")) {
                        "Ulasan hanya bisa ditulis setelah pesananmu diterima" to ApiResult.Kind.FORBIDDEN
                    } else {
                        "Kamu tidak punya izin untuk aksi ini" to ApiResult.Kind.FORBIDDEN
                    }
                }
                // HTTP 409 atau code 23505 (Unique violation)
                statusCode == 409 || code == "23505" -> {
                    if (combined.contains("review") || combined.contains("uq_reviews_one_per_user")) {
                        "Kamu sudah memberi ulasan untuk produk ini" to ApiResult.Kind.VALIDATION
                    } else {
                        "Data sudah ada" to ApiResult.Kind.VALIDATION
                    }
                }
                statusCode == 401 -> {
                    "Sesi berakhir, silakan masuk lagi" to ApiResult.Kind.AUTH
                }
                statusCode in 500..599 -> {
                    "Server sedang bermasalah, coba beberapa saat lagi." to ApiResult.Kind.SERVER
                }
                message.isNotBlank() -> {
                    message to ApiResult.Kind.UNKNOWN
                }
                else -> {
                    "Terjadi kesalahan, coba lagi." to ApiResult.Kind.UNKNOWN
                }
            }
        } catch (_: Exception) {
            when {
                statusCode in 500..599 -> "Server sedang bermasalah, coba beberapa saat lagi." to ApiResult.Kind.SERVER
                statusCode == 401 -> "Sesi berakhir, silakan masuk lagi." to ApiResult.Kind.AUTH
                else -> "Terjadi kesalahan, coba lagi." to ApiResult.Kind.UNKNOWN
            }
        }
    }
}
