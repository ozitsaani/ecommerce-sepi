package com.sepi.market.ui.favorite

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
import com.sepi.market.R
import com.sepi.market.SepiApp
import com.sepi.market.ui.common.SkeletonHelper
import com.sepi.market.core.UiState
import com.sepi.market.core.bounce
import com.sepi.market.core.gone
import com.sepi.market.core.showIf
import com.sepi.market.core.visible
import com.sepi.market.data.remote.dto.ProductDto
import com.sepi.market.databinding.FragmentFavoritesBinding
import com.sepi.market.ui.product.ProductAdapter
import kotlinx.coroutines.launch

/**
 * Fragment Layar Produk Favorit Pengguna (PRD Bagian 9.3).
 */
class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoritesViewModel by viewModels {
        (requireActivity().application as SepiApp).appContainer.viewModelFactory
    }

    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupListeners()
        observeViewModel()

        viewModel.loadFavorites()
    }

    private fun setupAdapter() {
        productAdapter = ProductAdapter(
            onProductClick = { product ->
                findNavController().navigate(
                    R.id.productDetailFragment,
                    Bundle().apply { putString("productId", product.id) }
                )
            },
            onFavoriteClick = { product ->
                viewModel.removeFavorite(product.id)
            }
        )
        binding.rvFavorites.adapter = productAdapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadFavorites(forceRefresh = true)
        }

        binding.btnCartContainer.setOnClickListener {
            findNavController().navigate(R.id.cartFragment)
        }
    }

    private fun observeViewModel() {
        val appContainer = (requireActivity().application as SepiApp).appContainer

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.favoritesState.collect { state ->
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
                SkeletonHelper.showSkeleton(binding.shimmerFavorites, binding.rvFavorites)
                binding.errorView.gone()
                binding.layoutEmpty.gone()
            }
            is UiState.Success -> {
                SkeletonHelper.hideSkeleton(binding.shimmerFavorites, binding.rvFavorites)
                binding.errorView.gone()

                val list = state.data
                if (list.isEmpty()) {
                    binding.rvFavorites.gone()
                    binding.layoutEmpty.visible()
                } else {
                    binding.layoutEmpty.gone()
                    binding.rvFavorites.visible()
                    productAdapter.submitList(list)
                }
            }
            is UiState.Error -> {
                SkeletonHelper.hideSkeleton(binding.shimmerFavorites, binding.rvFavorites)
                binding.rvFavorites.gone()
                binding.layoutEmpty.gone()
                binding.errorView.visible()
                binding.errorView.setError(state.message) {
                    viewModel.loadFavorites()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
