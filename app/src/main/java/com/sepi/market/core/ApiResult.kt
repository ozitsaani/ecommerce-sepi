package com.sepi.market.core

import retrofit2.HttpException
import java.io.IOException

/**
 * Representasi hasil operasi API: Sukses dengan data [data] atau Error dengan pesan [message] dan jenis [kind].
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val message: String, val kind: Kind = Kind.UNKNOWN) : ApiResult<Nothing>

    enum class Kind {
        NETWORK,
        AUTH,
        VALIDATION,
        FORBIDDEN,
        SERVER,
        UNKNOWN
    }
}

/**
 * Helper suspend function untuk membungkus panggilan Retrofit ke dalam ApiResult.
 * Menangkap IOException dan HttpException secara otomatis lalu mem-parsing pesannya lewat ErrorParser.
 */
suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (e: IOException) {
        ApiResult.Error(ErrorParser.parseException(e), ApiResult.Kind.NETWORK)
    } catch (e: HttpException) {
        val parsed = ErrorParser.parseHttpException(e)
        ApiResult.Error(parsed.first, parsed.second)
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Terjadi kesalahan, coba lagi.", ApiResult.Kind.UNKNOWN)
    }
}
