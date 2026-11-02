package com.yachay.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yachay.app.ui.theme.Ink
import com.yachay.app.ui.theme.Lime
import com.yachay.app.ui.theme.SparkleShape
import com.yachay.app.ui.theme.StarburstShape
import com.yachay.app.ui.theme.neoShadow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Brilho de 4 pontas. Com [outline], ganha contorno em tinta (para aparecer sobre cores claras). */
@Composable
fun Sparkle(
    modifier: Modifier = Modifier,
    extent: Dp = 20.dp,
    color: Color = Ink,
    outline: Color? = null,
) {
    Box(
        modifier
            .size(extent)
            .background(color, SparkleShape)
            .then(if (outline != null) Modifier.border(1.5.dp, outline, SparkleShape) else Modifier),
    )
}

/**
 * Marcas de impacto ("!!" das referências): três cunhas que irradiam do canto inferior direito
 * para cima/esquerda. [mirrored] espelha para irradiar para cima/direita.
 */
@Composable
fun ImpactMarks(
    modifier: Modifier = Modifier,
    extent: Dp = 36.dp,
    color: Color = Ink,
    mirrored: Boolean = false,
) {
    Canvas(
        modifier
            .size(extent)
            .graphicsLayer { scaleX = if (mirrored) -1f else 1f },
    ) {
        val w = size.width
        val origin = Offset(w, size.height)
        // (ângulo em graus na tela: 180 = esquerda, 270 = cima; comprimento relativo)
        val wedges = listOf(200f to 0.78f, 235f to 1f, 270f to 0.8f)
        wedges.forEach { (degrees, length) ->
            val angle = degrees * PI.toFloat() / 180f
            val dir = Offset(cos(angle), sin(angle))
            val perp = Offset(-dir.y, dir.x)
            val tip = origin + dir * (w * 0.32f)
            val outer = origin + dir * (w * 0.95f * length)
            val half = w * 0.1f
            val wedge = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(outer.x + perp.x * half, outer.y + perp.y * half)
                lineTo(outer.x - perp.x * half, outer.y - perp.y * half)
                close()
            }
            drawPath(wedge, color)
        }
    }
}

/** Selo estourado de quadrinho (ex.: "Lido!"), com contorno e sombra dura. */
@Composable
fun BurstBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Lime,
    extent: Dp = 58.dp,
) {
    val shape = StarburstShape()
    Box(
        modifier
            .size(extent)
            .neoShadow(3.dp, shape)
            .background(color, shape)
            .border(2.dp, Ink, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = Ink)
    }
}
