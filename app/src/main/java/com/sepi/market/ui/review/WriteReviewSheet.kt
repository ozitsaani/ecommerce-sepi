package com.sepi.market.ui.review

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.sepi.market.databinding.DialogWriteReviewSheetBinding
import com.sepi.market.ui.common.GlassBottomSheet

/**
 * Bottom Sheet Kaca untuk menulis ulasan produk (PRD Bagian 9.3 Screen 12).
 */
class WriteReviewSheet(
    private val onSubmit: (rating: Int, comment: String) -> Unit
) : GlassBottomSheet() {

    private var _binding: DialogWriteReviewSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogWriteReviewSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSubmitReview.setOnClickListener {
            val rating = binding.ratingBar.rating.toInt()
            val comment = binding.etReviewComment.text?.toString()?.trim().orEmpty()

            if (rating < 1) {
                Toast.makeText(requireContext(), "Beri rating minimal 1 bintang", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            onSubmit(rating, comment)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
