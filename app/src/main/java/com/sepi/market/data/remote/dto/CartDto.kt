package com.sepi.market.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO item keranjang belanja.
 */
data class CartItemDto(
    val id: String,
    val quantity: Int,
    @SerializedName("variant_id") val variantId: String? = null,
    val product: CartProductDto? = null,
    val variant: VariantDto? = null
)

/**
 * DTO informasi produk singkat yang disematkan dalam item keranjang.
 */
data class CartProductDto(
    val id: String,
    val name: String,
    val price: Long,
    @SerializedName("image_url") val imageUrl: String? = null,
    val stock: Int = 0
)

/**
 * Request body untuk RPC add_to_cart.
 */
data class AddToCartRequest(
    @SerializedName("p_product_id") val productId: String,
    @SerializedName("p_variant_id") val variantId: String? = null,
    @SerializedName("p_quantity") val quantity: Int = 1
)

/**
 * Request body untuk PATCH jumlah item keranjang.
 */
data class UpdateCartQuantityRequest(
    val quantity: Int
)
