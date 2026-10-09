package com.sepi.market.ui.order

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sepi.market.R
import com.sepi.market.core.Formatters
import com.sepi.market.core.loadImage
import com.sepi.market.core.showIf
import com.sepi.market.data.remote.dto.OrderDto
import com.sepi.market.databinding.ItemOrderCardBinding

/**
 * ListAdapter RecyclerView untuk daftar pesanan pengguna.
 */
class OrderAdapter(
    private val onOrderClick: (OrderDto) -> Unit
) : ListAdapter<OrderDto, OrderAdapter.OrderViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderViewHolder(
        private val binding: ItemOrderCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: OrderDto) {
            val context = binding.root.context
            binding.tvOrderCode.text = order.orderCode
            binding.tvOrderTotal.text = Formatters.formatRupiah(order.total)

            val statusText: String
            val textColor: Int
            val bgColor: Int

            when (order.status.uppercase()) {
                "UNPAID" -> {
                    val isExpired = try {
                        val deadline = java.time.OffsetDateTime.parse(order.paymentDeadline)
                        java.time.OffsetDateTime.now().isAfter(deadline)
                    } catch (e: Exception) {
                        false
                    }
                    if (isExpired) {
                        statusText = context.getString(R.string.order_expired_badge)
                        textColor = R.color.status_cancelled_text
                        bgColor = R.color.status_cancelled_bg
                    } else {
                        statusText = context.getString(R.string.tab_unpaid)
                        textColor = R.color.status_unpaid_text
                        bgColor = R.color.status_unpaid_bg
                    }
                }
                "PAID", "PACKED" -> {
                    statusText = context.getString(R.string.tab_process)
                    textColor = R.color.status_paid_text
                    bgColor = R.color.status_paid_bg
                }
                "SHIPPED" -> {
                    statusText = context.getString(R.string.tab_shipped)
                    textColor = R.color.status_shipped_text
                    bgColor = R.color.status_shipped_bg
                }
                "DELIVERED" -> {
                    statusText = context.getString(R.string.tab_delivered)
                    textColor = R.color.status_delivered_text
                    bgColor = R.color.status_delivered_bg
                }
                else -> {
                    statusText = context.getString(R.string.tab_cancelled)
                    textColor = R.color.status_cancelled_text
                    bgColor = R.color.status_cancelled_bg
                }
            }

            binding.tvStatusBadge.text = statusText
            binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, textColor))
            binding.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, bgColor)
            )

            // First item info
            val items = order.items.orEmpty()
            val firstItem = items.firstOrNull()
            binding.ivOrderProductImage.loadImage(firstItem?.imageUrl)
            binding.tvOrderProductName.text = firstItem?.productName ?: "Pesanan"

            val otherCount = items.size - 1
            val hasOtherItems = otherCount > 0
            binding.tvOtherProductsCount.showIf(hasOtherItems)
            if (hasOtherItems) {
                binding.tvOtherProductsCount.text = context.getString(R.string.other_products, otherCount)
            }

            binding.root.setOnClickListener {
                onOrderClick(order)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<OrderDto>() {
        override fun areItemsTheSame(oldItem: OrderDto, newItem: OrderDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: OrderDto, newItem: OrderDto): Boolean {
            return oldItem == newItem
        }
    }
}
