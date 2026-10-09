package com.sepi.market.ui.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sepi.market.data.remote.dto.AddressDto
import com.sepi.market.databinding.DialogAddressPickerSheetBinding
import com.sepi.market.ui.common.GlassBottomSheet
import com.sepi.market.ui.profile.AddressAdapter

/**
 * Bottom Sheet Kaca untuk memilih alamat pengiriman saat checkout (PRD Bagian 9.3 Screen 8).
 */
class AddressPickerSheet(
    private val addresses: List<AddressDto>,
    private val onAddressSelected: (AddressDto) -> Unit,
    private val onAddNewAddressClick: () -> Unit
) : GlassBottomSheet() {

    private var _binding: DialogAddressPickerSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddressPickerSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = AddressAdapter { address ->
            onAddressSelected(address)
            dismiss()
        }
        binding.rvAddresses.adapter = adapter
        adapter.submitList(addresses)

        binding.btnAddAddress.setOnClickListener {
            dismiss()
            onAddNewAddressClick()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
