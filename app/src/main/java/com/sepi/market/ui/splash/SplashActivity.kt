package com.sepi.market.ui.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sepi.market.BuildConfig
import com.sepi.market.SepiApp
import com.sepi.market.core.gone
import com.sepi.market.core.visible
import com.sepi.market.databinding.ActivitySplashBinding
import com.sepi.market.ui.auth.AuthActivity
import com.sepi.market.ui.main.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Layar pembuka (Splash Screen) aplikasi Sepi.
 * Menampilkan animasi logo 600ms (fade + scale), memeriksa sesi login minimal 1,2 detik,
 * dan memvalidasi konfigurasi Supabase tanpa crash (PRD Bagian 6.1 & 9.3).
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playEntranceAnimation()
        checkConfigAndSession()
    }

    /**
     * Animasi masuk 600 ms (fade + scale) untuk konten logo dan slogan (PRD Bagian 9.3).
     */
    private fun playEntranceAnimation() {
        binding.clContent.alpha = 0f
        binding.clContent.scaleX = 0.85f
        binding.clContent.scaleY = 0.85f

        val fadeAnim = ObjectAnimator.ofFloat(binding.clContent, View.ALPHA, 0f, 1f)
        val scaleXAnim = ObjectAnimator.ofFloat(binding.clContent, View.SCALE_X, 0.85f, 1f)
        val scaleYAnim = ObjectAnimator.ofFloat(binding.clContent, View.SCALE_Y, 0.85f, 1f)

        AnimatorSet().apply {
            playTogether(fadeAnim, scaleXAnim, scaleYAnim)
            duration = 600L
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun checkConfigAndSession() {
        lifecycleScope.launch {
            // Tampil minimal 1,2 detik sambil memeriksa sesi (PRD Bagian 9.3)
            delay(1200.milliseconds)

            // Validasi konfigurasi Supabase (PRD Bagian 6.1)
            val hasConfig = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank()
            if (!hasConfig) {
                binding.progressBar.gone()
                binding.tvConfigError.text = "Konfigurasi backend belum diisi, lihat README"
                binding.tvConfigError.visible()
                return@launch
            }

            val sessionManager = (application as SepiApp).appContainer.sessionManager
            val targetClass = if (sessionManager.isLoggedIn) {
                MainActivity::class.java
            } else {
                AuthActivity::class.java
            }

            val intent = Intent(this@SplashActivity, targetClass).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
