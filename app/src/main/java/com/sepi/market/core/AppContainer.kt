package com.sepi.market.core

import android.content.Context
import com.sepi.market.data.remote.NetworkModule
import com.sepi.market.data.repository.AuthRepository
import com.sepi.market.data.repository.CartRepository
import com.sepi.market.data.repository.FavoriteRepository
import com.sepi.market.data.repository.OrderRepository
import com.sepi.market.data.repository.ProductRepository
import com.sepi.market.data.repository.ProfileRepository

/**
 * Kontainer Manual Dependency Injection (DI) yang memegang seluruh singleton repository, network, dan ViewModelFactory.
 */
class AppContainer(context: Context) {

    val sessionManager: SessionManager = SessionManager(context)

    val networkModule: NetworkModule = NetworkModule(sessionManager)

    val authRepository: AuthRepository = AuthRepository(
        authApi = networkModule.authApi,
        sessionManager = sessionManager
    )

    val productRepository: ProductRepository = ProductRepository(
        restApi = networkModule.restApi
    )

    val cartRepository: CartRepository = CartRepository(
        restApi = networkModule.restApi
    )

    val favoriteRepository: FavoriteRepository = FavoriteRepository(
        restApi = networkModule.restApi
    )

    val orderRepository: OrderRepository = OrderRepository(
        restApi = networkModule.restApi,
        cartRepository = cartRepository
    )

    val profileRepository: ProfileRepository = ProfileRepository(
        restApi = networkModule.restApi
    )

    val viewModelFactory: ViewModelFactory = ViewModelFactory(this)
}
