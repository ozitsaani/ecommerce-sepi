package com.sepi.market.ui.review

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sepi.market.core.Formatters
import com.sepi.market.data.remote.dto.ReviewDto
import com.sepi.market.databinding.ItemReviewCardBinding

/**
 * ListAdapter RecyclerView untuk menampilkan daftar ulasan pembeli (PRD Bagian 9.3 Screen 12).
 */
class ReviewAdapter : ListAdapter<ReviewDto, ReviewAdapter.ReviewViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReviewViewHolder(
        private val binding: ItemReviewCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(review: ReviewDto) {
            val name = review.reviewerName.orEmpty().ifBlank { "Pengguna Sepi" }
            binding.tvReviewerName.text = name
            binding.tvAvatarInitial.text = name.take(1).uppercase()

            binding.ratingBar.rating = review.rating.toFloat()
            binding.tvReviewTime.text = Formatters.relativeTime(review.createdAt)
            binding.tvReviewComment.text = review.comment
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ReviewDto>() {
        override fun areItemsTheSame(oldItem: ReviewDto, newItem: ReviewDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ReviewDto, newItem: ReviewDto): Boolean {
            return oldItem == newItem
        }
    }
}
