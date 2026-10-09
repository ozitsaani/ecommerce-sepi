package com.sepi.market.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sepi.market.R
import com.sepi.market.data.remote.dto.VoucherDto
import com.sepi.market.databinding.ItemVoucherCardBinding

/**
 * ListAdapter RecyclerView untuk carousel/daftar voucher promo.
 */
class VoucherAdapter(
    private val onCopyClick: (VoucherDto) -> Unit
) : ListAdapter<VoucherDto, VoucherAdapter.VoucherViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherViewHolder {
        val binding = ItemVoucherCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        val lm = (parent as? RecyclerView)?.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
        if (lm?.orientation == RecyclerView.VERTICAL) {
            val lp = binding.root.layoutParams as? ViewGroup.MarginLayoutParams
            lp?.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp?.marginEnd = 0
            lp?.bottomMargin = parent.context.resources.getDimensionPixelSize(R.dimen.spacing_sm)
            binding.root.layoutParams = lp
        }
        return VoucherViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VoucherViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VoucherViewHolder(
        private val binding: ItemVoucherCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(voucher: VoucherDto) {
            binding.tvVoucherTitle.text = voucher.title
            binding.tvVoucherDesc.text = voucher.description
            binding.tvVoucherCode.text = "KODE: ${voucher.code}"

            binding.btnCopyCode.setOnClickListener {
                onCopyClick(voucher)
            }
            binding.root.setOnClickListener {
                onCopyClick(voucher)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<VoucherDto>() {
        override fun areItemsTheSame(oldItem: VoucherDto, newItem: VoucherDto): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: VoucherDto, newItem: VoucherDto): Boolean {
            return oldItem == newItem
        }
    }
}
