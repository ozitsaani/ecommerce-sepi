package com.sepi.market.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sepi.market.R
import com.sepi.market.data.remote.dto.CategoryDto
import com.sepi.market.databinding.ItemCategoryChipBinding

/**
 * ListAdapter RecyclerView untuk baris horizontal ubin lingkaran kategori di Beranda (design/01_beranda.png).
 */
class HomeCategoryAdapter(
    private val onCategoryClick: (CategoryDto) -> Unit
) : ListAdapter<CategoryDto, HomeCategoryAdapter.HomeCategoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeCategoryViewHolder {
        val binding = ItemCategoryChipBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HomeCategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeCategoryViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class HomeCategoryViewHolder(
        private val binding: ItemCategoryChipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: CategoryDto, position: Int) {
            binding.tvCategoryName.text = category.name
            binding.ivCategoryIcon.setImageResource(getCategoryIconRes(category.iconKey))

            val circleBgRes = when (position % 4) {
                0 -> R.drawable.bg_clay_circle_blush
                1 -> R.drawable.bg_clay_circle_butter
                2 -> R.drawable.bg_clay_circle_lavender
                else -> R.drawable.bg_clay_circle_sky
            }
            binding.layoutCategoryTile.setBackgroundResource(circleBgRes)

            binding.root.setOnClickListener {
                onCategoryClick(category)
            }
        }

        private fun getCategoryIconRes(key: String): Int {
            return when (key.lowercase()) {
                "headphones" -> R.drawable.ic_cat_headphones
                "shirt" -> R.drawable.ic_cat_shirt
                "dress" -> R.drawable.ic_cat_dress
                "shoe" -> R.drawable.ic_cat_shoe
                "bag" -> R.drawable.ic_cat_bag
                "home" -> R.drawable.ic_cat_home
                "sparkles" -> R.drawable.ic_cat_sparkles
                "ball" -> R.drawable.ic_cat_ball
                else -> R.drawable.ic_cat_tag
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CategoryDto>() {
        override fun areItemsTheSame(oldItem: CategoryDto, newItem: CategoryDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CategoryDto, newItem: CategoryDto): Boolean {
            return oldItem == newItem
        }
    }
}
