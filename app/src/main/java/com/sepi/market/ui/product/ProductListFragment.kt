package com.sepi.market.ui.product

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sepi.market.R
import com.sepi.market.SepiApp
import com.sepi.market.ui.common.SkeletonHelper
import com.sepi.market.core.UiState
import com.sepi.market.core.bounce
import com.sepi.market.core.gone
import com.sepi.market.core.showIf
import com.sepi.market.core.visible
import com.sepi.market.data.remote.dto.ProductDto
import com.sepi.market.databinding.FragmentProductListBinding
import kotlinx.coroutines.launch

/**
 * Fragment Layar Daftar Produk dengan pencarian instan, filter, urutan, dan infinite scroll (PRD Bagian 9.3 Screen 5).
 */
class ProductListFragment : Fragment() {

    private var _binding: FragmentProductListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductListViewModel by viewModels {
        (requireActivity().application as SepiApp).appContainer.viewModelFactory
    }

    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        readArguments()
        setupAdapter()
        setupListeners()
        observeViewModel()
    }

    private fun readArguments() {
        val args = arguments ?: return
        val catId = args.getInt("categoryId", -1)
        val query = args.getString("query", "")
        val flashOnly = args.getBoolean("flashOnly", false)
        val focusSearch = args.getBoolean("focusSearch", false)

        viewModel.setInitialFilters(
            categoryId = catId,
            query = query,
            flashOnly = flashOnly
        )

        if (query.isNotBlank()) {
            binding.etSearch.setText(query)
            binding.btnClearSearch.visible()
        }

        if (focusSearch) {
            binding.etSearch.requestFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun setupAdapter() {
        val appContainer = (requireActivity().application as SepiApp).appContainer

        productAdapter = ProductAdapter(
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
        binding.rvProducts.adapter = productAdapter

        // Infinite scroll pagination listener
        binding.rvProducts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (totalItemCount > 0 && lastVisibleItem >= totalItemCount - 4) {
                    viewModel.loadNextPage()
                }
            }
        })
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCartContainer.setOnClickListener {
            findNavController().navigate(R.id.cartFragment)
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.setText("")
            binding.btnClearSearch.gone()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString().orEmpty()
                binding.btnClearSearch.showIf(text.isNotEmpty())
                viewModel.searchQuery.value = text
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.resetAndLoad()
        }

        binding.btnFilter.setOnClickListener {
            val sheet = FilterSheet(
                initialMinPrice = viewModel.minPriceFilter,
                initialMaxPrice = viewModel.maxPriceFilter,
                initialMinRating4 = viewModel.minRating4Filter,
                onApply = { minP, maxP, minRating4 ->
                    viewModel.minPriceFilter = minP
                    viewModel.maxPriceFilter = maxP
                    viewModel.minRating4Filter = minRating4
                    updateFilterButtonState()
                    viewModel.resetAndLoad()
                },
                onReset = {
                    viewModel.resetFilters()
                    updateFilterButtonState()
                }
            )
            sheet.show(childFragmentManager, "FilterSheet")
        }

        binding.btnResetFilterEmpty.setOnClickListener {
            viewModel.resetFilters()
            binding.etSearch.setText("")
            updateFilterButtonState()
        }

        binding.chipGroupSort.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chipNewest -> viewModel.currentSortOrder = "created_at.desc"
                R.id.chipBestseller -> viewModel.currentSortOrder = "sold_count.desc"
                R.id.chipPriceAsc -> viewModel.currentSortOrder = "price.asc"
                R.id.chipPriceDesc -> viewModel.currentSortOrder = "price.desc"
            }
            viewModel.resetAndLoad()
        }
    }

    private fun updateFilterButtonState() {
        if (viewModel.hasActiveFilters()) {
            binding.btnFilter.text = getString(R.string.filter_title) + " (1)"
        } else {
            binding.btnFilter.text = getString(R.string.filter_title)
        }
    }

    private fun observeViewModel() {
        val appContainer = (requireActivity().application as SepiApp).appContainer

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.productsState.collect { state ->
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
                        productAdapter.setFavoriteIds(favIds)
                    }
                }
            }
        }
    }

    private fun handleState(state: UiState<List<ProductDto>>) {
        binding.swipeRefresh.isRefreshing = false
        when (state) {
            is UiState.Loading -> {
                SkeletonHelper.showSkeleton(binding.shimmerProductList, binding.rvProducts)
                binding.errorView.gone()
                binding.layoutEmpty.gone()
            }
            is UiState.Success -> {
                SkeletonHelper.hideSkeleton(binding.shimmerProductList, binding.rvProducts)
                binding.errorView.gone()
                val list = state.data
                if (list.isEmpty()) {
                    binding.rvProducts.gone()
                    binding.layoutEmpty.visible()
                } else {
                    binding.layoutEmpty.gone()
                    binding.rvProducts.visible()
                    productAdapter.submitList(list)
                }
            }
            is UiState.Error -> {
                SkeletonHelper.hideSkeleton(binding.shimmerProductList, binding.rvProducts)
                binding.rvProducts.gone()
                binding.layoutEmpty.gone()
                binding.errorView.visible()
                binding.errorView.setError(state.message) {
                    viewModel.resetAndLoad()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
