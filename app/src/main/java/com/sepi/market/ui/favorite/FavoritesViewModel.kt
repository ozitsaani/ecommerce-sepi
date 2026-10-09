package com.sepi.market.ui.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sepi.market.core.ApiResult
import com.sepi.market.core.UiState
import com.sepi.market.data.remote.dto.ProductDto
import com.sepi.market.data.repository.CartRepository
import com.sepi.market.data.repository.FavoriteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel(
    val favoriteRepository: FavoriteRepository,
    val cartRepository: CartRepository
) : ViewModel() {

    private val _favoritesState = MutableStateFlow<UiState<List<ProductDto>>>(UiState.Loading)
    val favoritesState: StateFlow<UiState<List<ProductDto>>> = _favoritesState.asStateFlow()

    val cartCount: StateFlow<Int> = cartRepository.cartCount

    fun loadFavorites(forceRefresh: Boolean = false) {
        if (!forceRefresh && _favoritesState.value is UiState.Success) {
            return
        }
        _favoritesState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = favoriteRepository.getFavoriteProducts()) {
                is ApiResult.Success -> {
                    _favoritesState.value = UiState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _favoritesState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun removeFavorite(productId: String) {
        val currentState = _favoritesState.value
        if (currentState is UiState.Success) {
            val updated = currentState.data.filter { it.id != productId }
            _favoritesState.value = UiState.Success(updated)
        }
        viewModelScope.launch {
            val result = favoriteRepository.toggleFavorite(productId)
            if (result is ApiResult.Error) {
                loadFavorites()
            }
        }
    }
}
