package com.sepi.market.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sepi.market.core.ApiResult
import com.sepi.market.core.UiState
import com.sepi.market.data.remote.dto.AddressDto
import com.sepi.market.data.remote.dto.CartItemDto
import com.sepi.market.data.remote.dto.OrderDto
import com.sepi.market.data.remote.dto.VoucherDto
import com.sepi.market.data.repository.CartRepository
import com.sepi.market.data.repository.OrderRepository
import com.sepi.market.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CheckoutUiEvent {
    data class OrderCreatedSuccess(val order: OrderDto) : CheckoutUiEvent
    data class ShowToast(val message: String) : CheckoutUiEvent
}

data class CheckoutData(
    val selectedCartItems: List<CartItemDto>,
    val addresses: List<AddressDto>,
    val vouchers: List<VoucherDto>
)

class CheckoutViewModel(
    val orderRepository: OrderRepository,
    val profileRepository: ProfileRepository,
    val cartRepository: CartRepository
) : ViewModel() {

    private val _checkoutState = MutableStateFlow<UiState<CheckoutData>>(UiState.Loading)
    val checkoutState: StateFlow<UiState<CheckoutData>> = _checkoutState.asStateFlow()

    private val _checkoutEvent = MutableSharedFlow<CheckoutUiEvent>(extraBufferCapacity = 8)
    val checkoutEvent: SharedFlow<CheckoutUiEvent> = _checkoutEvent.asSharedFlow()

    val selectedAddress = MutableStateFlow<AddressDto?>(null)
    val shippingMethod = MutableStateFlow("REGULER")
    val selectedVoucher = MutableStateFlow<VoucherDto?>(null)

    val isSubmitting = MutableStateFlow(false)

    fun loadCheckoutData(cartItemIds: List<String>) {
        _checkoutState.value = UiState.Loading
        viewModelScope.launch {
            val cartRes = cartRepository.getCartItems()
            val addrRes = profileRepository.getAddresses()
            val voucherRes = profileRepository.getVouchers()

            if (cartRes is ApiResult.Success && addrRes is ApiResult.Success) {
                val allCartItems = cartRes.data
                val selectedItems = if (cartItemIds.isNotEmpty()) {
                    allCartItems.filter { cartItemIds.contains(it.id) }
                } else {
                    allCartItems
                }

                val addresses = addrRes.data
                val defaultAddr = addresses.find { it.isDefault } ?: addresses.firstOrNull()
                selectedAddress.value = defaultAddr

                val vouchers = (voucherRes as? ApiResult.Success)?.data ?: emptyList()

                _checkoutState.value = UiState.Success(
                    CheckoutData(
                        selectedCartItems = selectedItems,
                        addresses = addresses,
                        vouchers = vouchers
                    )
                )
            } else {
                val errorMsg = (cartRes as? ApiResult.Error)?.message
                    ?: (addrRes as? ApiResult.Error)?.message
                    ?: "Gagal memuat data checkout"
                _checkoutState.value = UiState.Error(errorMsg)
            }
        }
    }

    fun selectAddress(address: AddressDto) {
        selectedAddress.value = address
    }

    fun selectVoucher(voucher: VoucherDto?) {
        selectedVoucher.value = voucher
    }

    fun placeOrder(cartItemIds: List<String>) {
        val address = selectedAddress.value
        if (address == null) {
            _checkoutEvent.tryEmit(CheckoutUiEvent.ShowToast("Pilih alamat pengiriman terlebih dahulu"))
            return
        }

        if (!address.city.contains("Cirebon", ignoreCase = true)) {
            _checkoutEvent.tryEmit(CheckoutUiEvent.ShowToast("Pengiriman hanya tersedia untuk wilayah Cirebon"))
            return
        }

        isSubmitting.value = true
        viewModelScope.launch {
            val result = orderRepository.checkout(
                addressId = address.id ?: "",
                shippingMethod = shippingMethod.value,
                voucherCode = selectedVoucher.value?.code,
                cartItemIds = cartItemIds
            )
            isSubmitting.value = false

            when (result) {
                is ApiResult.Success -> {
                    _checkoutEvent.tryEmit(CheckoutUiEvent.OrderCreatedSuccess(result.data))
                }
                is ApiResult.Error -> {
                    _checkoutEvent.tryEmit(CheckoutUiEvent.ShowToast(result.message))
                }
            }
        }
    }
}
