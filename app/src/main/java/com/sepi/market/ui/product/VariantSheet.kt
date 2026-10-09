package com.sepi.market.ui.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.chip.Chip
import com.sepi.market.R
import com.sepi.market.core.Formatters
import com.sepi.market.core.loadImage
import com.sepi.market.core.showIf
import com.sepi.market.data.remote.dto.ProductDetailDto
import com.sepi.market.data.remote.dto.VariantDto
import com.sepi.market.databinding.DialogVariantSheetBinding
import com.sepi.market.ui.common.GlassBottomSheet
import kotlin.math.min

/**
 * Bottom Sheet Kaca untuk memilih varian dan jumlah sebelum menambah ke keranjang/beli sekarang (PRD Bagian 9.3 Screen 6).
 */
class VariantSheet(
    private val product: ProductDetailDto,
    private val buyNow: Boolean,
    private val onConfirm: (selectedVariant: VariantDto?, quantity: Int, buyNow: Boolean) -> Unit
) : GlassBottomSheet() {

    private var _binding: DialogVariantSheetBinding? = null
    private val binding get() = _binding!!

    private var selectedVariant: VariantDto? = product.variants?.firstOrNull()
    private var quantity: Int = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogVariantSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupProductInfo()
        setupVariants()
        setupQuantityStepper()
        setupConfirmButton()
    }

    private fun setupProductInfo() {
        binding.ivSheetProductImage.loadImage(product.imageUrl)
        updatePriceAndStock()
    }

    private fun updatePriceAndStock() {
        val delta = selectedVariant?.priceDelta ?: 0L
        val totalPrice = product.price + delta
        binding.tvSheetPrice.text = Formatters.formatRupiah(totalPrice)
        binding.tvSheetStock.text = getString(R.string.stock_count, product.stock)
    }

    private fun setupVariants() {
        val variants = product.variants
        val hasVariants = !variants.isNullOrEmpty()
        binding.layoutVariantGroup.showIf(hasVariants)

        if (hasVariants && variants != null) {
            val groupName = variants.firstOrNull()?.groupName ?: getString(R.string.select_variant)
            binding.tvVariantGroupName.text = groupName

            binding.chipGroupVariants.removeAllViews()
            variants.forEachIndexed { index, variant ->
                val chip = Chip(requireContext()).apply {
                    text = if (variant.priceDelta > 0) {
                        "${variant.optionName} (+${Formatters.formatRupiah(variant.priceDelta)})"
                    } else {
                        variant.optionName
                    }
                    isCheckable = true
                    id = index
                    isChecked = index == 0
                }
                binding.chipGroupVariants.addView(chip)
            }

            binding.chipGroupVariants.setOnCheckedStateChangeListener { _, checkedIds ->
                val selectedIdx = checkedIds.firstOrNull() ?: -1
                selectedVariant = if (selectedIdx in variants.indices) {
                    variants[selectedIdx]
                } else null
                updatePriceAndStock()
            }
        }
    }

    private fun setupQuantityStepper() {
        val maxStock = min(product.stock, 99)
        binding.tvQuantity.text = quantity.toString()

        binding.btnMinus.setOnClickListener {
            if (quantity > 1) {
                quantity--
                binding.tvQuantity.text = quantity.toString()
            }
        }

        binding.btnPlus.setOnClickListener {
            if (quantity < maxStock) {
                quantity++
                binding.tvQuantity.text = quantity.toString()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.stock_remaining, maxStock),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupConfirmButton() {
        binding.btnConfirmVariant.text = if (buyNow) {
            getString(R.string.btn_buy_now)
        } else {
            getString(R.string.btn_add_to_cart)
        }

        binding.btnConfirmVariant.setOnClickListener {
            val hasVariants = !product.variants.isNullOrEmpty()
            if (hasVariants && selectedVariant == null) {
                Toast.makeText(requireContext(), getString(R.string.select_variant), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onConfirm(selectedVariant, quantity, buyNow)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
