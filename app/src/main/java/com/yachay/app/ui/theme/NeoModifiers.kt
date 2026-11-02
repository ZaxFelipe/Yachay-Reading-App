package com.yachay.app.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object NeoDefaults {
    val BorderWidth = 2.5.dp
    val ShadowOffset = 4.dp
    val CornerRadius = 4.dp
    val Shape: Shape = RoundedCornerShape(CornerRadius)
}

/**
 * Sombra dura neobrutalista: a mesma forma, preenchida com [color], deslocada para baixo/direita
 * e sem blur. Desenhada fora dos limites do componente — deixe espaço (padding) no pai.
 */
fun Modifier.neoShadow(
    offset: Dp = NeoDefaults.ShadowOffset,
    shape: Shape = NeoDefaults.Shape,
    color: Color = Ink,
): Modifier = if (offset <= 0.dp) this else drawBehind {
    val px = offset.toPx()
    translate(left = px, top = px) {
        drawOutline(shape.createOutline(size, layoutDirection, this), color)
    }
}

/** Contorno sólido grosso em [Ink]. */
fun Modifier.neoBorder(
    width: Dp = NeoDefaults.BorderWidth,
    shape: Shape = NeoDefaults.Shape,
    color: Color = Ink,
): Modifier = border(width, color, shape)
