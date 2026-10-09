package com.sepi.market.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.sepi.market.data.remote.dto.VoucherDto
import com.sepi.market.databinding.FragmentVouchersBinding
import kotlinx.coroutines.launch

/**
 * Fragment Layar Koleksi Voucher Pengguna (PRD Bagian 9.3 Screen 11).
 */
class VouchersFragment : Fragment() {

    private var _binding: FragmentVouchersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VouchersViewModel by viewModels {
        (requireActivity().application as SepiApp).appContainer.viewModelFactory
    }

    private lateinit var voucherAdapter: VoucherAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVouchersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupListeners()
        observeViewModel()

        viewModel.loadVouchers()
    }

    private fun setupAdapter() {
        voucherAdapter = VoucherAdapter { voucher ->
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Voucher", voucher.code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(
                requireContext(),
                getString(R.string.voucher_code_copied, voucher.code),
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.rvVouchers.adapter = voucherAdapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadVouchers(forceRefresh = true)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.vouchersState.collect { state ->
                    handleState(state)
                }
            }
        }
    }

    private fun handleState(state: UiState<List<VoucherDto>>) {
        binding.swipeRefresh.isRefreshing = false
        when (state) {
            is UiState.Loading -> {
                SkeletonHelper.showSkeleton(binding.shimmerVouchers, binding.rvVouchers)
                binding.errorView.gone()
                binding.layoutEmpty.gone()
            }
            is UiState.Success -> {
                SkeletonHelper.hideSkeleton(binding.shimmerVouchers, binding.rvVouchers)
                binding.errorView.gone()

                val list = state.data
                if (list.isEmpty()) {
                    binding.rvVouchers.gone()
                    binding.layoutEmpty.visible()
                } else {
                    binding.layoutEmpty.gone()
                    binding.rvVouchers.visible()
                    voucherAdapter.submitList(list)
                }
            }
            is UiState.Error -> {
                SkeletonHelper.hideSkeleton(binding.shimmerVouchers, binding.rvVouchers)
                binding.rvVouchers.gone()
                binding.layoutEmpty.gone()
                binding.errorView.visible()
                binding.errorView.setError(state.message) {
                    viewModel.loadVouchers()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
