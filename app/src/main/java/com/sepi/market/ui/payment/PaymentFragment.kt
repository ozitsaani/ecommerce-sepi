package com.sepi.market.ui.payment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.sepi.market.core.UiState
import com.sepi.market.core.gone
import com.sepi.market.core.visible
import com.sepi.market.data.remote.dto.OrderDto
import com.sepi.market.databinding.FragmentPaymentBinding
import kotlinx.coroutines.launch

/**
 * Fragment Layar Pembayaran Virtual Account BCA Sandbox (PRD Bagian 9.3 Screen 9).
 */
class PaymentFragment : Fragment() {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PaymentViewModel by viewModels {
        (requireActivity().application as SepiApp).appContainer.viewModelFactory
    }

    private var orderId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderId = arguments?.getString("orderId").orEmpty()

        setupListeners()
        observeViewModel()

        if (orderId.isNotBlank()) {
            viewModel.loadOrderDetail(orderId)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCopyVa.setOnClickListener {
            val va = binding.tvVaNumber.text.toString().replace(" ", "")
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("VA Number", va)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), getString(R.string.copied), Toast.LENGTH_SHORT).show()
        }

        binding.btnCheckPayment.setOnClickListener {
            if (orderId.isNotBlank()) {
                viewModel.checkPaymentStatus(orderId)
            }
        }

        binding.btnCancelOrder.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.btn_cancel_order)
                .setMessage(R.string.cancel_order_confirm)
                .setPositiveButton(R.string.btn_cancel_order) { dialog, _ ->
                    dialog.dismiss()
                    if (orderId.isNotBlank()) {
                        viewModel.cancelOrder(orderId)
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.orderState.collect { state ->
                        handleState(state)
                    }
                }
                launch {
                    viewModel.countdownText.collect { cd ->
                        if (!viewModel.isExpired.value) {
                             binding.tvCountdown.text = cd
                        }
                    }
                }
                launch {
                    viewModel.isExpired.collect { expired ->
                        if (expired) {
                            binding.tvCountdown.text = getString(R.string.payment_expired)
                            binding.btnCheckPayment.isEnabled = false
                        }
                    }
                }
                launch {
                    viewModel.isActionLoading.collect { loading ->
                        binding.btnCheckPayment.isEnabled = !loading && !viewModel.isExpired.value
                        binding.btnCancelOrder.isEnabled = !loading
                    }
                }
                launch {
                    viewModel.event.collect { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    private fun handleState(state: UiState<OrderDto>) {
        when (state) {
            is UiState.Loading -> {
                binding.cardPayment.gone()
                binding.errorView.gone()
            }
            is UiState.Success -> {
                binding.errorView.gone()
                binding.cardPayment.visible()

                val order = state.data
                binding.tvVaNumber.text = Formatters.formatVa(order.vaNumber)
                binding.tvOrderCode.text = order.orderCode
                binding.tvTotalAmount.text = Formatters.formatRupiah(order.total)
            }
            is UiState.Error -> {
                binding.cardPayment.gone()
                binding.errorView.visible()
                binding.errorView.setError(state.message) {
                    viewModel.loadOrderDetail(orderId)
                }
            }
        }
    }

    private fun handleEvent(event: PaymentUiEvent) {
        when (event) {
            is PaymentUiEvent.PaymentSuccess -> {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.payment_success_dialog)
                    .setMessage(R.string.payment_success_desc)
                    .setIcon(R.drawable.ic_check_circle)
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        val navOptions = androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.paymentFragment, true)
                            .setEnterAnim(R.anim.nav_fade_enter)
                            .setExitAnim(R.anim.nav_fade_exit)
                            .build()
                        findNavController().navigate(
                            R.id.orderDetailFragment,
                            Bundle().apply { putString("orderId", event.order.id) },
                            navOptions
                        )
                    }
                    .show()
            }
            is PaymentUiEvent.OrderCancelled -> {
                Toast.makeText(requireContext(), "Pesanan dibatalkan", Toast.LENGTH_SHORT).show()
                val navOptions = androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.homeFragment, false)
                    .setEnterAnim(R.anim.nav_fade_enter)
                    .setExitAnim(R.anim.nav_fade_exit)
                    .build()
                findNavController().navigate(R.id.homeFragment, null, navOptions)
            }
            is PaymentUiEvent.ShowToast -> {
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
            }
            is PaymentUiEvent.RedirectToDetail -> {
                val navOptions = androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.paymentFragment, true)
                    .setEnterAnim(R.anim.nav_fade_enter)
                    .setExitAnim(R.anim.nav_fade_exit)
                    .build()
                findNavController().navigate(
                    R.id.orderDetailFragment,
                    Bundle().apply { putString("orderId", event.orderId) },
                    navOptions
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
