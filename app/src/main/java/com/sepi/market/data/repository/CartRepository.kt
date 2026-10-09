package com.sepi.market.data.repository

import com.sepi.market.core.ApiResult
import com.sepi.market.core.safeCall
import com.sepi.market.data.remote.RestApi
import com.sepi.market.data.remote.dto.AddToCartRequest
import com.sepi.market.data.remote.dto.CartItemDto
import com.sepi.market.data.remote.dto.UpdateCartQuantityRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository keranjang belanja yang mengekspos cartCount: StateFlow<Int>.
 */
class CartRepository(
    private val restApi: RestApi
) {

    private val _cartCount = MutableStateFlow(0)
    val cartCount: StateFlow<Int> = _cartCount.asStateFlow()

    /**
     * Memuat jumlah baris keranjang untuk badge di header aplikasi.
     */
    suspend fun refreshCartCount() {
        val result = safeCall { restApi.getCartItems() }
        if (result is ApiResult.Success) {
            _cartCount.value = result.data.size
        }
    }

    /**
     * Mengambil seluruh isi item di dalam keranjang belanja pengguna saat ini.
     */
    suspend fun getCartItems(): ApiResult<List<CartItemDto>> {
        val result = safeCall { restApi.getCartItems() }
        if (result is ApiResult.Success) {
            _cartCount.value = result.data.size
        }
        return result
    }

    /**
     * Menambahkan produk/varian ke keranjang belanja via RPC add_to_cart.
     */
    suspend fun addToCart(
        productId: String,
        variantId: String? = null,
        quantity: Int = 1
    ): ApiResult<CartItemDto> {
        val result = safeCall {
            restApi.addToCart(
                AddToCartRequest(
                    productId = productId,
                    variantId = variantId,
                    quantity = quantity
                )
            )
        }
        if (result is ApiResult.Success) {
            refreshCartCount()
        }
        return result
    }

    /**
     * Memperbarui kuantitas suatu item dalam keranjang.
     */
    suspend fun updateQuantity(cartItemId: String, newQuantity: Int): ApiResult<List<CartItemDto>> {
        val result = safeCall {
            restApi.updateCartQuantity(
                idFilter = "eq.$cartItemId",
                request = UpdateCartQuantityRequest(newQuantity)
            )
        }
        if (result is ApiResult.Success) {
            refreshCartCount()
        }
        return result
    }

    /**
     * Menghapus sebuah item dari keranjang belanja.
     */
    suspend fun deleteCartItem(cartItemId: String): ApiResult<Unit> {
        val result = safeCall {
            restApi.deleteCartItem(idFilter = "eq.$cartItemId")
            Unit
        }
        if (result is ApiResult.Success) {
            refreshCartCount()
        }
        return result
    }

    /**
     * Bersihkan cache lokal jumlah keranjang saat keluar akun.
     */
    fun clearLocal() {
        _cartCount.value = 0
    }
}
