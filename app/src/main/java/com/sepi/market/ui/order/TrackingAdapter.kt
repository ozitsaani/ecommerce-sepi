package com.sepi.market.ui.order

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sepi.market.core.Formatters
import com.sepi.market.core.showIf
import com.sepi.market.data.remote.dto.TrackingDto
import com.sepi.market.databinding.ItemTrackingTimelineBinding

/**
 * ListAdapter RecyclerView untuk timeline vertikal pelacakan pesanan (PRD Bagian 9.3 Screen 10).
 */
class TrackingAdapter : ListAdapter<TrackingDto, TrackingAdapter.TrackingViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackingViewHolder {
        val binding = ItemTrackingTimelineBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TrackingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrackingViewHolder, position: Int) {
        holder.bind(getItem(position), isLastItem = position == itemCount - 1)
    }

    inner class TrackingViewHolder(
        private val binding: ItemTrackingTimelineBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tracking: TrackingDto, isLastItem: Boolean) {
            binding.tvTrackingTitle.text = tracking.title
            binding.tvTrackingDesc.text = tracking.description
            binding.tvTrackingTime.text = Formatters.relativeTime(tracking.createdAt)
            binding.lineTimeline.showIf(!isLastItem)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<TrackingDto>() {
        override fun areItemsTheSame(oldItem: TrackingDto, newItem: TrackingDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TrackingDto, newItem: TrackingDto): Boolean {
            return oldItem == newItem
        }
    }
}
