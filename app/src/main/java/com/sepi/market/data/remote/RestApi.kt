package com.sepi.market.data.remote

import com.sepi.market.data.remote.dto.AddToCartRequest
import com.sepi.market.data.remote.dto.AddressDto
import com.sepi.market.data.remote.dto.AddFavoriteRequest
import com.sepi.market.data.remote.dto.AdvanceOrderRequest
import com.sepi.market.data.remote.dto.CancelOrderRequest
import com.sepi.market.data.remote.dto.CartItemDto
import com.sepi.market.data.remote.dto.CategoryDto
import com.sepi.market.data.remote.dto.CheckoutRequest
import com.sepi.market.data.remote.dto.CreateReviewRequest
import com.sepi.market.data.remote.dto.FavoriteItemDto
import com.sepi.market.data.remote.dto.OrderDto
import com.sepi.market.data.remote.dto.ProductDetailDto
import com.sepi.market.data.remote.dto.ProductDto
import com.sepi.market.data.remote.dto.ProfileDto
import com.sepi.market.data.remote.dto.ReviewDto
import com.sepi.market.data.remote.dto.SimulatePaymentRequest
import com.sepi.market.data.remote.dto.UpdateCartQuantityRequest
import com.sepi.market.data.remote.dto.UpdateProfileRequest
import com.sepi.market.data.remote.dto.VoucherDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Interface Retrofit untuk endpoint REST PostgREST dan fungsi RPC Supabase (rest/v1/...).
 */
interface RestApi {

    // ==========================================
    // 1. KATEGORI & PRODUK
    // ==========================================

    @GET("rest/v1/categories?select=*&order=sort_order.asc")
    suspend fun getCategories(): List<CategoryDto>

    @GET("rest/v1/products")
    suspend fun getProducts(
        @Query("select") select: String = "id,slug,name,price,original_price,image_url,rating_avg,rating_count,sold_count,stock,is_flash_sale,shop_city",
        @Query("order") order: String = "sold_count.desc",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("category_id") categoryId: String? = null,
        @Query("is_flash_sale") isFlashSale: String? = null,
        @Query("rating_avg") ratingAvg: String? = null,
        @Query("and") andPrice: String? = null,
        @Query("or") orSearch: String? = null
    ): List<ProductDto>

    @GET("rest/v1/products")
    suspend fun getProductDetail(
        @Query("id") idFilter: String,
        @Query("select") select: String = "*,product_variants(id,group_name,option_name,price_delta)"
    ): List<ProductDetailDto>

    // ==========================================
    // 2. ULASAN
    // ==========================================

    @GET("rest/v1/reviews?select=id,reviewer_name,rating,comment,created_at&order=created_at.desc")
    suspend fun getProductReviews(
        @Query("product_id") productIdFilter: String
    ): List<ReviewDto>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/reviews")
    suspend fun createReview(
        @Body request: CreateReviewRequest
    ): List<ReviewDto>

    // ==========================================
    // 3. KERANJANG
    // ==========================================

    @POST("rest/v1/rpc/add_to_cart")
    suspend fun addToCart(
        @Body request: AddToCartRequest
    ): CartItemDto

    @GET("rest/v1/cart_items?select=id,quantity,variant_id,product:products(id,name,price,image_url,stock),variant:product_variants(id,group_name,option_name,price_delta)&order=created_at.desc")
    suspend fun getCartItems(): List<CartItemDto>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/cart_items")
    suspend fun updateCartQuantity(
        @Query("id") idFilter: String,
        @Body request: UpdateCartQuantityRequest
    ): List<CartItemDto>

    @DELETE("rest/v1/cart_items")
    suspend fun deleteCartItem(
        @Query("id") idFilter: String
    ): Response<Unit>

    // ==========================================
    // 4. FAVORIT
    // ==========================================

    @GET("rest/v1/favorites?select=product_id")
    suspend fun getFavoriteIds(): List<FavoriteItemDto>

    @GET("rest/v1/favorites?select=created_at,product:products(id,slug,name,price,original_price,image_url,rating_avg,rating_count,sold_count,stock,is_flash_sale,shop_city)&order=created_at.desc")
    suspend fun getFavorites(): List<FavoriteItemDto>

    @Headers("Prefer: resolution=ignore-duplicates")
    @POST("rest/v1/favorites")
    suspend fun addFavorite(
        @Body request: AddFavoriteRequest
    ): Response<Unit>

    @DELETE("rest/v1/favorites")
    suspend fun removeFavorite(
        @Query("product_id") productIdFilter: String
    ): Response<Unit>

    // ==========================================
    // 5. ALAMAT
    // ==========================================

    @GET("rest/v1/addresses?select=*&order=is_default.desc,created_at.desc")
    suspend fun getAddresses(): List<AddressDto>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/addresses")
    suspend fun createAddress(
        @Body address: AddressDto
    ): List<AddressDto>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/addresses")
    suspend fun updateAddress(
        @Query("id") idFilter: String,
        @Body address: AddressDto
    ): List<AddressDto>

    @DELETE("rest/v1/addresses")
    suspend fun deleteAddress(
        @Query("id") idFilter: String
    ): Response<Unit>

    // ==========================================
    // 6. VOUCHER
    // ==========================================

    @GET("rest/v1/vouchers?select=*&order=min_spend.asc")
    suspend fun getVouchers(): List<VoucherDto>

    // ==========================================
    // 7. PESANAN & CHECKOUT (RPC & ORDERS)
    // ==========================================

    @POST("rest/v1/rpc/checkout")
    suspend fun checkout(
        @Body request: CheckoutRequest
    ): OrderDto

    @POST("rest/v1/rpc/simulate_payment")
    suspend fun simulatePayment(
        @Body request: SimulatePaymentRequest
    ): OrderDto

    @POST("rest/v1/rpc/cancel_order")
    suspend fun cancelOrder(
        @Body request: CancelOrderRequest
    ): OrderDto

    @POST("rest/v1/rpc/advance_order")
    suspend fun advanceOrder(
        @Body request: AdvanceOrderRequest
    ): OrderDto

    @GET("rest/v1/orders?select=*,order_items(*)&order=created_at.desc")
    suspend fun getOrders(
        @Query("status") statusFilter: String? = null
    ): List<OrderDto>

    @GET("rest/v1/orders?select=*,order_items(*),order_tracking(*)")
    suspend fun getOrderDetail(
        @Query("id") idFilter: String
    ): List<OrderDto>

    // ==========================================
    // 8. PROFIL
    // ==========================================

    @GET("rest/v1/profiles?select=*")
    suspend fun getProfile(
        @Query("id") idFilter: String
    ): List<ProfileDto>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/profiles")
    suspend fun updateProfile(
        @Query("id") idFilter: String,
        @Body request: UpdateProfileRequest
    ): List<ProfileDto>
}
