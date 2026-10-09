package com.sepi.market.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sepi.market.core.showIf
import com.sepi.market.data.remote.dto.AddressDto
import com.sepi.market.databinding.ItemAddressCardBinding

/**
 * ListAdapter RecyclerView untuk daftar alamat pengiriman pengguna.
 */
class AddressAdapter(
    private val onAddressClick: (AddressDto) -> Unit
) : ListAdapter<AddressDto, AddressAdapter.AddressViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val binding = ItemAddressCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AddressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AddressViewHolder(
        private val binding: ItemAddressCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(address: AddressDto) {
            binding.tvAddressLabel.text = address.label
            binding.tvDefaultBadge.showIf(address.isDefault)
            binding.tvRecipient.text = "${address.recipientName} (${address.phone})"
            binding.tvFullAddress.text = "${address.street}, ${address.district}, ${address.city} ${address.postalCode}".trimEnd()

            binding.root.setOnClickListener {
                onAddressClick(address)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<AddressDto>() {
        override fun areItemsTheSame(oldItem: AddressDto, newItem: AddressDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AddressDto, newItem: AddressDto): Boolean {
            return oldItem == newItem
        }
    }
}
