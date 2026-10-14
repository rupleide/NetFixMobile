package com.rupleide.netfix.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.rupleide.netfix.R

object NetFixFonts {
    val Manrope = FontFamily(
        Font(R.font.manrope_medium, FontWeight.Normal),
        Font(R.font.manrope_medium, FontWeight.Medium),
        Font(R.font.manrope_semibold, FontWeight.SemiBold),
        Font(R.font.manrope_bold, FontWeight.Bold)
    )

    val Unbounded = FontFamily(
        Font(R.font.unbounded_semibold, FontWeight.SemiBold)
    )

    val JetBrainsMono = FontFamily(
        Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
        Font(R.font.jetbrains_mono_medium, FontWeight.Medium)
    )
}
