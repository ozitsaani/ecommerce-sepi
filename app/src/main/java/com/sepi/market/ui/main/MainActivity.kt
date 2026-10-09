package com.sepi.market.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.sepi.market.R
import com.sepi.market.SepiApp
import com.sepi.market.core.gone
import com.sepi.market.core.visible
import com.sepi.market.databinding.ActivityMainBinding
import com.sepi.market.ui.auth.AuthActivity
import kotlinx.coroutines.launch

/**
 * Aktivitas utama aplikasi Sepi yang memuat NavHostFragment dan BottomNavigation kaca (PRD Bagian 9.2).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupNavigation()
        observeSessionExpired()
    }

    private fun setupWindowInsets() {
        // Cegah BottomNavigationView internal menambahkan padding insets ganda di dalam bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { view, insets ->
            view.setPadding(0, 0, 0, 0)
            insets
        }

        // Terapkan navigation bars inset HANYA pada margin bar mengambang, tanpa menambah padding ganda
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavContainer) { view, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val defaultMargin = resources.getDimensionPixelSize(R.dimen.spacing_base)
            val params = view.layoutParams as? ViewGroup.MarginLayoutParams
            if (params != null) {
                params.bottomMargin = defaultMargin + navBarInsets.bottom
                params.leftMargin = defaultMargin + navBarInsets.left
                params.rightMargin = defaultMargin + navBarInsets.right
                view.layoutParams = params
            }
            insets
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)

        // Sembunyikan bottom navigation di layar detail/sub-halaman (PRD Bagian 9.2)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment,
                R.id.categoryFragment,
                R.id.favoritesFragment,
                R.id.ordersFragment,
                R.id.profileFragment -> {
                    binding.bottomNavContainer.visible()
                }
                else -> {
                    binding.bottomNavContainer.gone()
                }
            }
        }
    }

    private fun observeSessionExpired() {
        val sessionManager = (application as SepiApp).appContainer.sessionManager
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionManager.sessionExpired.collect {
                    Toast.makeText(this@MainActivity, getString(R.string.session_expired), Toast.LENGTH_LONG).show()
                    val intent = Intent(this@MainActivity, AuthActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
