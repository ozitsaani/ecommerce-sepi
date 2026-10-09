package com.sepi.market.ui.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.sepi.market.data.remote.dto.AddressDto
import com.sepi.market.data.remote.dto.CartItemDto
import com.sepi.market.data.remote.dto.VoucherDto
import com.sepi.market.databinding.FragmentCheckoutBinding
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * Fragment Layar Checkout dengan alamat Cirebon, pengiriman Reguler/Express, voucher, dan pembuatan pesanan (PRD Bagian 9.3 Screen 8 & design 05_checkout.png).
 */
class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CheckoutViewModel by viewModels {
        (requireActivity().application as SepiApp).appContainer.viewModelFactory
    }

    private lateinit var checkoutAdapter: CheckoutAdapter
    private var cartItemIds: List<String> = emptyList()
    private var currentCheckoutData: CheckoutData? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val array = arguments?.getStringArray("cartItemIds")
        cartItemIds = array?.toList() ?: emptyList()

        setupWindowInsets()
        setupAdapter()
        setupListeners()
        observeViewModel()

        viewModel.loadCheckoutData(cartItemIds)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.headerContainer) { v, insets ->
            val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(v.paddingLeft, statusBarInset + resources.getDimensionPixelSize(R.dimen.spacing_xs), v.paddingRight, v.paddingBottom)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomBarOrder) { v, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val defaultMargin = resources.getDimensionPixelSize(R.dimen.spacing_sm)
            val params = v.layoutParams as? ViewGroup.MarginLayoutParams
            if (params != null) {
                params.bottomMargin = defaultMargin + navBarInset
                v.layoutParams = params
            }
            insets
        }
    }

    private fun setupAdapter() {
        checkoutAdapter = CheckoutAdapter()
        binding.rvCheckoutItems.adapter = checkoutAdapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadCheckoutData(cartItemIds)
        }

        binding.btnChangeAddress.setOnClickListener {
            val data = currentCheckoutData ?: return@setOnClickListener
            val sheet = AddressPickerSheet(
                addresses = data.addresses,
                onAddressSelected = { address ->
                    viewModel.selectAddress(address)
                },
                onAddNewAddressClick = {
                    findNavController().navigate(R.id.addressesFragment)
                }
            )
            sheet.show(childFragmentManager, "AddressPickerSheet")
        }

        binding.rgShippingMethod.setOnCheckedChangeListener { _, checkedId ->
            val method = if (checkedId == R.id.rbShippingExpress) "EXPRESS" else "REGULER"
            viewModel.shippingMethod.value = method
            updateSummary()
        }

        val openVoucherPicker = {
            val data = currentCheckoutData
            if (data != null) {
                val subtotal = calculateSubtotal(data.selectedCartItems)
                val sheet = VoucherPickerSheet(
                    subtotal = subtotal,
                    vouchers = data.vouchers,
                    onVoucherSelected = { voucher ->
                        viewModel.selectVoucher(voucher)
                        updateSummary()
                    }
                )
                sheet.show(childFragmentManager, "VoucherPickerSheet")
            }
        }

        binding.btnSelectVoucher.setOnClickListener { openVoucherPicker() }
        binding.boxVoucherContent.setOnClickListener { openVoucherPicker() }
        binding.cardVoucherContainer.setOnClickListener { openVoucherPicker() }

        binding.btnPlaceOrder.setOnClickListener {
            viewModel.placeOrder(cartItemIds)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.checkoutState.collect { state ->
                        handleState(state)
                    }
                }
                launch {
                    viewModel.selectedAddress.collect { address ->
                        populateAddress(address)
                    }
                }
                launch {
                    viewModel.selectedVoucher.collect { voucher ->
                        populateVoucher(voucher)
                    }
                }
                launch {
                    viewModel.isSubmitting.collect { submitting ->
                        binding.btnPlaceOrder.isEnabled = !submitting
                    }
                }
                launch {
                    viewModel.checkoutEvent.collect { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    private fun handleState(state: UiState<CheckoutData>) {
        binding.swipeRefresh.isRefreshing = false
        when (state) {
            is UiState.Loading -> {
                SkeletonHelper.showSkeleton(binding.shimmerCheckout, binding.contentScroll)
                binding.errorView.gone()
            }
            is UiState.Success -> {
                SkeletonHelper.hideSkeleton(binding.shimmerCheckout, binding.contentScroll)
                binding.errorView.gone()

                val data = state.data
                currentCheckoutData = data

                binding.tvOrderItemsTitle.text = "Produk Dipesan (${data.selectedCartItems.size})"
                binding.tvSubtotalLabel.text = "Subtotal Produk (${data.selectedCartItems.size} item)"

                checkoutAdapter.submitList(data.selectedCartItems)
                checkoutAdapter.setSelectedIds(data.selectedCartItems.map { it.id }.toSet())

                updateSummary()
            }
            is UiState.Error -> {
                SkeletonHelper.hideSkeleton(binding.shimmerCheckout, binding.contentScroll)
                binding.contentScroll.gone()
                binding.errorView.visible()
                binding.errorView.setError(state.message) {
                    viewModel.loadCheckoutData(cartItemIds)
                }
            }
        }
    }

    private fun populateAddress(address: AddressDto?) {
        if (address != null) {
            val phone = if (!address.phone.isNullOrBlank()) " • ${address.phone}" else ""
            binding.tvRecipient.text = "${address.recipientName}$phone"
            binding.tvFullAddress.text = "${address.street}, ${address.district}, ${address.city} ${address.postalCode}".trimEnd()
            binding.tvBadgeUtama.visible()

            val isCirebon = address.city.contains("Cirebon", ignoreCase = true)
            binding.tvNonCirebonWarning.showIf(!isCirebon)
            binding.btnPlaceOrder.isEnabled = isCirebon
        } else {
            binding.tvRecipient.text = getString(R.string.no_address_warning)
            binding.tvFullAddress.text = ""
            binding.tvBadgeUtama.gone()
            binding.tvNonCirebonWarning.gone()
            binding.btnPlaceOrder.isEnabled = false
        }
    }

    private fun populateVoucher(voucher: VoucherDto?) {
        if (voucher != null) {
            binding.tvVoucherCode.visible()
            binding.tvVoucherCode.text = voucher.code
            binding.btnSelectVoucher.text = getString(R.string.voucher_used)
        } else {
            binding.tvVoucherCode.gone()
            binding.tvVoucherPreview.text = getString(R.string.voucher_section_title)
            binding.btnSelectVoucher.text = getString(R.string.btn_select_voucher)
        }
        updateSummary()
    }

    private fun calculateSubtotal(items: List<CartItemDto>): Long {
        var subtotal: Long = 0
        items.forEach { item ->
            val delta = item.variant?.priceDelta ?: 0L
            val unitPrice = (item.product?.price ?: 0L) + delta
            subtotal += unitPrice * item.quantity
        }
        return subtotal
    }

    private fun updateSummary() {
        val data = currentCheckoutData ?: return
        val subtotal = calculateSubtotal(data.selectedCartItems)

        val shippingCost: Long = if (viewModel.shippingMethod.value == "EXPRESS") 25000L else 10000L

        val voucher = viewModel.selectedVoucher.value
        var discount: Long = 0L
        if (voucher != null && subtotal >= voucher.minSpend) {
            discount = when (voucher.discountType.uppercase()) {
                "PERCENT" -> {
                    val rawDisc = (subtotal * voucher.discountValue) / 100
                    val maxD = voucher.maxDiscount ?: Long.MAX_VALUE
                    min(rawDisc, maxD)
                }
                "FIXED" -> {
                    min(voucher.discountValue, subtotal)
                }
                "FREE_SHIPPING" -> {
                    val maxD = voucher.maxDiscount ?: shippingCost
                    min(shippingCost, maxD)
                }
                else -> 0L
            }
        }

        val total = max(0L, subtotal + shippingCost - discount)

        binding.tvSubtotal.text = Formatters.formatRupiah(subtotal)
        binding.tvShippingCost.text = Formatters.formatRupiah(shippingCost)
        binding.tvDiscount.text = "-${Formatters.formatRupiah(discount)}"

        if (voucher != null) {
            binding.tvVoucherPreview.text = "-${Formatters.formatRupiah(discount)}"
        }

        binding.tvTotalPayment.text = Formatters.formatRupiah(total)
        binding.tvSummaryTotal.text = Formatters.formatRupiah(total)
    }

    private fun handleEvent(event: CheckoutUiEvent) {
        when (event) {
            is CheckoutUiEvent.OrderCreatedSuccess -> {
                val navOptions = androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.cartFragment, true)
                    .setEnterAnim(R.anim.nav_fade_enter)
                    .setExitAnim(R.anim.nav_fade_exit)
                    .build()
                findNavController().navigate(
                    R.id.paymentFragment,
                    Bundle().apply { putString("orderId", event.order.id) },
                    navOptions
                )
            }
            is CheckoutUiEvent.ShowToast -> {
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
