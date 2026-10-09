package com.sepi.market.ui.common

import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sepi.market.R

/**
 * Kelas dasar BottomSheetDialogFragment bertema Claymorphism (PRD Bagian 10.7 & DESIGN_CLAY.md).
 * Menggunakan latar bg_clay_sheet dengan sudut atas 32dp, solid claymorphism
 * demi performa tinggi dan konsistensi lintas versi (API 24+).
 */
abstract class ClayBottomSheet : BottomSheetDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Sepi_BottomSheetDialog)
    }
}
