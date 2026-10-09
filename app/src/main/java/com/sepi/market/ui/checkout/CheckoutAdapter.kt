package com.sepi.market.ui.checkout

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sepi.market.core.Formatters
import com.sepi.market.core.gone
import com.sepi.market.core.loadImage
import com.sepi.market.core.visible
import com.sepi.market.data.remote.dto.CartItemDto
import com.sepi.market.databinding.ItemCheckoutProductBinding

/**
 * Adapter khusus untuk menampilkan ringkasan produk dipesan pada layar Checkout (PRD 9.3 & design 05_checkout.png).
 */
class CheckoutAdapter : ListAdapter<CartItemDto, CheckoutAdapter.ViewHolder>(DiffCallback) {

    fun setSelectedIds(ids: Set<String>) {
        // Compatibility method for CheckoutFragment
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCheckoutProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemCheckoutProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItemDto) {
            val product = item.product
            val variant = item.variant

            binding.ivCheckoutProductImage.loadImage(product?.imageUrl)
            binding.tvCheckoutProductName.text = product?.name ?: "Produk"

            if (variant != null && variant.optionName.isNotBlank()) {
                binding.tvCheckoutVariant.visible()
                binding.tvCheckoutVariant.text = if (variant.groupName.isNotBlank()) {
                    "${variant.groupName}: ${variant.optionName}"
                } else {
                    variant.optionName
                }
            } else {
                binding.tvCheckoutVariant.gone()
            }

            val delta = variant?.priceDelta ?: 0L
            val unitPrice = (product?.price ?: 0L) + delta
            binding.tvCheckoutPrice.text = "${item.quantity} × ${Formatters.formatRupiah(unitPrice)}"
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CartItemDto>() {
        override fun areItemsTheSame(oldItem: CartItemDto, newItem: CartItemDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CartItemDto, newItem: CartItemDto): Boolean {
            return oldItem == newItem
        }
    }
}
