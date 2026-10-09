package com.sepi.market.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sepi.market.ui.auth.AuthViewModel
import com.sepi.market.ui.cart.CartViewModel
import com.sepi.market.ui.category.CategoryViewModel
import com.sepi.market.ui.checkout.CheckoutViewModel
import com.sepi.market.ui.favorite.FavoritesViewModel
import com.sepi.market.ui.home.HomeViewModel
import com.sepi.market.ui.order.OrderDetailViewModel
import com.sepi.market.ui.order.OrdersViewModel
import com.sepi.market.ui.payment.PaymentViewModel
import com.sepi.market.ui.product.ProductDetailViewModel
import com.sepi.market.ui.product.ProductListViewModel
import com.sepi.market.ui.profile.AddressesViewModel
import com.sepi.market.ui.profile.ProfileViewModel
import com.sepi.market.ui.profile.VouchersViewModel
import com.sepi.market.ui.review.ReviewsViewModel

/**
 * Satu ViewModelProvider.Factory terpadu untuk semua ViewModel aplikasi Sepi (PRD Bagian 5 & 9.1).
 */
class ViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(
                    authRepository = container.authRepository,
                    cartRepository = container.cartRepository,
                    favoriteRepository = container.favoriteRepository
                ) as T
            }
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    productRepository = container.productRepository,
                    cartRepository = container.cartRepository,
                    profileRepository = container.profileRepository,
                    sessionManager = container.sessionManager
                ) as T
            }
            modelClass.isAssignableFrom(CategoryViewModel::class.java) -> {
                CategoryViewModel(
                    productRepository = container.productRepository
                ) as T
            }
            modelClass.isAssignableFrom(ProductListViewModel::class.java) -> {
                ProductListViewModel(
                    productRepository = container.productRepository,
                    favoriteRepository = container.favoriteRepository,
                    cartRepository = container.cartRepository
                ) as T
            }
            modelClass.isAssignableFrom(ProductDetailViewModel::class.java) -> {
                ProductDetailViewModel(
                    productRepository = container.productRepository,
                    favoriteRepository = container.favoriteRepository,
                    cartRepository = container.cartRepository
                ) as T
            }
            modelClass.isAssignableFrom(CartViewModel::class.java) -> {
                CartViewModel(
                    cartRepository = container.cartRepository
                ) as T
            }
            modelClass.isAssignableFrom(CheckoutViewModel::class.java) -> {
                CheckoutViewModel(
                    orderRepository = container.orderRepository,
                    profileRepository = container.profileRepository,
                    cartRepository = container.cartRepository
                ) as T
            }
            modelClass.isAssignableFrom(PaymentViewModel::class.java) -> {
                PaymentViewModel(
                    orderRepository = container.orderRepository
                ) as T
            }
            modelClass.isAssignableFrom(OrdersViewModel::class.java) -> {
                OrdersViewModel(
                    orderRepository = container.orderRepository
                ) as T
            }
            modelClass.isAssignableFrom(OrderDetailViewModel::class.java) -> {
                OrderDetailViewModel(
                    orderRepository = container.orderRepository
                ) as T
            }
            modelClass.isAssignableFrom(FavoritesViewModel::class.java) -> {
                FavoritesViewModel(
                    favoriteRepository = container.favoriteRepository,
                    cartRepository = container.cartRepository
                ) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(
                    profileRepository = container.profileRepository,
                    authRepository = container.authRepository,
                    cartRepository = container.cartRepository,
                    favoriteRepository = container.favoriteRepository
                ) as T
            }
            modelClass.isAssignableFrom(AddressesViewModel::class.java) -> {
                AddressesViewModel(
                    profileRepository = container.profileRepository
                ) as T
            }
            modelClass.isAssignableFrom(VouchersViewModel::class.java) -> {
                VouchersViewModel(
                    profileRepository = container.profileRepository
                ) as T
            }
            modelClass.isAssignableFrom(ReviewsViewModel::class.java) -> {
                ReviewsViewModel(
                    productRepository = container.productRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Kelas ViewModel tidak dikenal: ${modelClass.name}")
        }
    }
}
