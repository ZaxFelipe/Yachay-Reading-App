package com.yachay.app.ui.reader

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.yachay.app.domain.ReadingTheme
import com.yachay.app.ui.theme.Cream
import com.yachay.app.ui.theme.Ink
import com.yachay.app.ui.theme.Night

/** Cores da página e do texto em cada tema de leitura (usadas pelos dois leitores e pela prévia em Ajustes). */
data class ReadingColors(val page: Color, val text: Color)

fun ReadingTheme.colors(): ReadingColors = when (this) {
    ReadingTheme.PAPER -> ReadingColors(page = Cream, text = Ink)
    ReadingTheme.NIGHT -> ReadingColors(page = Night, text = Cream)
}

/**
 * Filtro para as páginas do PDF (que o PdfRenderer entrega em branco e preto):
 * - Papel: multiplica pelo creme — o branco vira creme e as cores das imagens se mantêm.
 * - Noturno: duotom por luminância — branco vira a página Night e preto vira o texto creme
 *   (imagens coloridas ficam em duas cores; é o preço de não ofuscar no escuro).
 */
fun ReadingTheme.pdfPageFilter(): ColorFilter {
    val colors = colors()
    return when (this) {
        ReadingTheme.PAPER -> ColorFilter.tint(colors.page, BlendMode.Multiply)
        ReadingTheme.NIGHT -> ColorFilter.colorMatrix(luminanceDuotone(light = colors.page, dark = colors.text))
    }
}

/** Mapeia a luminância: branco → [light], preto → [dark]. Deslocamentos em 0..255, como no ColorMatrix do Android. */
private fun luminanceDuotone(light: Color, dark: Color): ColorMatrix {
    fun row(lightC: Float, darkC: Float): FloatArray {
        val span = lightC - darkC
        return floatArrayOf(span * 0.2126f, span * 0.7152f, span * 0.0722f, 0f, darkC * 255f)
    }
    return ColorMatrix(
        row(light.red, dark.red) +
            row(light.green, dark.green) +
            row(light.blue, dark.blue) +
            floatArrayOf(0f, 0f, 0f, 1f, 0f),
    )
}
