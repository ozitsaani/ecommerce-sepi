package com.sepi.market.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sepi.market.core.ApiResult
import com.sepi.market.core.UiState
import com.sepi.market.data.repository.AuthRepository
import com.sepi.market.data.repository.CartRepository
import com.sepi.market.data.repository.FavoriteRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiEvent {
    object NavigateToMain : AuthUiEvent
    data class ShowToast(val message: String) : AuthUiEvent
    data class ShowInfoDialog(val title: String, val message: String) : AuthUiEvent
}

class AuthViewModel(
    val authRepository: AuthRepository,
    val cartRepository: CartRepository,
    val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<UiState<Unit>>(UiState.Success(Unit))
    val authState: StateFlow<UiState<Unit>> = _authState.asStateFlow()

    private val _authEvent = MutableSharedFlow<AuthUiEvent>(extraBufferCapacity = 8)
    val authEvent: SharedFlow<AuthUiEvent> = _authEvent.asSharedFlow()

    fun signIn(email: String, pass: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _authState.value = UiState.Error("Format email tidak valid")
            return
        }
        if (pass.length < 6) {
            _authState.value = UiState.Error("Kata sandi minimal 6 karakter")
            return
        }

        _authState.value = UiState.Loading
        viewModelScope.launch {
            val result = authRepository.signIn(trimmedEmail, pass)
            when (result) {
                is ApiResult.Success -> {
                    _authState.value = UiState.Success(Unit)
                    cartRepository.refreshCartCount()
                    favoriteRepository.loadFavoriteIds()
                    _authEvent.tryEmit(AuthUiEvent.NavigateToMain)
                }
                is ApiResult.Error -> {
                    _authState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun signUp(fullName: String, email: String, pass: String, confirmPass: String) {
        val trimmedName = fullName.trim()
        val trimmedEmail = email.trim()

        if (trimmedName.isBlank()) {
            _authState.value = UiState.Error("Nama lengkap tidak boleh kosong")
            return
        }
        if (trimmedEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _authState.value = UiState.Error("Format email tidak valid")
            return
        }
        if (pass.length < 6) {
            _authState.value = UiState.Error("Kata sandi minimal 6 karakter")
            return
        }
        if (pass != confirmPass) {
            _authState.value = UiState.Error("Konfirmasi kata sandi tidak cocok")
            return
        }

        _authState.value = UiState.Loading
        viewModelScope.launch {
            val result = authRepository.signUp(trimmedName, trimmedEmail, pass)
            when (result) {
                is ApiResult.Success -> {
                    _authState.value = UiState.Success(Unit)
                    val token = result.data.accessToken
                    if (!token.isNullOrBlank()) {
                        cartRepository.refreshCartCount()
                        favoriteRepository.loadFavoriteIds()
                        _authEvent.tryEmit(AuthUiEvent.NavigateToMain)
                    } else {
                        _authEvent.tryEmit(
                            AuthUiEvent.ShowInfoDialog(
                                title = "Konfirmasi Email",
                                message = "Cek emailmu untuk konfirmasi akun, lalu masuk kembali."
                            )
                        )
                    }
                }
                is ApiResult.Error -> {
                    _authState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun clearError() {
        if (_authState.value is UiState.Error) {
            _authState.value = UiState.Success(Unit)
        }
    }
}
