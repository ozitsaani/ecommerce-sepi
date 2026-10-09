package com.sepi.market.ui.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.sepi.market.core.Formatters
import com.sepi.market.data.remote.dto.VoucherDto
import com.sepi.market.databinding.DialogVoucherPickerSheetBinding
import com.sepi.market.ui.common.GlassBottomSheet
import com.sepi.market.ui.profile.VoucherAdapter

/**
 * Bottom Sheet Kaca untuk memilih atau memasukkan kode voucher saat checkout (PRD Bagian 9.3 Screen 8).
 */
class VoucherPickerSheet(
    private val subtotal: Long,
    private val vouchers: List<VoucherDto>,
    private val onVoucherSelected: (VoucherDto?) -> Unit
) : GlassBottomSheet() {

    private var _binding: DialogVoucherPickerSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogVoucherPickerSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = VoucherAdapter { voucher ->
            applyVoucherIfValid(voucher)
        }
        binding.rvVouchers.adapter = adapter
        adapter.submitList(vouchers)

        binding.btnApplyCode.setOnClickListener {
            val code = binding.etVoucherCode.text?.toString()?.trim().orEmpty()
            if (code.isBlank()) {
                Toast.makeText(requireContext(), "Masukkan kode voucher", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val matching = vouchers.find { it.code.equals(code, ignoreCase = true) }
            if (matching != null) {
                applyVoucherIfValid(matching)
            } else {
                Toast.makeText(requireContext(), "Kode voucher tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyVoucherIfValid(voucher: VoucherDto) {
        if (subtotal < voucher.minSpend) {
            Toast.makeText(
                requireContext(),
                "Minimal belanja untuk voucher ini ${Formatters.formatRupiah(voucher.minSpend)}",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        onVoucherSelected(voucher)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
