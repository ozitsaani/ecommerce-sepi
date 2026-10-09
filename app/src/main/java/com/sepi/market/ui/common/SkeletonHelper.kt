package com.sepi.market.ui.common

import android.view.View
import com.facebook.shimmer.ShimmerFrameLayout

/**
 * Helper untuk mengontrol animasi Skeleton Loading menggunakan Facebook Shimmer (PRD Bagian 9.1 & SEPI-16).
 */
object SkeletonHelper {

    fun showSkeleton(shimmerLayout: ShimmerFrameLayout, contentView: View? = null) {
        shimmerLayout.visibility = View.VISIBLE
        shimmerLayout.startShimmer()
        contentView?.visibility = View.GONE
    }

    fun hideSkeleton(shimmerLayout: ShimmerFrameLayout, contentView: View? = null) {
        shimmerLayout.stopShimmer()
        shimmerLayout.visibility = View.GONE
        contentView?.visibility = View.VISIBLE
    }
}
