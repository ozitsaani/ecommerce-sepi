package com.sepi.market.ui.product

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sepi.market.R
import com.sepi.market.core.Formatters
import com.sepi.market.core.loadImage
import com.sepi.market.core.showIf
import com.sepi.market.data.remote.dto.ProductDto
import com.sepi.market.databinding.ItemProductCardBinding

/**
 * ListAdapter RecyclerView untuk menampilkan produk dalam bentuk kartu grid 2 kolom (PRD Bagian 9.1).
 * Menggunakan DiffUtil, tanpa notifyDataSetChanged().
 */
class ProductAdapter(
    private val onProductClick: (ProductDto) -> Unit,
    private val onFavoriteClick: (ProductDto) -> Unit
) : ListAdapter<ProductDto, ProductAdapter.ProductViewHolder>(DiffCallback) {

    private var favoriteIds: Set<String> = emptySet()

    fun setFavoriteIds(ids: Set<String>) {
        this.favoriteIds = ids
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(
        private val binding: ItemProductCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductDto) {
            binding.ivProduct.loadImage(product.imageUrl)
            binding.tvProductName.text = product.name
            binding.tvPrice.text = Formatters.formatRupiah(product.price)

            // Coretan harga asli & badge diskon
            val hasDiscount = product.originalPrice != null && product.originalPrice > product.price
            binding.layoutDiscount.showIf(hasDiscount)
            if (hasDiscount && product.originalPrice != null) {
                binding.tvOriginalPrice.text = Formatters.formatRupiah(product.originalPrice)
                binding.tvOriginalPrice.paintFlags = binding.tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                val percent = Formatters.discountPercent(product.price, product.originalPrice)
                binding.tvDiscountBadge.text = "$percent%"
            }

            // Rating & Jumlah Terjual
            binding.tvRating.text = String.format("%.1f", product.ratingAvg)
            binding.tvSoldCount.text = binding.root.context.getString(
                R.string.sold_count, product.soldCount
            )

            // Status Ikon Favorit
            val isFavorite = favoriteIds.contains(product.id)
            binding.ivFavorite.setImageResource(
                if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
            )

            binding.root.setOnClickListener {
                onProductClick(product)
            }

            binding.ivFavorite.setOnClickListener {
                onFavoriteClick(product)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ProductDto>() {
        override fun areItemsTheSame(oldItem: ProductDto, newItem: ProductDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ProductDto, newItem: ProductDto): Boolean {
            return oldItem == newItem
        }
    }
}
