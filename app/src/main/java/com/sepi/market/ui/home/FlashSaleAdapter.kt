package com.sepi.market.ui.home

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sepi.market.core.Formatters
import com.sepi.market.core.loadImage
import com.sepi.market.core.showIf
import com.sepi.market.data.remote.dto.ProductDto
import com.sepi.market.databinding.ItemFlashSaleCardBinding

/**
 * Adapter kartu Flash Sale horizontal (148dp, foto 1:1, badge diskon, tanpa hati).
 */
class FlashSaleAdapter(
    private val onProductClick: (ProductDto) -> Unit
) : ListAdapter<ProductDto, FlashSaleAdapter.FlashSaleViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ProductDto>() {
            override fun areItemsTheSame(a: ProductDto, b: ProductDto) = a.id == b.id
            override fun areContentsTheSame(a: ProductDto, b: ProductDto) = a == b
        }
    }

    inner class FlashSaleViewHolder(val binding: ItemFlashSaleCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductDto) {
            binding.root.setOnClickListener { onProductClick(product) }
            binding.ivProduct.loadImage(product.imageUrl)
            binding.tvProductName.text = product.name
            binding.tvPrice.text = Formatters.formatRupiah(product.price)

            val hasDiscount = product.originalPrice != null && product.originalPrice > product.price
            binding.tvDiscountBadge.showIf(hasDiscount)
            if (hasDiscount && product.originalPrice != null) {
                val pct = Formatters.discountPercent(product.price, product.originalPrice)
                binding.tvDiscountBadge.text = "-$pct%"
                binding.tvOriginalPrice.text = Formatters.formatRupiah(product.originalPrice)
                binding.tvOriginalPrice.paintFlags =
                    binding.tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvOriginalPrice.visibility = View.VISIBLE
            } else {
                binding.tvOriginalPrice.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlashSaleViewHolder {
        val binding = ItemFlashSaleCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FlashSaleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FlashSaleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
