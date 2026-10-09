package com.sepi.market.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sepi.market.core.ApiResult
import com.sepi.market.core.UiState
import com.sepi.market.core.SessionManager
import com.sepi.market.data.remote.dto.CategoryDto
import com.sepi.market.data.remote.dto.ProductDto
import com.sepi.market.data.remote.dto.VoucherDto
import com.sepi.market.data.repository.CartRepository
import com.sepi.market.data.repository.ProductRepository
import com.sepi.market.data.repository.ProfileRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeData(
    val userName: String,
    val vouchers: List<VoucherDto>,
    val categories: List<CategoryDto>,
    val flashSaleProducts: List<ProductDto>,
    val recommendedProducts: List<ProductDto>
)

class HomeViewModel(
    val productRepository: ProductRepository,
    val cartRepository: CartRepository,
    val profileRepository: ProfileRepository,
    val sessionManager: SessionManager
) : ViewModel() {

    private val _homeState = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val homeState: StateFlow<UiState<HomeData>> = _homeState.asStateFlow()

    val cartCount: StateFlow<Int> = cartRepository.cartCount

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        _homeState.value = UiState.Loading
        viewModelScope.launch {
            cartRepository.refreshCartCount()

            val vouchersDeferred = async { profileRepository.getVouchers() }
            val categoriesDeferred = async { productRepository.getCategories() }
            val flashSaleDeferred = async {
                productRepository.getProducts(
                    isFlashSale = true,
                    limit = 10
                )
            }
            val recommendedDeferred = async {
                productRepository.getProducts(
                    order = "sold_count.desc",
                    limit = 20
                )
            }

            val vouchersRes = vouchersDeferred.await()
            val categoriesRes = categoriesDeferred.await()
            val flashSaleRes = flashSaleDeferred.await()
            val recommendedRes = recommendedDeferred.await()

            val isAnySuccess = categoriesRes is ApiResult.Success ||
                    recommendedRes is ApiResult.Success ||
                    flashSaleRes is ApiResult.Success

            if (isAnySuccess) {
                val storedName = sessionManager.userName.orEmpty().trim()
                val firstName = if (storedName.isNotBlank()) {
                    storedName.split("\\s+".toRegex()).firstOrNull() ?: storedName
                } else {
                    "Sobat Sepi"
                }
                val data = HomeData(
                    userName = firstName,
                    vouchers = (vouchersRes as? ApiResult.Success)?.data ?: emptyList(),
                    categories = (categoriesRes as? ApiResult.Success)?.data ?: emptyList(),
                    flashSaleProducts = (flashSaleRes as? ApiResult.Success)?.data ?: emptyList(),
                    recommendedProducts = (recommendedRes as? ApiResult.Success)?.data ?: emptyList()
                )
                _homeState.value = UiState.Success(data)
            } else {
                val errorMsg = (recommendedRes as? ApiResult.Error)?.message
                    ?: (categoriesRes as? ApiResult.Error)?.message
                    ?: "Gagal memuat data Beranda"
                _homeState.value = UiState.Error(errorMsg)
            }
        }
    }
}
