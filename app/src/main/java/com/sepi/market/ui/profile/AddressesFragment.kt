package com.sepi.market.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.sepi.market.R
import com.sepi.market.SepiApp
import com.sepi.market.ui.common.SkeletonHelper
import com.sepi.market.core.UiState
import com.sepi.market.core.gone
import com.sepi.market.core.visible
import com.sepi.market.data.remote.dto.AddressDto
import com.sepi.market.databinding.FragmentAddressesBinding
import kotlinx.coroutines.launch

/**
 * Fragment Layar Daftar Alamat Pengiriman Pengguna dengan aksi CRUD (PRD Bagian 9.3 Screen 11).
 */
class AddressesFragment : Fragment() {

    private var _binding: FragmentAddressesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddressesViewModel by viewModels {
        (requireActivity().application as SepiApp).appContainer.viewModelFactory
    }

    private lateinit var addressAdapter: AddressAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddressesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupListeners()
        observeViewModel()

        viewModel.loadAddresses()
    }

    private fun setupAdapter() {
        addressAdapter = AddressAdapter { address ->
            showEditOrDeleteDialog(address)
        }
        binding.rvAddresses.adapter = addressAdapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadAddresses(forceRefresh = true)
        }

        binding.btnAddAddressTop.setOnClickListener {
            openAddressFormSheet(null)
        }

        binding.btnAddAddressEmpty.setOnClickListener {
            openAddressFormSheet(null)
        }
    }

    private fun openAddressFormSheet(addressToEdit: AddressDto?) {
        val sheet = AddressFormSheet(
            addressToEdit = addressToEdit,
            onSave = { address ->
                if (addressToEdit != null && addressToEdit.id != null) {
                    viewModel.updateAddress(addressToEdit.id, address)
                } else {
                    viewModel.createAddress(address)
                }
            }
        )
        sheet.show(childFragmentManager, "AddressFormSheet")
    }

    private fun showEditOrDeleteDialog(address: AddressDto) {
        val options = arrayOf("Ubah Alamat", "Hapus Alamat")
        AlertDialog.Builder(requireContext())
            .setTitle(address.label)
            .setItems(options) { dialog, which ->
                dialog.dismiss()
                if (which == 0) {
                    openAddressFormSheet(address)
                } else {
                    if (address.id != null) {
                        AlertDialog.Builder(requireContext())
                            .setTitle(R.string.delete_address_confirm)
                            .setMessage("Apakah kamu yakin ingin menghapus alamat ini?")
                            .setPositiveButton(R.string.delete) { confirmDialog, _ ->
                                confirmDialog.dismiss()
                                viewModel.deleteAddress(address.id)
                            }
                            .setNegativeButton(R.string.cancel) { confirmDialog, _ ->
                                confirmDialog.dismiss()
                            }
                            .show()
                    }
                }
            }
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.addressesState.collect { state ->
                        handleState(state)
                    }
                }
                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            is ProfileUiEvent.ShowToast -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun handleState(state: UiState<List<AddressDto>>) {
        binding.swipeRefresh.isRefreshing = false
        when (state) {
            is UiState.Loading -> {
                SkeletonHelper.showSkeleton(binding.shimmerAddresses, binding.rvAddresses)
                binding.errorView.gone()
                binding.layoutEmpty.gone()
            }
            is UiState.Success -> {
                SkeletonHelper.hideSkeleton(binding.shimmerAddresses, binding.rvAddresses)
                binding.errorView.gone()

                val list = state.data
                if (list.isEmpty()) {
                    binding.rvAddresses.gone()
                    binding.layoutEmpty.visible()
                } else {
                    binding.layoutEmpty.gone()
                    binding.rvAddresses.visible()
                    addressAdapter.submitList(list)
                }
            }
            is UiState.Error -> {
                SkeletonHelper.hideSkeleton(binding.shimmerAddresses, binding.rvAddresses)
                binding.rvAddresses.gone()
                binding.layoutEmpty.gone()
                binding.errorView.visible()
                binding.errorView.setError(state.message) {
                    viewModel.loadAddresses()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
