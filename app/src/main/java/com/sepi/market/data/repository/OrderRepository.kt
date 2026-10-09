package com.sepi.market.data.repository

import com.sepi.market.core.ApiResult
import com.sepi.market.core.safeCall
import com.sepi.market.data.remote.RestApi
import com.sepi.market.data.remote.dto.AdvanceOrderRequest
import com.sepi.market.data.remote.dto.CancelOrderRequest
import com.sepi.market.data.remote.dto.CheckoutRequest
import com.sepi.market.data.remote.dto.OrderDto
import com.sepi.market.data.remote.dto.SimulatePaymentRequest

/**
 * Repository untuk alur pemesanan: checkout RPC, pembayaran sandbox, pembatalan, mode demo advance order, dan pelacakan.
 */
class OrderRepository(
    private val restApi: RestApi,
    private val cartRepository: CartRepository
) {

    /**
     * Membuat pesanan baru dari item keranjang yang dipilih via RPC checkout.
     */
    suspend fun checkout(
        addressId: String,
        shippingMethod: String,
        voucherCode: String? = null,
        cartItemIds: List<String>? = null
    ): ApiResult<OrderDto> {
        val result = safeCall {
            restApi.checkout(
                CheckoutRequest(
                    addressId = addressId,
                    shippingMethod = shippingMethod,
                    voucherCode = if (voucherCode.isNullOrBlank()) null else voucherCode.trim(),
                    cartItemIds = cartItemIds
                )
            )
        }
        if (result is ApiResult.Success) {
            cartRepository.refreshCartCount()
        }
        return result
    }

    /**
     * Mensimulasikan pembayaran Virtual Account BCA (sandbox) via RPC simulate_payment.
     */
    suspend fun simulatePayment(orderId: String): ApiResult<OrderDto> {
        return safeCall {
            restApi.simulatePayment(SimulatePaymentRequest(orderId))
        }
    }

    /**
     * Membatalkan pesanan yang belum dibayar via RPC cancel_order (stok dikembalikan di server).
     */
    suspend fun cancelOrder(orderId: String): ApiResult<OrderDto> {
        return safeCall {
            restApi.cancelOrder(CancelOrderRequest(orderId))
        }
    }

    /**
     * MODE DEMO: Memajukan status pesanan satu langkah via RPC advance_order (PAID -> PACKED -> SHIPPED -> DELIVERED).
     */
    suspend fun advanceOrder(orderId: String): ApiResult<OrderDto> {
        return safeCall {
            restApi.advanceOrder(AdvanceOrderRequest(orderId))
        }
    }

    /**
     * Mengambil daftar pesanan pengguna, dengan opsi filter status tab (misal: in.(PAID,PACKED)).
     */
    suspend fun getOrders(statusFilter: String? = null): ApiResult<List<OrderDto>> {
        return safeCall {
            restApi.getOrders(statusFilter = statusFilter)
        }
    }

    /**
     * Mengambil detail lengkap pesanan beserta rincian barang dan riwayat tracking.
     */
    suspend fun getOrderDetail(orderId: String): ApiResult<OrderDto?> {
        return safeCall {
            val list = restApi.getOrderDetail(idFilter = "eq.$orderId")
            list.firstOrNull()
        }
    }
}
