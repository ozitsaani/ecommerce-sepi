package com.sepi.market.data.repository

import com.sepi.market.core.ApiResult
import com.sepi.market.core.safeCall
import com.sepi.market.data.remote.RestApi
import com.sepi.market.data.remote.dto.CategoryDto
import com.sepi.market.data.remote.dto.CreateReviewRequest
import com.sepi.market.data.remote.dto.ProductDetailDto
import com.sepi.market.data.remote.dto.ProductDto
import com.sepi.market.data.remote.dto.ReviewDto

/**
 * Repository untuk data produk, kategori, varian, dan ulasan.
 */
class ProductRepository(
    private val restApi: RestApi
) {

    /**
     * Mengambil daftar seluruh kategori produk yang diurutkan berdasar sort_order.
     */
    suspend fun getCategories(): ApiResult<List<CategoryDto>> {
        return safeCall { restApi.getCategories() }
    }

    /**
     * Mengambil daftar produk dengan filter dinamis dan paginasi (PRD Bagian 7.4).
     */
    suspend fun getProducts(
        order: String = "sold_count.desc",
        limit: Int = 20,
        offset: Int = 0,
        categoryId: Int? = null,
        isFlashSale: Boolean? = null,
        minRating: Double? = null,
        minPrice: Long? = null,
        maxPrice: Long? = null,
        query: String? = null
    ): ApiResult<List<ProductDto>> {
        return safeCall {
            val categoryFilter = if (categoryId != null && categoryId > 0) "eq.$categoryId" else null
            val flashSaleFilter = if (isFlashSale == true) "eq.true" else null
            val ratingFilter = if (minRating != null && minRating > 0) "gte.$minRating" else null

            // Rentang harga format PostgREST: and=(price.gte.X,price.lte.Y)
            val andPrice = when {
                minPrice != null && maxPrice != null -> "(price.gte.$minPrice,price.lte.$maxPrice)"
                minPrice != null -> "(price.gte.$minPrice)"
                maxPrice != null -> "(price.lte.$maxPrice)"
                else -> null
            }

            // Pembersihan kata kunci pencarian dari karakter berbahaya PostgREST: * , ( ) % \
            val orSearch = if (!query.isNullOrBlank()) {
                val cleanWord = query.replace(Regex("[*,()\\\\%]+"), " ").trim()
                if (cleanWord.isNotBlank()) {
                    "(name.ilike.*$cleanWord*,description.ilike.*$cleanWord*)"
                } else null
            } else null

            restApi.getProducts(
                order = order,
                limit = limit,
                offset = offset,
                categoryId = categoryFilter,
                isFlashSale = flashSaleFilter,
                ratingAvg = ratingFilter,
                andPrice = andPrice,
                orSearch = orSearch
            )
        }
    }

    /**
     * Mengambil detail lengkap produk beserta grup variannya.
     */
    suspend fun getProductDetail(productId: String): ApiResult<ProductDetailDto?> {
        return safeCall {
            val list = restApi.getProductDetail(idFilter = "eq.$productId")
            list.firstOrNull()
        }
    }

    /**
     * Mengambil ulasan pembeli untuk suatu produk.
     */
    suspend fun getProductReviews(productId: String): ApiResult<List<ReviewDto>> {
        return safeCall {
            restApi.getProductReviews(productIdFilter = "eq.$productId")
        }
    }

    /**
     * Mengirimkan ulasan baru untuk produk yang telah dibeli dan berstatus DELIVERED.
     */
    suspend fun createReview(productId: String, rating: Int, comment: String): ApiResult<List<ReviewDto>> {
        return safeCall {
            restApi.createReview(
                CreateReviewRequest(
                    productId = productId,
                    rating = rating,
                    comment = comment.trim()
                )
            )
        }
    }
}
