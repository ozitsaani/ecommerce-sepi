package com.sepi.market.ui.order

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sepi.market.core.Formatters
import com.sepi.market.core.loadImage
import com.sepi.market.core.showIf
import com.sepi.market.data.remote.dto.OrderItemDto
import com.sepi.market.databinding.ItemOrderDetailProductBinding

data class OrderItemUiModel(
    val item: OrderItemDto,
    val isDelivered: Boolean
)

/**
 * ListAdapter RecyclerView untuk menampilkan daftar item dalam detail pesanan (PRD Bagian 9.3 Screen 10).
 */
class OrderItemAdapter(
    private val onReviewClick: (OrderItemDto) -> Unit
) : ListAdapter<OrderItemUiModel, OrderItemAdapter.OrderItemViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderItemViewHolder {
        val binding = ItemOrderDetailProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OrderItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderItemViewHolder(
        private val binding: ItemOrderDetailProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uiModel: OrderItemUiModel) {
            val item = uiModel.item
            binding.ivProductImage.loadImage(item.imageUrl)
            binding.tvProductName.text = item.productName

            val hasVariant = !item.variantLabel.isNullOrBlank()
            binding.tvVariantLabel.showIf(hasVariant)
            if (hasVariant) {
                binding.tvVariantLabel.text = item.variantLabel
            }

            binding.tvQuantityAndPrice.text = "${item.quantity}x ${Formatters.formatRupiah(item.unitPrice)}"

            // Tombol tulis ulasan hanya aktif jika status pesanan DELIVERED dan productId tersedia
            val canReview = uiModel.isDelivered && !item.productId.isNullOrBlank()
            binding.btnWriteReview.showIf(canReview)
            binding.btnWriteReview.setOnClickListener {
                onReviewClick(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<OrderItemUiModel>() {
        override fun areItemsTheSame(oldItem: OrderItemUiModel, newItem: OrderItemUiModel): Boolean {
            return oldItem.item.id == newItem.item.id
        }

        override fun areContentsTheSame(oldItem: OrderItemUiModel, newItem: OrderItemUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
