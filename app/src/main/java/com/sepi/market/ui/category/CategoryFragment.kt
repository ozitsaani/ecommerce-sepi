package com.sepi.market.ui.category

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
import com.sepi.market.core.UiState
import com.sepi.market.core.gone
import com.sepi.market.core.visible
import com.sepi.market.data.remote.dto.CategoryDto
import com.sepi.market.databinding.FragmentCategoryBinding
import com.sepi.market.ui.common.SkeletonHelper
import kotlinx.coroutines.launch

/**
 * Fragment Layar Kategori (PRD Bagian 9.3 Screen 4).
 */
class CategoryFragment : Fragment() {

    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryViewModel by viewModels {
        (requireActivity().application as SepiApp).appContainer.viewModelFactory
    }

    private lateinit var adapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupListeners()
        observeViewModel()
    }

    private fun setupAdapter() {
        adapter = CategoryAdapter { category ->
            val args = Bundle().apply {
                putInt("categoryId", category.id)
                putString("title", category.name)
            }
            findNavController().navigate(R.id.productListFragment, args)
        }
        binding.rvCategories.adapter = adapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadCategories()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categoryState.collect { state ->
                    handleState(state)
                }
            }
        }
    }

    private fun handleState(state: UiState<List<CategoryDto>>) {
        binding.swipeRefresh.isRefreshing = false
        when (state) {
            is UiState.Loading -> {
                SkeletonHelper.showSkeleton(binding.shimmerCategory, binding.rvCategories)
                binding.errorView.gone()
            }
            is UiState.Success -> {
                SkeletonHelper.hideSkeleton(binding.shimmerCategory, binding.rvCategories)
                binding.errorView.gone()
                adapter.submitList(state.data)
            }
            is UiState.Error -> {
                SkeletonHelper.hideSkeleton(binding.shimmerCategory, binding.rvCategories)
                binding.rvCategories.gone()
                binding.errorView.visible()
                binding.errorView.setError(state.message) {
                    viewModel.loadCategories()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
