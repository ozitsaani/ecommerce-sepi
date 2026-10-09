package com.sepi.market.ui.order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import com.sepi.market.R
import com.sepi.market.SepiApp
import com.sepi.market.core.UiState
import com.sepi.market.core.gone
import com.sepi.market.core.visible
import com.sepi.market.data.remote.dto.OrderDto
import com.sepi.market.databinding.FragmentOrdersBinding
import com.sepi.market.ui.common.SkeletonHelper
import kotlinx.coroutines.launch

/**
 * Fragment Layar Daftar Pesanan Pengguna dengan filter tab status (PRD Bagian 9.3 Screen 10).
 */
class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrdersViewModel by viewModels {
        (requireActivity().application as SepiApp).appContainer.viewModelFactory
    }

    private lateinit var orderAdapter: OrderAdapter
    private var currentTabPosition = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentTabPosition = savedInstanceState?.getInt("savedTabPosition")
            ?: (arguments?.getInt("initialTab", 0) ?: 0)

        setupAdapter()
        setupTabs()
        setupListeners()
        observeViewModel()

        binding.tabOrders.getTabAt(currentTabPosition)?.select()
        loadOrdersForCurrentTab(forceRefresh = false)
    }

    private fun setupAdapter() {
        orderAdapter = OrderAdapter { order ->
            if (order.status.uppercase() == "UNPAID") {
                findNavController().navigate(
                    R.id.paymentFragment,
                    Bundle().apply { putString("orderId", order.id) }
                )
            } else {
                findNavController().navigate(
                    R.id.orderDetailFragment,
                    Bundle().apply { putString("orderId", order.id) }
                )
            }
        }
        binding.rvOrders.adapter = orderAdapter
    }

    private fun setupTabs() {
        binding.tabOrders.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTabPosition = tab?.position ?: 0
                loadOrdersForCurrentTab()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            loadOrdersForCurrentTab(forceRefresh = true)
        }
    }

    private fun loadOrdersForCurrentTab(forceRefresh: Boolean = false) {
        val statusFilter = when (currentTabPosition) {
            1 -> "eq.UNPAID"
            2 -> "in.(PAID,PACKED)"
            3 -> "eq.SHIPPED"
            4 -> "eq.DELIVERED"
            5 -> "eq.CANCELLED"
            else -> null
        }
        viewModel.loadOrders(statusFilter, forceRefresh = forceRefresh)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("savedTabPosition", currentTabPosition)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ordersState.collect { state ->
                    handleState(state)
                }
            }
        }
    }

    private fun handleState(state: UiState<List<OrderDto>>) {
        binding.swipeRefresh.isRefreshing = false
        when (state) {
            is UiState.Loading -> {
                SkeletonHelper.showSkeleton(binding.shimmerOrders, binding.rvOrders)
                binding.errorView.gone()
                binding.layoutEmpty.gone()
            }
            is UiState.Success -> {
                SkeletonHelper.hideSkeleton(binding.shimmerOrders, binding.rvOrders)
                binding.errorView.gone()

                val list = state.data
                if (list.isEmpty()) {
                    binding.rvOrders.gone()
                    binding.layoutEmpty.visible()
                } else {
                    binding.layoutEmpty.gone()
                    binding.rvOrders.visible()
                    orderAdapter.submitList(list)
                }
            }
            is UiState.Error -> {
                SkeletonHelper.hideSkeleton(binding.shimmerOrders, binding.rvOrders)
                binding.rvOrders.gone()
                binding.layoutEmpty.gone()
                binding.errorView.visible()
                binding.errorView.setError(state.message) {
                    loadOrdersForCurrentTab()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
