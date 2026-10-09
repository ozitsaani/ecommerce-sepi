package com.sepi.market.ui.cart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sepi.market.R
import com.sepi.market.core.Formatters
import com.sepi.market.core.loadImage
import com.sepi.market.core.showIf
import com.sepi.market.data.remote.dto.CartItemDto
import com.sepi.market.databinding.ItemCartCardBinding

/**
 * ListAdapter RecyclerView untuk menampilkan daftar item di keranjang belanja.
 */
class CartAdapter(
    private val onSelectChange: (CartItemDto) -> Unit,
    private val onQuantityChange: (CartItemDto, Int) -> Unit,
    private val onDeleteClick: (CartItemDto) -> Unit
) : ListAdapter<CartItemDto, CartAdapter.CartViewHolder>(DiffCallback) {

    private var selectedIds: Set<String> = emptySet()

    fun setSelectedIds(ids: Set<String>) {
        this.selectedIds = ids
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartViewHolder(
        private val binding: ItemCartCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItemDto) {
            val product = item.product
            val variant = item.variant

            binding.cbSelect.isChecked = selectedIds.contains(item.id)

            binding.ivCartProductImage.loadImage(product?.imageUrl)
            binding.tvCartProductName.text = product?.name ?: "Produk"

            val hasVariant = variant != null
            binding.tvCartVariantLabel.showIf(hasVariant)
            if (hasVariant && variant != null) {
                binding.tvCartVariantLabel.text = "${variant.groupName}: ${variant.optionName}"
            }

            val delta = variant?.priceDelta ?: 0L
            val unitPrice = (product?.price ?: 0L) + delta
            binding.tvCartUnitPrice.text = Formatters.formatRupiah(unitPrice)

            binding.tvQuantity.text = item.quantity.toString()

            val stock = product?.stock ?: 0
            val isExceedingStock = item.quantity > stock
            binding.tvStockWarning.showIf(isExceedingStock)
            if (isExceedingStock) {
                binding.tvStockWarning.text = binding.root.context.getString(
                    R.string.stock_remaining, stock
                )
            }

            binding.cbSelect.setOnClickListener {
                onSelectChange(item)
            }

            binding.btnMinus.setOnClickListener {
                if (item.quantity > 1) {
                    onQuantityChange(item, item.quantity - 1)
                }
            }

            binding.btnPlus.setOnClickListener {
                if (item.quantity < stock) {
                    onQuantityChange(item, item.quantity + 1)
                }
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(item)
            }
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
