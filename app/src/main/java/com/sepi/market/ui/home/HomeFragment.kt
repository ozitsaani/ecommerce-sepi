package com.sepi.market.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.sepi.market.core.bounce
import com.sepi.market.core.gone
import com.sepi.market.core.showIf
import com.sepi.market.core.visible
import com.sepi.market.databinding.FragmentHomeBinding
import com.sepi.market.ui.product.ProductAdapter
import com.sepi.market.ui.profile.VoucherAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fragment Beranda aplikasi Sepi (PRD Bagian 9.3 Screen 3 & DESIGN_CLAY.md).
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        (requireActivity().application as SepiApp).appContainer.viewModelFactory
    }

    private lateinit var voucherAdapter: VoucherAdapter
    private lateinit var categoryAdapter: HomeCategoryAdapter
    private lateinit var flashSaleAdapter: FlashSaleAdapter
    private lateinit var recommendationAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyWindowInsets()
        setupAdapters()
        setupListeners()
        observeViewModel()
        startFlashSaleCountdown()
    }

    private fun applyWindowInsets() {
        val window = requireActivity().window
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            val extraTop = resources.getDimensionPixelSize(R.dimen.spacing_md) // 12dp
            binding.headerBlue.setPadding(
                binding.headerBlue.paddingLeft,
                statusBarInsets.top + extraTop,
                binding.headerBlue.paddingRight,
                binding.headerBlue.paddingBottom
            )

            val bottomNavHeight = resources.getDimensionPixelSize(R.dimen.bottom_nav_total_height)
            binding.contentScroll.setPadding(
                binding.contentScroll.paddingLeft,
                binding.contentScroll.paddingTop,
                binding.contentScroll.paddingRight,
                bottomNavHeight + navBarInsets.bottom
            )
            insets
        }
    }

    private fun setupAdapters() {
        val appContainer = (requireActivity().application as SepiApp).appContainer

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

        categoryAdapter = HomeCategoryAdapter { category ->
            findNavController().navigate(
                R.id.productListFragment,
                Bundle().apply {
                    putInt("categoryId", category.id)
                    putString("title", category.name)
                }
            )
        }
        binding.rvCategories.adapter = categoryAdapter

        flashSaleAdapter = FlashSaleAdapter(
            onProductClick = { product ->
                findNavController().navigate(
                    R.id.productDetailFragment,
                    Bundle().apply { putString("productId", product.id) }
                )
            }
        )
        binding.rvFlashSale.adapter = flashSaleAdapter

        recommendationAdapter = ProductAdapter(
            onProductClick = { product ->
                findNavController().navigate(
                    R.id.productDetailFragment,
                    Bundle().apply { putString("productId", product.id) }
                )
            },
            onFavoriteClick = { product ->
                lifecycleScope.launch {
                    appContainer.favoriteRepository.toggleFavorite(product.id)
                }
            }
        )
        binding.rvRecommendations.adapter = recommendationAdapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadHomeData()
        }

        binding.btnCartContainer.setOnClickListener {
            findNavController().navigate(R.id.cartFragment)
        }

        binding.searchBar.setOnClickListener {
            findNavController().navigate(
                R.id.productListFragment,
                Bundle().apply { putBoolean("focusSearch", true) }
            )
        }

        binding.tvSeeAllVouchers.setOnClickListener {
            findNavController().navigate(R.id.vouchersFragment)
        }

        binding.tvSeeAllCategories.setOnClickListener {
            findNavController().navigate(R.id.categoryFragment)
        }

        binding.tvSeeAllRecsHeader.setOnClickListener {
            findNavController().navigate(
                R.id.productListFragment,
                Bundle().apply { putString("title", getString(R.string.section_recommendations)) }
            )
        }

        binding.tvSeeAllRecommendations.setOnClickListener {
            findNavController().navigate(
                R.id.productListFragment,
                Bundle().apply { putString("title", getString(R.string.section_recommendations)) }
            )
        }
    }

    private fun observeViewModel() {
        val appContainer = (requireActivity().application as SepiApp).appContainer

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.homeState.collect { state ->
                        handleState(state)
                    }
                }
                launch {
                    viewModel.cartCount.collect { count ->
                        val hasItems = count > 0
                        binding.tvCartBadge.showIf(hasItems)
                        if (hasItems) {
                            binding.tvCartBadge.text = count.toString()
                            binding.tvCartBadge.bounce()
                        }
                    }
                }
                launch {
                    appContainer.favoriteRepository.favoriteIds.collect { favIds ->
                        recommendationAdapter.setFavoriteIds(favIds)
                    }
                }
            }
        }
    }

    private fun handleState(state: UiState<HomeData>) {
        binding.swipeRefresh.isRefreshing = false
        when (state) {
            is UiState.Loading -> {
                SkeletonHelper.showSkeleton(binding.shimmerHome, binding.contentScroll)
                binding.errorView.gone()
            }
            is UiState.Success -> {
                SkeletonHelper.hideSkeleton(binding.shimmerHome, binding.contentScroll)
                binding.errorView.gone()

                val data = state.data
                binding.tvGreeting.text = getString(R.string.home_greeting, data.userName)

                voucherAdapter.submitList(data.vouchers)
                binding.sectionVouchers.showIf(data.vouchers.isNotEmpty())

                categoryAdapter.submitList(data.categories)
                binding.sectionCategories.showIf(data.categories.isNotEmpty())

                flashSaleAdapter.submitList(data.flashSaleProducts)
                binding.sectionFlashSale.showIf(data.flashSaleProducts.isNotEmpty())

                recommendationAdapter.submitList(data.recommendedProducts)
                binding.sectionRecommendations.showIf(data.recommendedProducts.isNotEmpty())
            }
            is UiState.Error -> {
                SkeletonHelper.hideSkeleton(binding.shimmerHome, binding.contentScroll)
                binding.contentScroll.gone()
                binding.errorView.visible()
                binding.errorView.setError(state.message) {
                    viewModel.loadHomeData()
                }
            }
        }
    }

    private fun startFlashSaleCountdown() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val nextEnd = Formatters.nextFlashSaleEnd().toString()
                    binding.tvFlashCountdown.text = Formatters.countdown(nextEnd)
                    delay(1000)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
