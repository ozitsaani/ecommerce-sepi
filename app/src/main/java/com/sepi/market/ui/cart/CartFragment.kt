package com.sepi.market.ui.cart

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
import com.sepi.market.core.Formatters
import com.sepi.market.ui.common.SkeletonHelper
import com.sepi.market.core.UiState
import com.sepi.market.core.gone
import com.sepi.market.core.showIf
import com.sepi.market.core.visible
import com.sepi.market.data.remote.dto.CartItemDto
import com.sepi.market.databinding.FragmentCartBinding
import kotlinx.coroutines.launch

/**
 * Fragment Layar Keranjang Belanja dengan pengubah kuantitas, seleksi item, dan konfirmasi hapus (PRD Bagian 9.3 Screen 7).
 */
class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CartViewModel by viewModels {
        (requireActivity().application as SepiApp).appContainer.viewModelFactory
    }

    private lateinit var cartAdapter: CartAdapter
    private var preselectId: String? = null
    private var currentItemsList: List<CartItemDto> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preselectId = arguments?.getString("preselectId")

        setupAdapter()
        setupListeners()
        observeViewModel()

        viewModel.loadCartItems(preselectId)
    }

    private fun setupAdapter() {
        cartAdapter = CartAdapter(
            onSelectChange = { item ->
                viewModel.toggleSelectItem(item.id)
            },
            onQuantityChange = { item, newQty ->
                viewModel.updateQuantity(item.id, newQty)
            },
            onDeleteClick = { item ->
                showDeleteConfirmationDialog(item)
            }
        )
        binding.rvCartItems.adapter = cartAdapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadCartItems(preselectId, forceRefresh = true)
        }

        binding.btnStartShopping.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }

        binding.cbSelectAll.setOnClickListener {
            val isChecked = binding.cbSelectAll.isChecked
            viewModel.toggleSelectAll(isChecked)
        }

        binding.btnCheckout.setOnClickListener {
            val selectedSet = viewModel.selectedIds.value
            val selectedItems = currentItemsList.filter { selectedSet.contains(it.id) }

            if (selectedItems.isEmpty()) {
                Toast.makeText(requireContext(), "Pilih minimal satu item", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Periksa jika ada item yang melebihi stok
            val invalidStockItem = selectedItems.find { it.quantity > (it.product?.stock ?: 0) }
            if (invalidStockItem != null) {
                Toast.makeText(
                    requireContext(),
                    "Jumlah beberapa item melebihi stok tersedia",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val itemIdsArray = selectedItems.map { it.id }.toTypedArray()
            val args = Bundle().apply {
                putStringArray("cartItemIds", itemIdsArray)
            }
            findNavController().navigate(R.id.checkoutFragment, args)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.cartState.collect { state ->
                        handleState(state)
                    }
                }
                launch {
                    viewModel.selectedIds.collect { selectedSet ->
                        cartAdapter.setSelectedIds(selectedSet)
                        updateBottomSummary(selectedSet)
                    }
                }
                launch {
                    viewModel.cartEvent.collect { event ->
                        when (event) {
                            is CartUiEvent.ShowToast -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleState(state: UiState<List<CartItemDto>>) {
        binding.swipeRefresh.isRefreshing = false
        when (state) {
            is UiState.Loading -> {
                SkeletonHelper.showSkeleton(binding.shimmerCart, binding.rvCartItems)
                binding.errorView.gone()
                binding.layoutEmpty.gone()
            }
            is UiState.Success -> {
                SkeletonHelper.hideSkeleton(binding.shimmerCart, binding.rvCartItems)
                binding.errorView.gone()
                currentItemsList = state.data

                if (currentItemsList.isEmpty()) {
                    binding.rvCartItems.gone()
                    binding.bottomBarCheckout.gone()
                    binding.layoutEmpty.visible()
                } else {
                    binding.layoutEmpty.gone()
                    binding.rvCartItems.visible()
                    binding.bottomBarCheckout.visible()
                    cartAdapter.submitList(currentItemsList)
                    updateBottomSummary(viewModel.selectedIds.value)
                }
            }
            is UiState.Error -> {
                SkeletonHelper.hideSkeleton(binding.shimmerCart, binding.rvCartItems)
                binding.rvCartItems.gone()
                binding.bottomBarCheckout.gone()
                binding.layoutEmpty.gone()
                binding.errorView.visible()
                binding.errorView.setError(state.message) {
                    viewModel.loadCartItems(preselectId)
                }
            }
        }
    }

    private fun updateBottomSummary(selectedSet: Set<String>) {
        val selectedItems = currentItemsList.filter { selectedSet.contains(it.id) }
        val allSelected = currentItemsList.isNotEmpty() && selectedSet.size == currentItemsList.size
        binding.cbSelectAll.isChecked = allSelected

        var total: Long = 0
        selectedItems.forEach { item ->
            val delta = item.variant?.priceDelta ?: 0L
            val unitPrice = (item.product?.price ?: 0L) + delta
            total += unitPrice * item.quantity
        }

        binding.tvTotalPrice.text = Formatters.formatRupiah(total)
        binding.btnCheckout.text = getString(R.string.checkout_btn, selectedItems.size)
        val hasInvalidStock = selectedItems.any { it.quantity > (it.product?.stock ?: 0) }
        binding.btnCheckout.isEnabled = selectedItems.isNotEmpty() && !hasInvalidStock
    }

    private fun showDeleteConfirmationDialog(item: CartItemDto) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_cart_confirm)
            .setMessage(item.product?.name)
            .setPositiveButton(R.string.delete) { dialog, _ ->
                dialog.dismiss()
                viewModel.deleteItem(item.id)
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
