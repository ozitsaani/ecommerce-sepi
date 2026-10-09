package com.sepi.market.ui.order

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.sepi.market.R
import com.sepi.market.SepiApp
import com.sepi.market.core.Formatters
import com.sepi.market.core.UiState
import com.sepi.market.core.gone
import com.sepi.market.core.showIf
import com.sepi.market.core.visible
import com.sepi.market.data.remote.dto.OrderDto
import com.sepi.market.databinding.FragmentOrderDetailBinding
import com.sepi.market.ui.common.SkeletonHelper
import kotlinx.coroutines.launch

/**
 * Fragment Layar Detail & Tracking Pesanan dengan Simulasi Mode Demo (PRD Bagian 9.3 Screen 10).
 */
class OrderDetailFragment : Fragment() {

    private var _binding: FragmentOrderDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderDetailViewModel by viewModels {
        (requireActivity().application as SepiApp).appContainer.viewModelFactory
    }

    private lateinit var trackingAdapter: TrackingAdapter
    private lateinit var orderItemAdapter: OrderItemAdapter
    private var orderId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderId = arguments?.getString("orderId").orEmpty()

        setupAdapters()
        setupListeners()
        observeViewModel()

        if (orderId.isNotBlank()) {
            viewModel.loadOrderDetail(orderId)
        }
    }

    private fun setupAdapters() {
        trackingAdapter = TrackingAdapter()
        binding.rvTrackingTimeline.adapter = trackingAdapter

        orderItemAdapter = OrderItemAdapter { item ->
            // Buka layar ulasan untuk produk ini (PRD Bagian 9.3 Screen 10)
            val bundle = Bundle().apply {
                putString("productId", item.productId.orEmpty())
                putString("productName", item.productName)
                putBoolean("openWrite", true)
            }
            findNavController().navigate(R.id.reviewsFragment, bundle)
        }
        binding.rvOrderItems.adapter = orderItemAdapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.swipeRefresh.setOnRefreshListener {
            if (orderId.isNotBlank()) {
                viewModel.loadOrderDetail(orderId, forceRefresh = true)
            }
        }

        binding.btnAdvanceOrderDemo.setOnClickListener {
            if (orderId.isNotBlank()) {
                viewModel.advanceOrder(orderId)
            }
        }

        binding.btnPayNow.setOnClickListener {
            if (orderId.isNotBlank()) {
                val bundle = Bundle().apply {
                    putString("orderId", orderId)
                }
                findNavController().navigate(R.id.paymentFragment, bundle)
            }
        }

        binding.btnCopyOrderCode.setOnClickListener {
            val code = binding.tvOrderCode.text.toString()
            copyToClipboard("Kode Pesanan", code)
        }

        binding.btnCopyTrackingNumber.setOnClickListener {
            val resi = binding.tvTrackingNumber.text.toString()
            copyToClipboard("Nomor Resi", resi)
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), getString(R.string.copied), Toast.LENGTH_SHORT).show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.orderDetailState.collect { state ->
                        handleState(state)
                    }
                }
                launch {
                    viewModel.isAdvancing.collect { advancing ->
                        binding.btnAdvanceOrderDemo.isEnabled = !advancing
                    }
                }
                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            is OrderDetailUiEvent.ShowToast -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleState(state: UiState<OrderDto>) {
        binding.swipeRefresh.isRefreshing = false
        when (state) {
            is UiState.Loading -> {
                SkeletonHelper.showSkeleton(binding.shimmerOrderDetail, binding.contentScroll)
                binding.errorView.gone()
            }
            is UiState.Success -> {
                SkeletonHelper.hideSkeleton(binding.shimmerOrderDetail, binding.contentScroll)
                binding.errorView.gone()

                val order = state.data
                populateOrderDetails(order)
            }
            is UiState.Error -> {
                SkeletonHelper.hideSkeleton(binding.shimmerOrderDetail, binding.contentScroll)
                binding.contentScroll.gone()
                binding.errorView.visible()
                binding.errorView.setError(state.message) {
                    viewModel.loadOrderDetail(orderId)
                }
            }
        }
    }

    private fun populateOrderDetails(order: OrderDto) {
        val status = order.status.uppercase()
        val isDelivered = status == "DELIVERED"

        // 1. Highlight Stepper Steps
        val context = requireContext()
        val activeColor = ContextCompat.getColor(context, R.color.sepi_orange_700)
        val inactiveColor = ContextCompat.getColor(context, R.color.sepi_ink_soft)

        binding.stepPaid.setTextColor(if (status in listOf("PAID", "PACKED", "SHIPPED", "DELIVERED")) activeColor else inactiveColor)
        binding.stepPacked.setTextColor(if (status in listOf("PACKED", "SHIPPED", "DELIVERED")) activeColor else inactiveColor)
        binding.stepShipped.setTextColor(if (status in listOf("SHIPPED", "DELIVERED")) activeColor else inactiveColor)
        binding.stepDelivered.setTextColor(if (isDelivered) activeColor else inactiveColor)

        // Tombol UNPAID vs MODE DEMO
        binding.layoutUnpaidAction.showIf(status == "UNPAID")
        val canAdvance = status in listOf("PAID", "PACKED", "SHIPPED")
        binding.layoutDemoMode.showIf(canAdvance)

        // 2. Info Pesanan & Pengiriman
        binding.tvOrderCode.text = order.orderCode
        binding.tvOrderDate.text = Formatters.relativeTime(order.createdAt)
        binding.tvCourierName.text = "${order.courierName} (${order.shippingMethod})"

        val hasResi = !order.trackingNumber.isNullOrBlank()
        binding.tvTrackingNumber.text = if (hasResi) order.trackingNumber else getString(R.string.receipt_not_available)
        binding.btnCopyTrackingNumber.showIf(hasResi)

        // 3. Timeline Pelacakan (urut terbaru di atas)
        val trackingList = order.tracking.orEmpty().sortedByDescending { it.createdAt }
        trackingAdapter.submitList(trackingList)

        // 4. Alamat Tujuan
        binding.tvRecipient.text = "${order.recipientName} (${order.recipientPhone})"
        binding.tvShippingAddress.text = order.shippingAddress

        // 5. Daftar Barang Pesanan (dengan tombol tulis ulasan jika status DELIVERED)
        val uiItems = order.items.orEmpty().map {
            OrderItemUiModel(item = it, isDelivered = isDelivered)
        }
        orderItemAdapter.submitList(uiItems)

        // 6. Rincian Pembayaran
        binding.tvSubtotal.text = Formatters.formatRupiah(order.subtotal)
        binding.tvShippingCost.text = Formatters.formatRupiah(order.shippingCost)
        binding.tvDiscount.text = "-${Formatters.formatRupiah(order.discount)}"
        binding.tvTotalAmount.text = Formatters.formatRupiah(order.total)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
