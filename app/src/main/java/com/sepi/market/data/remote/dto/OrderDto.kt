package com.sepi.market.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO pesanan lengkap.
 */
data class OrderDto(
    val id: String,
    @SerializedName("order_code") val orderCode: String,
    val status: String,
    @SerializedName("recipient_name") val recipientName: String,
    @SerializedName("recipient_phone") val recipientPhone: String,
    @SerializedName("shipping_address") val shippingAddress: String,
    @SerializedName("shipping_method") val shippingMethod: String,
    @SerializedName("courier_name") val courierName: String,
    @SerializedName("tracking_number") val trackingNumber: String? = null,
    val subtotal: Long,
    @SerializedName("shipping_cost") val shippingCost: Long,
    val discount: Long = 0L,
    val total: Long,
    @SerializedName("voucher_code") val voucherCode: String? = null,
    @SerializedName("va_number") val vaNumber: String = "1234567890",
    @SerializedName("payment_deadline") val paymentDeadline: String,
    @SerializedName("paid_at") val paidAt: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("order_items") val items: List<OrderItemDto>? = null,
    @SerializedName("order_tracking") val tracking: List<TrackingDto>? = null
)

/**
 * DTO item dalam pesanan.
 */
data class OrderItemDto(
    val id: String,
    @SerializedName("product_id") val productId: String? = null,
    @SerializedName("product_name") val productName: String,
    @SerializedName("variant_label") val variantLabel: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("unit_price") val unitPrice: Long,
    val quantity: Int
)

/**
 * DTO timeline tracking pengiriman pesanan.
 */
data class TrackingDto(
    val id: String,
    val title: String,
    val description: String = "",
    @SerializedName("created_at") val createdAt: String
)

/**
 * Request body untuk RPC checkout.
 */
data class CheckoutRequest(
    @SerializedName("p_address_id") val addressId: String,
    @SerializedName("p_shipping_method") val shippingMethod: String,
    @SerializedName("p_voucher_code") val voucherCode: String? = null,
    @SerializedName("p_cart_item_ids") val cartItemIds: List<String>? = null
)

/**
 * Request body untuk RPC simulate_payment.
 */
data class SimulatePaymentRequest(
    @SerializedName("p_order_id") val orderId: String
)

/**
 * Request body untuk RPC cancel_order.
 */
data class CancelOrderRequest(
    @SerializedName("p_order_id") val orderId: String
)

/**
 * Request body untuk RPC advance_order (Mode Demo).
 */
data class AdvanceOrderRequest(
    @SerializedName("p_order_id") val orderId: String
)
