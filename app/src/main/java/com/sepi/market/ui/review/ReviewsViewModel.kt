package com.sepi.market.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sepi.market.core.ApiResult
import com.sepi.market.core.UiState
import com.sepi.market.data.remote.dto.ReviewDto
import com.sepi.market.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ReviewUiEvent {
    object ReviewSubmittedSuccess : ReviewUiEvent
    data class ShowToast(val message: String) : ReviewUiEvent
}

class ReviewsViewModel(
    val productRepository: ProductRepository
) : ViewModel() {

    private val _reviewsState = MutableStateFlow<UiState<List<ReviewDto>>>(UiState.Loading)
    val reviewsState: StateFlow<UiState<List<ReviewDto>>> = _reviewsState.asStateFlow()

    private val _event = MutableSharedFlow<ReviewUiEvent>(extraBufferCapacity = 8)
    val event: SharedFlow<ReviewUiEvent> = _event.asSharedFlow()

    private var lastProductId: String = ""

    fun loadReviews(productId: String, forceRefresh: Boolean = false) {
        if (!forceRefresh && lastProductId == productId && _reviewsState.value is UiState.Success) {
            return
        }
        lastProductId = productId
        _reviewsState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = productRepository.getProductReviews(productId)) {
                is ApiResult.Success -> {
                    _reviewsState.value = UiState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _reviewsState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun submitReview(productId: String, rating: Int, comment: String) {
        if (rating < 1) {
            _event.tryEmit(ReviewUiEvent.ShowToast("Pilih bintang minimal 1"))
            return
        }

        viewModelScope.launch {
            when (val result = productRepository.createReview(productId, rating, comment)) {
                is ApiResult.Success -> {
                    _event.tryEmit(ReviewUiEvent.ReviewSubmittedSuccess)
                    loadReviews(productId, forceRefresh = true)
                }
                is ApiResult.Error -> {
                    _event.tryEmit(ReviewUiEvent.ShowToast(result.message))
                }
            }
        }
    }
}
