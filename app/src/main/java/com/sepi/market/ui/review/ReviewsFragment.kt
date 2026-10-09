package com.sepi.market.ui.review

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
import com.google.android.material.snackbar.Snackbar
import com.sepi.market.R
import com.sepi.market.SepiApp
import com.sepi.market.core.UiState
import com.sepi.market.core.gone
import com.sepi.market.core.visible
import com.sepi.market.data.remote.dto.ReviewDto
import com.sepi.market.databinding.FragmentReviewsBinding
import com.sepi.market.ui.common.SkeletonHelper
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Fragment Layar Daftar Ulasan Produk & Tulis Ulasan (PRD Bagian 9.3 Screen 12).
 */
class ReviewsFragment : Fragment() {

    private var _binding: FragmentReviewsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReviewsViewModel by viewModels {
        (requireActivity().application as SepiApp).appContainer.viewModelFactory
    }

    private lateinit var reviewAdapter: ReviewAdapter
    private var productId: String = ""
    private var productName: String = ""
    private var openWrite: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReviewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        productId = arguments?.getString("productId").orEmpty()
        productName = arguments?.getString("productName").orEmpty()
        openWrite = arguments?.getBoolean("openWrite", false) ?: false

        if (productName.isNotBlank()) {
            binding.tvHeaderTitle.text = "Ulasan: $productName"
        }

        setupAdapter()
        setupListeners()
        observeViewModel()

        if (productId.isNotBlank()) {
            viewModel.loadReviews(productId)
        }

        if (openWrite) {
            openWriteReviewSheet()
        }
    }

    private fun setupAdapter() {
        reviewAdapter = ReviewAdapter()
        binding.rvReviews.adapter = reviewAdapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.swipeRefresh.setOnRefreshListener {
            if (productId.isNotBlank()) {
                viewModel.loadReviews(productId, forceRefresh = true)
            }
        }

        binding.btnWriteReviewTop.setOnClickListener {
            openWriteReviewSheet()
        }
    }

    private fun openWriteReviewSheet() {
        val sheet = WriteReviewSheet { rating, comment ->
            if (productId.isNotBlank()) {
                viewModel.submitReview(productId, rating, comment)
            }
        }
        sheet.show(childFragmentManager, "WriteReviewSheet")
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.reviewsState.collect { state ->
                        handleState(state)
                    }
                }
                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            is ReviewUiEvent.ReviewSubmittedSuccess -> {
                                Snackbar.make(binding.root, R.string.review_thank_you, Snackbar.LENGTH_SHORT).show()
                            }
                            is ReviewUiEvent.ShowToast -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleState(state: UiState<List<ReviewDto>>) {
        binding.swipeRefresh.isRefreshing = false
        when (state) {
            is UiState.Loading -> {
                SkeletonHelper.showSkeleton(binding.shimmerReviews, binding.contentScroll)
                binding.errorView.gone()
                binding.layoutEmpty.gone()
            }
            is UiState.Success -> {
                SkeletonHelper.hideSkeleton(binding.shimmerReviews, binding.contentScroll)
                binding.errorView.gone()

                val list = state.data
                if (list.isEmpty()) {
                    binding.contentScroll.gone()
                    binding.layoutEmpty.visible()
                } else {
                    binding.layoutEmpty.gone()
                    binding.contentScroll.visible()

                    populateReviewSummary(list)
                    reviewAdapter.submitList(list)
                }
            }
            is UiState.Error -> {
                SkeletonHelper.hideSkeleton(binding.shimmerReviews, binding.contentScroll)
                binding.contentScroll.gone()
                binding.layoutEmpty.gone()
                binding.errorView.visible()
                binding.errorView.setError(state.message) {
                    if (productId.isNotBlank()) {
                        viewModel.loadReviews(productId)
                    }
                }
            }
        }
    }

    /**
     * Menghitung dan menampilkan ringkasan rating besar, rata-rata bintang, jumlah total ulasan,
     * serta batang distribusi 5 sampai 1 di sisi klien dari seluruh ulasan produk (PRD Bagian 9.3 Screen 12).
     */
    private fun populateReviewSummary(reviews: List<ReviewDto>) {
        val total = reviews.size
        if (total == 0) return

        val avg = reviews.map { it.rating }.average()
        binding.tvRatingAverage.text = String.format(Locale.US, "%.1f", avg)
        binding.rbSummaryAverage.rating = avg.toFloat()
        binding.tvTotalReviews.text = "($total ulasan)"

        val count5 = reviews.count { it.rating == 5 }
        val count4 = reviews.count { it.rating == 4 }
        val count3 = reviews.count { it.rating == 3 }
        val count2 = reviews.count { it.rating == 2 }
        val count1 = reviews.count { it.rating == 1 }

        binding.tvCountRating5.text = count5.toString()
        binding.pbRating5.progress = (count5 * 100) / total

        binding.tvCountRating4.text = count4.toString()
        binding.pbRating4.progress = (count4 * 100) / total

        binding.tvCountRating3.text = count3.toString()
        binding.pbRating3.progress = (count3 * 100) / total

        binding.tvCountRating2.text = count2.toString()
        binding.pbRating2.progress = (count2 * 100) / total

        binding.tvCountRating1.text = count1.toString()
        binding.pbRating1.progress = (count1 * 100) / total
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
