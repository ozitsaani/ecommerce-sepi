package com.sepi.market.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.sepi.market.R
import com.sepi.market.databinding.ViewErrorStateBinding

/**
 * Custom View untuk menampilkan kondisi error dengan pesan ramah dan tombol "Coba lagi" (PRD Bagian 9.1 & SEPI-16).
 */
class ErrorStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewErrorStateBinding = ViewErrorStateBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    fun setError(message: String, onRetry: () -> Unit) {
        binding.tvErrorMessage.text = message.ifBlank { context.getString(R.string.error_general) }
        binding.btnRetry.setOnClickListener { onRetry() }
    }

    fun setIcon(iconResId: Int) {
        binding.ivErrorIcon.setImageResource(iconResId)
    }
}
