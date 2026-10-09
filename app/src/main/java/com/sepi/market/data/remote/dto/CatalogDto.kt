package com.sepi.market.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO untuk kategori produk.
 */
data class CategoryDto(
    val id: Int,
    val name: String,
    @SerializedName("icon_key") val iconKey: String = "tag",
    @SerializedName("sort_order") val sortOrder: Int = 0
)

/**
 * DTO untuk produk dalam daftar/katalog (grid 2 kolom).
 */
data class ProductDto(
    val id: String,
    val slug: String,
    val name: String,
    val price: Long,
    @SerializedName("original_price") val originalPrice: Long? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("rating_avg") val ratingAvg: Double = 0.0,
    @SerializedName("rating_count") val ratingCount: Int = 0,
    @SerializedName("sold_count") val soldCount: Int = 0,
    val stock: Int = 0,
    @SerializedName("is_flash_sale") val isFlashSale: Boolean = false,
    @SerializedName("shop_city") val shopCity: String? = "Cirebon"
)

/**
 * DTO untuk detail produk lengkap beserta deskripsi, nama toko, dan varian.
 */
data class ProductDetailDto(
    val id: String,
    val slug: String,
    val name: String,
    val price: Long,
    @SerializedName("original_price") val originalPrice: Long? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("rating_avg") val ratingAvg: Double = 0.0,
    @SerializedName("rating_count") val ratingCount: Int = 0,
    @SerializedName("sold_count") val soldCount: Int = 0,
    val stock: Int = 0,
    @SerializedName("is_flash_sale") val isFlashSale: Boolean = false,
    @SerializedName("shop_city") val shopCity: String? = "Cirebon",
    val description: String = "",
    @SerializedName("shop_name") val shopName: String = "Sepi Official",
    @SerializedName("product_variants") val variants: List<VariantDto>? = null
)

/**
 * DTO untuk varian produk (misal: Ukuran / Warna).
 */
data class VariantDto(
    val id: String,
    @SerializedName("group_name") val groupName: String,
    @SerializedName("option_name") val optionName: String,
    @SerializedName("price_delta") val priceDelta: Long = 0L
)
