package com.sepi.market

import android.app.Application
import com.sepi.market.core.AppContainer
import timber.log.Timber

/**
 * Kelas Application utama untuk aplikasi Sepi Marketplace.
 * Menyiapkan logging Timber pada mode debug dan inisialisasi AppContainer.
 */
class SepiApp : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        appContainer = AppContainer(this)
    }
}
