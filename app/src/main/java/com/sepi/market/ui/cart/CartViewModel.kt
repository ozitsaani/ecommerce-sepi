package com.sepi.market.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sepi.market.core.ApiResult
import com.sepi.market.core.UiState
import com.sepi.market.data.remote.dto.CartItemDto
import com.sepi.market.data.repository.CartRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CartUiEvent {
    data class ShowToast(val message: String) : CartUiEvent
}

class CartViewModel(
    val cartRepository: CartRepository
) : ViewModel() {

    private val _cartState = MutableStateFlow<UiState<List<CartItemDto>>>(UiState.Loading)
    val cartState: StateFlow<UiState<List<CartItemDto>>> = _cartState.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _cartEvent = MutableSharedFlow<CartUiEvent>(extraBufferCapacity = 8)
    val cartEvent: SharedFlow<CartUiEvent> = _cartEvent.asSharedFlow()

    fun loadCartItems(preselectId: String? = null, forceRefresh: Boolean = false) {
        if (!forceRefresh && _cartState.value is UiState.Success) {
            return
        }
        _cartState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = cartRepository.getCartItems()) {
                is ApiResult.Success -> {
                    val items = result.data
                    _cartState.value = UiState.Success(items)

                    if (!preselectId.isNullOrBlank()) {
                        val matching = items.find { it.id == preselectId || it.product?.id == preselectId }
                        if (matching != null) {
                            _selectedIds.value = setOf(matching.id)
                        } else {
                            _selectedIds.value = items.map { it.id }.toSet()
                        }
                    } else if (_selectedIds.value.isEmpty()) {
                        _selectedIds.value = items.map { it.id }.toSet()
                    } else {
                        val validIds = items.map { it.id }.toSet()
                        _selectedIds.value = _selectedIds.value.filter { validIds.contains(it) }.toSet()
                    }
                }
                is ApiResult.Error -> {
                    _cartState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun toggleSelectItem(cartItemId: String) {
        val current = _selectedIds.value
        _selectedIds.value = if (current.contains(cartItemId)) {
            current - cartItemId
        } else {
            current + cartItemId
        }
    }

    fun toggleSelectAll(selectAll: Boolean) {
        val currentItems = (_cartState.value as? UiState.Success)?.data ?: return
        if (selectAll) {
            _selectedIds.value = currentItems.map { it.id }.toSet()
        } else {
            _selectedIds.value = emptySet()
        }
    }

    fun updateQuantity(cartItemId: String, newQuantity: Int) {
        if (newQuantity < 1) return
        viewModelScope.launch {
            when (val result = cartRepository.updateQuantity(cartItemId, newQuantity)) {
                is ApiResult.Success -> {
                    loadCartItems()
                }
                is ApiResult.Error -> {
                    _cartEvent.tryEmit(CartUiEvent.ShowToast(result.message))
                }
            }
        }
    }

    fun deleteItem(cartItemId: String) {
        viewModelScope.launch {
            when (val result = cartRepository.deleteCartItem(cartItemId)) {
                is ApiResult.Success -> {
                    _selectedIds.value = _selectedIds.value - cartItemId
                    loadCartItems()
                }
                is ApiResult.Error -> {
                    _cartEvent.tryEmit(CartUiEvent.ShowToast(result.message))
                }
            }
        }
    }
}
