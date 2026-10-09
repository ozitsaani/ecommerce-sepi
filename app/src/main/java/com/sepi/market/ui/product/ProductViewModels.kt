package com.sepi.market.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sepi.market.core.ApiResult
import com.sepi.market.core.UiState
import com.sepi.market.data.remote.dto.CartItemDto
import com.sepi.market.data.remote.dto.ProductDetailDto
import com.sepi.market.data.remote.dto.ProductDto
import com.sepi.market.data.remote.dto.ReviewDto
import com.sepi.market.data.remote.dto.VariantDto
import com.sepi.market.data.repository.CartRepository
import com.sepi.market.data.repository.FavoriteRepository
import com.sepi.market.data.repository.ProductRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class ProductListViewModel(
    val productRepository: ProductRepository,
    val favoriteRepository: FavoriteRepository,
    val cartRepository: CartRepository
) : ViewModel() {

    private val _productsState = MutableStateFlow<UiState<List<ProductDto>>>(UiState.Loading)
    val productsState: StateFlow<UiState<List<ProductDto>>> = _productsState.asStateFlow()

    val searchQuery = MutableStateFlow("")
    var currentSortOrder: String = "sold_count.desc"
    var categoryIdFilter: Int? = null
    var isFlashSaleFilter: Boolean? = null
    var minPriceFilter: Long? = null
    var maxPriceFilter: Long? = null
    var minRating4Filter: Boolean = false

    val cartCount: StateFlow<Int> = cartRepository.cartCount

    private var currentOffset = 0
    private var isEndReached = false
    private var isLoadingPage = false
    private val currentProductsList = mutableListOf<ProductDto>()

    init {
        viewModelScope.launch {
            searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect {
                    resetAndLoad()
                }
        }
    }

    fun setInitialFilters(
        categoryId: Int = -1,
        query: String = "",
        flashOnly: Boolean = false
    ) {
        if (categoryId > 0) categoryIdFilter = categoryId
        if (query.isNotBlank()) searchQuery.value = query
        if (flashOnly) isFlashSaleFilter = true
    }

    fun resetAndLoad() {
        currentOffset = 0
        isEndReached = false
        currentProductsList.clear()
        _productsState.value = UiState.Loading
        loadPage()
    }

    fun loadNextPage() {
        if (isLoadingPage || isEndReached) return
        loadPage()
    }

    private fun loadPage() {
        isLoadingPage = true
        viewModelScope.launch {
            val minRatingVal = if (minRating4Filter) 4.0 else null
            val result = productRepository.getProducts(
                order = currentSortOrder,
                limit = 20,
                offset = currentOffset,
                categoryId = categoryIdFilter,
                isFlashSale = isFlashSaleFilter,
                minRating = minRatingVal,
                minPrice = minPriceFilter,
                maxPrice = maxPriceFilter,
                query = searchQuery.value
            )

            isLoadingPage = false
            when (result) {
                is ApiResult.Success -> {
                    val newItems = result.data
                    if (newItems.size < 20) {
                        isEndReached = true
                    }
                    currentProductsList.addAll(newItems)
                    currentOffset += newItems.size
                    _productsState.value = UiState.Success(currentProductsList.toList())
                }
                is ApiResult.Error -> {
                    if (currentProductsList.isEmpty()) {
                        _productsState.value = UiState.Error(result.message)
                    }
                }
            }
        }
    }

    fun hasActiveFilters(): Boolean {
        return minPriceFilter != null || maxPriceFilter != null || minRating4Filter
    }

    fun resetFilters() {
        minPriceFilter = null
        maxPriceFilter = null
        minRating4Filter = false
        resetAndLoad()
    }
}

sealed interface ProductDetailUiEvent {
    data class AddedToCartSuccess(val cartItem: CartItemDto, val buyNow: Boolean) : ProductDetailUiEvent
    data class ShowToast(val message: String) : ProductDetailUiEvent
}

class ProductDetailViewModel(
    val productRepository: ProductRepository,
    val favoriteRepository: FavoriteRepository,
    val cartRepository: CartRepository
) : ViewModel() {

    private val _detailState = MutableStateFlow<UiState<ProductDetailDto>>(UiState.Loading)
    val detailState: StateFlow<UiState<ProductDetailDto>> = _detailState.asStateFlow()

    private val _reviewsState = MutableStateFlow<UiState<List<ReviewDto>>>(UiState.Loading)
    val reviewsState: StateFlow<UiState<List<ReviewDto>>> = _reviewsState.asStateFlow()

    private val _event = MutableSharedFlow<ProductDetailUiEvent>(extraBufferCapacity = 8)
    val event: SharedFlow<ProductDetailUiEvent> = _event.asSharedFlow()

    val selectedVariant = MutableStateFlow<VariantDto?>(null)
    val quantity = MutableStateFlow(1)

    val cartCount: StateFlow<Int> = cartRepository.cartCount

    private var lastProductId: String = ""

    fun loadProductDetail(productId: String, forceRefresh: Boolean = false) {
        if (!forceRefresh && lastProductId == productId && _detailState.value is UiState.Success) {
            return
        }
        lastProductId = productId
        _detailState.value = UiState.Loading
        _reviewsState.value = UiState.Loading

        viewModelScope.launch {
            when (val result = productRepository.getProductDetail(productId)) {
                is ApiResult.Success -> {
                    val product = result.data
                    if (product != null) {
                        _detailState.value = UiState.Success(product)
                        // Select first variant if variants exist
                        if (!product.variants.isNullOrEmpty()) {
                            selectedVariant.value = product.variants.first()
                        }
                    } else {
                        _detailState.value = UiState.Error("Produk tidak ditemukan")
                    }
                }
                is ApiResult.Error -> {
                    _detailState.value = UiState.Error(result.message)
                }
            }
        }

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

    fun addToCart(productId: String, buyNow: Boolean = false) {
        val currentProduct = (_detailState.value as? UiState.Success)?.data ?: return
        val hasVariants = !currentProduct.variants.isNullOrEmpty()
        val variantId = selectedVariant.value?.id

        if (hasVariants && variantId == null) {
            _event.tryEmit(ProductDetailUiEvent.ShowToast("Pilih varian terlebih dahulu"))
            return
        }

        viewModelScope.launch {
            when (val result = cartRepository.addToCart(productId, variantId, quantity.value)) {
                is ApiResult.Success -> {
                    _event.tryEmit(ProductDetailUiEvent.AddedToCartSuccess(result.data, buyNow))
                }
                is ApiResult.Error -> {
                    _event.tryEmit(ProductDetailUiEvent.ShowToast(result.message))
                }
            }
        }
    }
}
