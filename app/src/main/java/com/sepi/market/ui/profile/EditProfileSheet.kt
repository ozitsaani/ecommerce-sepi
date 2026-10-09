package com.sepi.market.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.sepi.market.databinding.DialogEditProfileSheetBinding
import com.sepi.market.ui.common.GlassBottomSheet

/**
 * Bottom Sheet Kaca untuk mengubah nama dan nomor telepon profil pengguna (PRD Bagian 9.3 Screen 11).
 */
class EditProfileSheet(
    private val currentName: String,
    private val currentPhone: String?,
    private val onSave: (fullName: String, phone: String?) -> Unit
) : GlassBottomSheet() {

    private var _binding: DialogEditProfileSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditProfileSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etEditFullName.setText(currentName)
        binding.etEditPhone.setText(currentPhone.orEmpty())

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etEditFullName.text?.toString()?.trim().orEmpty()
            val phone = binding.etEditPhone.text?.toString()?.trim()

            if (name.isBlank()) {
                Toast.makeText(requireContext(), "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            onSave(name, phone)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
