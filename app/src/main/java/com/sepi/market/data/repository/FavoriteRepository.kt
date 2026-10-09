package com.sepi.market.data.repository

import com.sepi.market.core.ApiResult
import com.sepi.market.core.safeCall
import com.sepi.market.data.remote.RestApi
import com.sepi.market.data.remote.dto.AddFavoriteRequest
import com.sepi.market.data.remote.dto.ProductDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository favorit pengguna dengan pembaruan UI secara optimistis dan ekspos StateFlow<Set<String>>.
 */
class FavoriteRepository(
    private val restApi: RestApi
) {

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    /**
     * Memuat daftar seluruh ID produk favorit milik user saat ini.
     */
    suspend fun loadFavoriteIds(): ApiResult<Unit> {
        val result = safeCall { restApi.getFavoriteIds() }
        return when (result) {
            is ApiResult.Success -> {
                val ids = result.data.mapNotNull { it.productId }.toSet()
                _favoriteIds.value = ids
                ApiResult.Success(Unit)
            }
            is ApiResult.Error -> ApiResult.Error(result.message, result.kind)
        }
    }

    /**
     * Mengambil daftar produk favorit lengkap untuk tab Favorit.
     */
    suspend fun getFavoriteProducts(): ApiResult<List<ProductDto>> {
        val result = safeCall { restApi.getFavorites() }
        return when (result) {
            is ApiResult.Success -> {
                val products = result.data.mapNotNull { it.product }
                _favoriteIds.value = products.map { it.id }.toSet()
                ApiResult.Success(products)
            }
            is ApiResult.Error -> ApiResult.Error(result.message, result.kind)
        }
    }

    /**
     * Menambah atau menghapus produk dari favorit secara optimistis (PRD Bagian 8.1 & 11).
     * Jika request backend gagal, status ID dikembalikan ke state sebelumnya.
     */
    suspend fun toggleFavorite(productId: String): ApiResult<Boolean> {
        val currentSet = _favoriteIds.value
        val isCurrentlyFav = currentSet.contains(productId)
        val shouldBeFav = !isCurrentlyFav

        // Pembaruan optimistis lokal langsung
        if (shouldBeFav) {
            _favoriteIds.value = currentSet + productId
        } else {
            _favoriteIds.value = currentSet - productId
        }

        val apiResult = if (shouldBeFav) {
            safeCall {
                restApi.addFavorite(AddFavoriteRequest(productId))
                true
            }
        } else {
            safeCall {
                restApi.removeFavorite(productIdFilter = "eq.$productId")
                false
            }
        }

        return when (apiResult) {
            is ApiResult.Success -> ApiResult.Success(shouldBeFav)
            is ApiResult.Error -> {
                // Kembalikan ke state semula karena gagal
                _favoriteIds.value = currentSet
                ApiResult.Error(apiResult.message, apiResult.kind)
            }
        }
    }

    /**
     * Bersihkan cache lokal saat pengguna keluar.
     */
    fun clearLocal() {
        _favoriteIds.value = emptySet()
    }
}
