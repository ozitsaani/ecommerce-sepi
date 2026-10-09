package com.sepi.market.ui.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sepi.market.databinding.DialogFilterSheetBinding
import com.sepi.market.ui.common.GlassBottomSheet

/**
 * Bottom Sheet Kaca untuk filter rentang harga dan rating minimal produk (PRD Bagian 9.3 Screen 5).
 */
class FilterSheet(
    private val initialMinPrice: Long?,
    private val initialMaxPrice: Long?,
    private val initialMinRating4: Boolean,
    private val onApply: (minPrice: Long?, maxPrice: Long?, minRating4: Boolean) -> Unit,
    private val onReset: () -> Unit
) : GlassBottomSheet() {

    private var _binding: DialogFilterSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFilterSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etMinPrice.setText(initialMinPrice?.toString() ?: "")
        binding.etMaxPrice.setText(initialMaxPrice?.toString() ?: "")
        binding.cbMinRating.isChecked = initialMinRating4

        binding.btnApplyFilter.setOnClickListener {
            val minP = binding.etMinPrice.text?.toString()?.toLongOrNull()
            val maxP = binding.etMaxPrice.text?.toString()?.toLongOrNull()
            val rating4 = binding.cbMinRating.isChecked
            onApply(minP, maxP, rating4)
            dismiss()
        }

        binding.btnResetFilter.setOnClickListener {
            onReset()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
