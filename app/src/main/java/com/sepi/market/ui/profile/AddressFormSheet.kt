package com.sepi.market.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.sepi.market.R
import com.sepi.market.data.remote.dto.AddressDto
import com.sepi.market.databinding.DialogAddressFormSheetBinding
import com.sepi.market.ui.common.GlassBottomSheet

/**
 * Bottom Sheet Kaca untuk menambah atau mengubah alamat pengiriman Cirebon (PRD Bagian 9.3 Screen 11).
 */
class AddressFormSheet(
    private val addressToEdit: AddressDto? = null,
    private val onSave: (address: AddressDto) -> Unit
) : GlassBottomSheet() {

    private var _binding: DialogAddressFormSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddressFormSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (addressToEdit != null) {
            binding.tvFormTitle.text = getString(R.string.edit_address_title)
            binding.etLabel.setText(addressToEdit.label)
            binding.etRecipientName.setText(addressToEdit.recipientName)
            binding.etPhone.setText(addressToEdit.phone)
            binding.etStreet.setText(addressToEdit.street)
            binding.etDistrict.setText(addressToEdit.district)
            binding.etPostalCode.setText(addressToEdit.postalCode)
            binding.switchDefault.isChecked = addressToEdit.isDefault
        } else {
            binding.tvFormTitle.text = getString(R.string.add_new_address)
        }

        binding.btnSaveAddress.setOnClickListener {
            val label = binding.etLabel.text?.toString()?.trim().orEmpty().ifBlank { "Rumah" }
            val name = binding.etRecipientName.text?.toString()?.trim().orEmpty()
            val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
            val street = binding.etStreet.text?.toString()?.trim().orEmpty()
            val district = binding.etDistrict.text?.toString()?.trim().orEmpty()
            val postalCode = binding.etPostalCode.text?.toString()?.trim().orEmpty()
            val isDefault = binding.switchDefault.isChecked

            if (name.isBlank()) {
                Toast.makeText(requireContext(), "Nama penerima tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (phone.isBlank()) {
                Toast.makeText(requireContext(), "Nomor telepon tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val phoneRegex = Regex("""^(\+62|62|0)8[0-9]{8,12}$""")
            if (!phoneRegex.matches(phone)) {
                Toast.makeText(requireContext(), "Nomor telepon tidak valid (contoh: 081234567890)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (street.isBlank()) {
                Toast.makeText(requireContext(), "Alamat jalan tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val postalRegex = Regex("""^[0-9]{5}$""")
            if (postalCode.isNotBlank() && !postalRegex.matches(postalCode)) {
                Toast.makeText(requireContext(), "Kode pos harus 5 digit angka", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newAddress = AddressDto(
                id = addressToEdit?.id,
                label = label,
                recipientName = name,
                phone = phone,
                street = street,
                district = district,
                city = "Cirebon",
                postalCode = postalCode,
                isDefault = isDefault
            )

            onSave(newAddress)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
