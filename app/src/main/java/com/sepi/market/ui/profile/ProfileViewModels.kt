package com.sepi.market.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sepi.market.core.ApiResult
import com.sepi.market.core.UiState
import com.sepi.market.data.remote.dto.AddressDto
import com.sepi.market.data.remote.dto.ProfileDto
import com.sepi.market.data.remote.dto.VoucherDto
import com.sepi.market.data.repository.AuthRepository
import com.sepi.market.data.repository.CartRepository
import com.sepi.market.data.repository.FavoriteRepository
import com.sepi.market.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProfileUiEvent {
    object LogoutSuccess : ProfileUiEvent
    data class ShowToast(val message: String) : ProfileUiEvent
}

class ProfileViewModel(
    val profileRepository: ProfileRepository,
    val authRepository: AuthRepository,
    val cartRepository: CartRepository,
    val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow<UiState<ProfileDto>>(UiState.Loading)
    val profileState: StateFlow<UiState<ProfileDto>> = _profileState.asStateFlow()

    private val _event = MutableSharedFlow<ProfileUiEvent>(extraBufferCapacity = 8)
    val event: SharedFlow<ProfileUiEvent> = _event.asSharedFlow()

    fun loadProfile() {
        val userId = authRepository.sessionManager.userId.orEmpty()
        val email = authRepository.sessionManager.userEmail.orEmpty()
        val name = authRepository.sessionManager.userName.orEmpty()

        if (userId.isBlank()) {
            _profileState.value = UiState.Error("Belum login")
            return
        }

        _profileState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = profileRepository.getProfile(userId)) {
                is ApiResult.Success -> {
                    val profile = result.data ?: ProfileDto(id = userId, fullName = name, phone = authRepository.sessionManager.userPhone)
                    _profileState.value = UiState.Success(profile)
                }
                is ApiResult.Error -> {
                    _profileState.value = UiState.Success(
                        ProfileDto(id = userId, fullName = if (name.isNotBlank()) name else email, phone = authRepository.sessionManager.userPhone)
                    )
                }
            }
        }
    }

    fun updateProfile(fullName: String, phone: String?) {
        val userId = authRepository.sessionManager.userId.orEmpty()
        viewModelScope.launch {
            when (val result = profileRepository.updateProfile(userId, fullName, phone)) {
                is ApiResult.Success -> {
                    authRepository.sessionManager.updateProfile(fullName, phone)
                    _event.tryEmit(ProfileUiEvent.ShowToast("Profil berhasil diperbarui"))
                    loadProfile()
                }
                is ApiResult.Error -> {
                    _event.tryEmit(ProfileUiEvent.ShowToast(result.message))
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            cartRepository.clearLocal()
            favoriteRepository.clearLocal()
            _event.tryEmit(ProfileUiEvent.LogoutSuccess)
        }
    }
}

class AddressesViewModel(
    val profileRepository: ProfileRepository
) : ViewModel() {

    private val _addressesState = MutableStateFlow<UiState<List<AddressDto>>>(UiState.Loading)
    val addressesState: StateFlow<UiState<List<AddressDto>>> = _addressesState.asStateFlow()

    private val _event = MutableSharedFlow<ProfileUiEvent>(extraBufferCapacity = 8)
    val event: SharedFlow<ProfileUiEvent> = _event.asSharedFlow()

    fun loadAddresses(forceRefresh: Boolean = false) {
        if (!forceRefresh && _addressesState.value is UiState.Success) {
            return
        }
        _addressesState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = profileRepository.getAddresses()) {
                is ApiResult.Success -> {
                    _addressesState.value = UiState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _addressesState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun createAddress(address: AddressDto) {
        viewModelScope.launch {
            when (val result = profileRepository.createAddress(address)) {
                is ApiResult.Success -> {
                    _event.tryEmit(ProfileUiEvent.ShowToast("Alamat berhasil ditambahkan"))
                    loadAddresses(forceRefresh = true)
                }
                is ApiResult.Error -> {
                    _event.tryEmit(ProfileUiEvent.ShowToast(result.message))
                }
            }
        }
    }

    fun updateAddress(id: String, address: AddressDto) {
        viewModelScope.launch {
            when (val result = profileRepository.updateAddress(id, address)) {
                is ApiResult.Success -> {
                    _event.tryEmit(ProfileUiEvent.ShowToast("Alamat berhasil diperbarui"))
                    loadAddresses(forceRefresh = true)
                }
                is ApiResult.Error -> {
                    _event.tryEmit(ProfileUiEvent.ShowToast(result.message))
                }
            }
        }
    }

    fun deleteAddress(id: String) {
        viewModelScope.launch {
            when (val result = profileRepository.deleteAddress(id)) {
                is ApiResult.Success -> {
                    _event.tryEmit(ProfileUiEvent.ShowToast("Alamat berhasil dihapus"))
                    loadAddresses(forceRefresh = true)
                }
                is ApiResult.Error -> {
                    _event.tryEmit(ProfileUiEvent.ShowToast(result.message))
                }
            }
        }
    }
}

class VouchersViewModel(
    val profileRepository: ProfileRepository
) : ViewModel() {

    private val _vouchersState = MutableStateFlow<UiState<List<VoucherDto>>>(UiState.Loading)
    val vouchersState: StateFlow<UiState<List<VoucherDto>>> = _vouchersState.asStateFlow()

    fun loadVouchers(forceRefresh: Boolean = false) {
        if (!forceRefresh && _vouchersState.value is UiState.Success) {
            return
        }
        _vouchersState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = profileRepository.getVouchers()) {
                is ApiResult.Success -> {
                    _vouchersState.value = UiState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _vouchersState.value = UiState.Error(result.message)
                }
            }
        }
    }
}
