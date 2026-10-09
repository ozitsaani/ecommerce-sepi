package com.sepi.market.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sepi.market.core.ApiResult
import com.sepi.market.core.UiState
import com.sepi.market.data.remote.dto.CategoryDto
import com.sepi.market.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CategoryViewModel(
    val productRepository: ProductRepository
) : ViewModel() {

    private val _categoryState = MutableStateFlow<UiState<List<CategoryDto>>>(UiState.Loading)
    val categoryState: StateFlow<UiState<List<CategoryDto>>> = _categoryState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        _categoryState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = productRepository.getCategories()) {
                is ApiResult.Success -> {
                    _categoryState.value = UiState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _categoryState.value = UiState.Error(result.message)
                }
            }
        }
    }
}
