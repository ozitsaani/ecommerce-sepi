package com.sepi.market.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO profil pengguna.
 */
data class ProfileDto(
    val id: String,
    @SerializedName("full_name") val fullName: String,
    val phone: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

/**
 * DTO alamat pengiriman pengguna (wilayah Cirebon).
 */
data class AddressDto(
    val id: String? = null,
    val label: String = "Rumah",
    @SerializedName("recipient_name") val recipientName: String,
    val phone: String,
    val street: String,
    val district: String = "",
    val city: String = "Cirebon",
    @SerializedName("postal_code") val postalCode: String = "",
    @SerializedName("is_default") val isDefault: Boolean = false
)

/**
 * DTO voucher diskon atau gratis ongkir.
 */
data class VoucherDto(
    val code: String,
    val title: String,
    val description: String = "",
    @SerializedName("discount_type") val discountType: String,
    @SerializedName("discount_value") val discountValue: Long,
    @SerializedName("min_spend") val minSpend: Long = 0L,
    @SerializedName("max_discount") val maxDiscount: Long? = null,
    @SerializedName("valid_until") val validUntil: String? = null,
    @SerializedName("is_active") val isActive: Boolean = true
)

/**
 * DTO ulasan produk.
 */
data class ReviewDto(
    val id: String,
    @SerializedName("reviewer_name") val reviewerName: String? = null,
    val rating: Int,
    val comment: String = "",
    @SerializedName("created_at") val createdAt: String
)

/**
 * DTO item favorit pengguna.
 */
data class FavoriteItemDto(
    @SerializedName("product_id") val productId: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val product: ProductDto? = null
)

/**
 * Request body untuk menambah produk ke favorit.
 */
data class AddFavoriteRequest(
    @SerializedName("product_id") val productId: String
)

/**
 * Request body untuk menulis ulasan baru.
 */
data class CreateReviewRequest(
    @SerializedName("product_id") val productId: String,
    val rating: Int,
    val comment: String
)

/**
 * Request body untuk mengubah profil (nama dan telepon).
 */
data class UpdateProfileRequest(
    @SerializedName("full_name") val fullName: String,
    val phone: String?
)
