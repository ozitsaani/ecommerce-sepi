package com.sepi.market.core

/**
 * Representasi status UI pada layar: Memuat (Loading), Berhasil (Success), atau Gagal (Error).
 */
sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
