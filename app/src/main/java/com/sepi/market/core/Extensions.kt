package com.sepi.market.core

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import androidx.core.content.ContextCompat
import coil.load
import com.sepi.market.R

/**
 * Kumpulan extension functions untuk View, Coil Image Loader, dan mikro-animasi.
 */

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.showIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

/**
 * Memuat gambar URL dengan Coil, menggunakan warna sky sebagai placeholder dan ikon broken saat gagal.
 */
fun ImageView.loadImage(url: String?) {
    val skyPlaceholder = ColorDrawable(ContextCompat.getColor(context, R.color.sepi_sky_100))
    load(url) {
        crossfade(200)
        placeholder(skyPlaceholder)
        error(R.drawable.ic_image_broken)
    }
}

/**
 * Animasi bounce 250ms untuk badge keranjang saat bertambah (PRD Bagian 10.8).
 */
fun View.bounce(durationMs: Long = 250L) {
    val scaleX = ObjectAnimator.ofFloat(this, View.SCALE_X, 1f, 1.4f, 1f)
    val scaleY = ObjectAnimator.ofFloat(this, View.SCALE_Y, 1f, 1.4f, 1f)
    AnimatorSet().apply {
        playTogether(scaleX, scaleY)
        duration = durationMs
        interpolator = OvershootInterpolator()
        start()
    }
}

/**
 * Helper untuk mencegah klik ganda cepat (throttling 500ms).
 */
fun View.setOnSingleClickListener(debounceTime: Long = 500L, action: (View) -> Unit) {
    var lastClickTime: Long = 0
    setOnClickListener { v ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= debounceTime) {
            lastClickTime = currentTime
            action(v)
        }
    }
}

/**
 * NavOptions dengan transisi fade 200 ms sesuai PRD Bagian 9.2.
 */
fun defaultNavOptions(): androidx.navigation.NavOptions =
    androidx.navigation.NavOptions.Builder()
        .setEnterAnim(R.anim.nav_fade_enter)
        .setExitAnim(R.anim.nav_fade_exit)
        .setPopEnterAnim(R.anim.nav_fade_enter)
        .setPopExitAnim(R.anim.nav_fade_exit)
        .build()

fun androidx.navigation.NavController.navigateWithFade(
    @androidx.annotation.IdRes resId: Int,
    args: android.os.Bundle? = null,
    navOptions: androidx.navigation.NavOptions? = defaultNavOptions()
) {
    navigate(resId, args, navOptions)
}
