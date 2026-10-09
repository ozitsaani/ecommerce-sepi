package com.sepi.market.ui.product

import android.graphics.Paint
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
import com.google.android.material.chip.Chip
import com.sepi.market.R
import com.sepi.market.SepiApp
import com.sepi.market.core.Formatters
import com.sepi.market.ui.common.SkeletonHelper
import com.sepi.market.core.UiState
import com.sepi.market.core.bounce
import com.sepi.market.core.gone
import com.sepi.market.core.loadImage
import com.sepi.market.core.showIf
import com.sepi.market.core.visible
import com.sepi.market.data.remote.dto.ProductDetailDto
import com.sepi.market.databinding.FragmentProductDetailBinding
import com.sepi.market.ui.review.ReviewAdapter
import kotlinx.coroutines.launch

/**
 * Fragment Layar Detail Produk dengan pilihan varian, ulasan, dan akselerasi Beli Sekarang (PRD Bagian 9.3 Screen 6).
 */
class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductDetailViewModel by viewModels {
        (requireActivity().application as SepiApp).appContainer.viewModelFactory
    }

    private lateinit var reviewAdapter: ReviewAdapter
    private var productId: String = ""
    private var currentProduct: ProductDetailDto? = null
    private var isDescriptionExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        productId = arguments?.getString("productId").orEmpty()

        setupAdapters()
        setupListeners()
        observeViewModel()

        if (productId.isNotBlank()) {
            viewModel.loadProductDetail(productId)
        }
    }

    private fun setupAdapters() {
        reviewAdapter = ReviewAdapter()
        binding.rvReviewsSummary.adapter = reviewAdapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCartContainer.setOnClickListener {
            findNavController().navigate(R.id.cartFragment)
        }

        binding.tvSeeMore.setOnClickListener {
            isDescriptionExpanded = !isDescriptionExpanded
            if (isDescriptionExpanded) {
                binding.tvDescription.maxLines = Int.MAX_VALUE
                binding.tvSeeMore.text = getString(R.string.see_less)
            } else {
                binding.tvDescription.maxLines = 4
                binding.tvSeeMore.text = getString(R.string.see_more)
            }
        }

        binding.btnSeeAllReviews.setOnClickListener {
            val product = currentProduct ?: return@setOnClickListener
            findNavController().navigate(
                R.id.reviewsFragment,
                Bundle().apply {
                    putString("productId", product.id)
                    putString("productName", product.name)
                }
            )
        }

        binding.btnFavorite.setOnClickListener {
            val appContainer = (requireActivity().application as SepiApp).appContainer
            if (productId.isNotBlank()) {
                lifecycleScope.launch {
                    appContainer.favoriteRepository.toggleFavorite(productId)
                }
            }
        }

        binding.btnAddToCart.setOnClickListener {
            val product = currentProduct ?: return@setOnClickListener
            if (product.stock <= 0) {
                Toast.makeText(requireContext(), getString(R.string.stock_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            openVariantSheet(product, buyNow = false)
        }

        binding.btnBuyNow.setOnClickListener {
            val product = currentProduct ?: return@setOnClickListener
            if (product.stock <= 0) {
                Toast.makeText(requireContext(), getString(R.string.stock_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            openVariantSheet(product, buyNow = true)
        }
    }

    private fun openVariantSheet(product: ProductDetailDto, buyNow: Boolean) {
        val sheet = VariantSheet(
            product = product,
            buyNow = buyNow,
            onConfirm = { variant, qty, isBuyNow ->
                viewModel.selectedVariant.value = variant
                viewModel.quantity.value = qty
                viewModel.addToCart(product.id, isBuyNow)
            }
        )
        sheet.show(childFragmentManager, "VariantSheet")
    }

    private fun observeViewModel() {
        val appContainer = (requireActivity().application as SepiApp).appContainer

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.detailState.collect { state ->
                        handleDetailState(state)
                    }
                }
                launch {
                    viewModel.reviewsState.collect { state ->
                        if (state is UiState.Success) {
                            reviewAdapter.submitList(state.data.take(3))
                        }
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
                        val isFav = favIds.contains(productId)
                        binding.btnFavorite.setImageResource(
                            if (isFav) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
                        )
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

    private fun handleDetailState(state: UiState<ProductDetailDto>) {
        when (state) {
            is UiState.Loading -> {
                SkeletonHelper.showSkeleton(binding.shimmerDetail, binding.contentScroll)
                binding.errorView.gone()
            }
            is UiState.Success -> {
                SkeletonHelper.hideSkeleton(binding.shimmerDetail, binding.contentScroll)
                binding.errorView.gone()

                val product = state.data
                currentProduct = product
                populateProductDetail(product)
            }
            is UiState.Error -> {
                SkeletonHelper.hideSkeleton(binding.shimmerDetail, binding.contentScroll)
                binding.contentScroll.gone()
                binding.errorView.visible()
                binding.errorView.setError(state.message) {
                    viewModel.loadProductDetail(productId)
                }
            }
        }
    }

    private fun populateProductDetail(product: ProductDetailDto) {
        binding.ivProductImage.loadImage(product.imageUrl)
        binding.tvProductName.text = product.name
        binding.tvPrice.text = Formatters.formatRupiah(product.price)

        val hasDiscount = product.originalPrice != null && product.originalPrice > product.price
        binding.tvOriginalPrice.showIf(hasDiscount)
        binding.tvDiscountBadge.showIf(hasDiscount)
        if (hasDiscount && product.originalPrice != null) {
            binding.tvOriginalPrice.text = Formatters.formatRupiah(product.originalPrice)
            binding.tvOriginalPrice.paintFlags = binding.tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.tvDiscountBadge.text = "${Formatters.discountPercent(product.price, product.originalPrice)}%"
        }

        binding.tvRating.text = "${String.format("%.1f", product.ratingAvg)} (${product.ratingCount})"
        binding.tvSoldCount.text = getString(R.string.sold_count, product.soldCount)

        if (product.stock <= 5 && product.stock > 0) {
            binding.tvStock.text = getString(R.string.stock_low, product.stock)
            binding.tvStock.setTextColor(requireContext().getColor(R.color.sepi_danger))
        } else if (product.stock <= 0) {
            binding.tvStock.text = getString(R.string.stock_empty)
            binding.tvStock.setTextColor(requireContext().getColor(R.color.sepi_danger))
            binding.btnAddToCart.isEnabled = false
            binding.btnBuyNow.isEnabled = false
        } else {
            binding.tvStock.text = getString(R.string.stock_count, product.stock)
            binding.tvStock.setTextColor(requireContext().getColor(R.color.sepi_ink_soft))
        }

        binding.tvShopInfo.text = "${product.shopName} • ${product.shopCity}"
        binding.tvDescription.text = product.description

        // Display Variants
        val variants = product.variants
        val hasVariants = !variants.isNullOrEmpty()
        binding.layoutVariants.showIf(hasVariants)

        if (hasVariants && variants != null) {
            binding.tvVariantTitle.text = variants.firstOrNull()?.groupName ?: getString(R.string.select_variant)
            binding.chipGroupDetailVariants.removeAllViews()

            variants.forEachIndexed { index, variant ->
                val chip = Chip(requireContext()).apply {
                    text = if (variant.priceDelta > 0) {
                        "${variant.optionName} (+${Formatters.formatRupiah(variant.priceDelta)})"
                    } else {
                        variant.optionName
                    }
                    isCheckable = true
                    id = index
                    isChecked = index == 0

                    // Gaya custom: hilangkan gaya Material default
                    setChipBackgroundColorResource(android.R.color.transparent)
                    chipStrokeWidth = 0f
                    isCheckedIconVisible = false
                    chipStartPadding = (16 * resources.displayMetrics.density)
                    chipEndPadding = (16 * resources.displayMetrics.density)
                    textSize = 14f
                    typeface = resources.getFont(R.font.nunito_semibold)

                    // Terapkan background clay chip custom
                    if (index == 0) {
                        setBackgroundResource(R.drawable.bg_clay_blue_chip)
                        setTextColor(requireContext().getColor(R.color.white))
                    } else {
                        setBackgroundResource(R.drawable.bg_clay_chip)
                        setTextColor(requireContext().getColor(R.color.sepi_ink))
                    }

                    minimumHeight = (40 * resources.displayMetrics.density).toInt()
                }
                binding.chipGroupDetailVariants.addView(chip)
            }

            binding.chipGroupDetailVariants.setOnCheckedStateChangeListener { group, checkedIds ->
                val selectedIdx = checkedIds.firstOrNull() ?: -1
                val variant = if (selectedIdx in variants.indices) variants[selectedIdx] else null
                viewModel.selectedVariant.value = variant

                // Update background setiap chip berdasarkan state terpilih
                for (i in 0 until group.childCount) {
                    val c = group.getChildAt(i) as? Chip ?: continue
                    if (c.isChecked) {
                        c.setBackgroundResource(R.drawable.bg_clay_blue_chip)
                        c.setTextColor(requireContext().getColor(R.color.white))
                    } else {
                        c.setBackgroundResource(R.drawable.bg_clay_chip)
                        c.setTextColor(requireContext().getColor(R.color.sepi_ink))
                    }
                }
            }
        }
    }

    private fun handleEvent(event: ProductDetailUiEvent) {
        when (event) {
            is ProductDetailUiEvent.AddedToCartSuccess -> {
                Toast.makeText(requireContext(), getString(R.string.added_to_cart), Toast.LENGTH_SHORT).show()
                binding.tvCartBadge.bounce()
                if (event.buyNow) {
                    findNavController().navigate(
                        R.id.cartFragment,
                        Bundle().apply { putString("preselectId", event.cartItem.id) }
                    )
                }
            }
            is ProductDetailUiEvent.ShowToast -> {
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
