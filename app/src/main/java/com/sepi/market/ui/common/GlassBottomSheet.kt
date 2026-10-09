package com.sepi.market.ui.common

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sepi.market.R

/**
 * Kelas dasar BottomSheetDialogFragment dengan dukungan efek Blur fisik pada API 31+ (PRD Bagian 10.7 & SEPI-18).
 * Perangkat dengan API 24-30 menggunakan fallback semi-transparan tanpa efek blur fisik demi performa dan stabilitas.
 */
abstract class GlassBottomSheet : BottomSheetDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Sepi_BottomSheetDialog)
    }

    override fun onStart() {
        super.onStart()
        applyWindowBlurIfSupported()
    }

    private fun applyWindowBlurIfSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val window = dialog?.window ?: return
            val windowManager = requireContext().getSystemService(WindowManager::class.java)
            if (windowManager != null && windowManager.isCrossWindowBlurEnabled) {
                try {
                    window.setBackgroundBlurRadius(48)
                } catch (_: Exception) {
                    // Abaikan jika perangkat tidak mendukung efek blur secara hardware
                }
            }
        }
    }
}
