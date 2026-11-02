package com.yachay.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.yachay.app.R

// Space Grotesk é uma fonte variável (eixo wght 300–700) empacotada em res/font.
@OptIn(ExperimentalTextApi::class)
private fun spaceGrotesk(weight: FontWeight) = Font(
    resId = R.font.space_grotesk,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val SpaceGrotesk = FontFamily(
    spaceGrotesk(FontWeight.Normal),
    spaceGrotesk(FontWeight.Medium),
    spaceGrotesk(FontWeight.SemiBold),
    spaceGrotesk(FontWeight.Bold),
)

private val base = Typography()

val YachayTypography = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    headlineLarge = base.headlineLarge.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = base.titleMedium.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold),
    bodyLarge = base.bodyLarge.copy(fontFamily = SpaceGrotesk),
    bodyMedium = base.bodyMedium.copy(fontFamily = SpaceGrotesk),
    bodySmall = base.bodySmall.copy(fontFamily = SpaceGrotesk),
    labelLarge = base.labelLarge.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold),
    labelSmall = base.labelSmall.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold),
)

/** Algarismos tabulares: números que mudam (%, páginas) não "dançam" de largura. */
private const val TABULAR = "tnum"

val PercentTextStyle = TextStyle(
    fontFamily = SpaceGrotesk,
    fontWeight = FontWeight.Bold,
    fontSize = 12.sp,
    fontFeatureSettings = TABULAR,
)

fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = TABULAR)
