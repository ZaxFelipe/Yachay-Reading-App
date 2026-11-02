package com.yachay.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

// Texturas e formas da linguagem de quadrinhos das referências (referencias/ref1.png, ref2.png):
// halftone em degradê num canto, listras, grão de papel, estouro e brilho de 4 pontas.
// Todas são desenhadas uma vez por tamanho (drawWithCache) e cortadas nos limites do elemento.

/** Halftone em degradê: pontos maiores em [from], sumindo até [reach] da diagonal. */
@Immutable
data class Halftone(
    val color: Color,
    val from: Alignment = Alignment.TopEnd,
    val spacing: Dp = 8.dp,
    val maxDot: Dp = 3.dp,
    val reach: Float = 0.7f,
)

fun Modifier.halftone(halftone: Halftone?): Modifier =
    if (halftone == null) {
        this
    } else {
        halftone(halftone.color, halftone.from, halftone.spacing, halftone.maxDot, halftone.reach)
    }

/**
 * Grade de pontos em quincôncio (linhas alternadas deslocadas, como na impressão), com o raio
 * caindo linearmente com a distância de [from]. Desenha por trás do conteúdo seguinte da cadeia:
 * coloque depois do `background` e do `clip` do elemento.
 */
fun Modifier.halftone(
    color: Color,
    from: Alignment = Alignment.TopEnd,
    spacing: Dp = 8.dp,
    maxDot: Dp = 3.dp,
    reach: Float = 0.7f,
): Modifier = drawWithCache {
    val step = spacing.toPx()
    val maxRadius = maxDot.toPx()
    val anchor = from.align(IntSize.Zero, IntSize(size.width.toInt(), size.height.toInt()), layoutDirection)
    val maxDistance = hypot(size.width, size.height) * reach
    val dots = Path()
    var row = 0
    var y = 0f
    while (y <= size.height + step) {
        var x = if (row % 2 == 0) 0f else step / 2
        while (x <= size.width + step) {
            val t = 1f - hypot(x - anchor.x, y - anchor.y) / maxDistance
            val radius = maxRadius * t
            if (radius > 0.4f) dots.addOval(Rect(Offset(x, y), radius))
            x += step
        }
        y += step * 0.866f
        row++
    }
    onDrawBehind { clipRect { drawPath(dots, color) } }
}

/** Listras diagonais (45°) — preenchem um espaço vazio sem parecer "faltando". */
fun Modifier.diagonalStripes(color: Color, gap: Dp = 7.dp, width: Dp = 2.dp): Modifier = drawWithCache {
    val step = gap.toPx()
    val stroke = width.toPx()
    onDrawBehind {
        clipRect {
            var x = -size.height
            while (x < size.width) {
                drawLine(color, Offset(x, size.height), Offset(x + size.height, 0f), stroke)
                x += step
            }
        }
    }
}

/**
 * Grão de papel: pontinhos aleatórios (semente fixa, então não "cintilam" entre recomposições).
 * [perDp2] é a densidade de grãos por dp².
 */
fun Modifier.paperGrain(color: Color = Cream.copy(alpha = 0.06f), perDp2: Float = 1f / 160f): Modifier = drawWithCache {
    val random = Random(7)
    val areaDp2 = size.width * size.height / (density * density)
    val specks = Path()
    repeat((areaDp2 * perDp2).toInt()) {
        val radius = (0.35f + random.nextFloat() * 0.6f) * density
        specks.addOval(Rect(Offset(random.nextFloat() * size.width, random.nextFloat() * size.height), radius))
    }
    onDrawBehind { drawPath(specks, color) }
}

/**
 * Sublinhado grosso sob o texto (o "marca-texto" do fundo escuro: texto creme sobre limão não se lê).
 * Use num Text sem `weight`/`fillMaxWidth`, para a barra acompanhar as palavras.
 */
fun Modifier.marker(color: Color = Lime): Modifier = drawBehind {
    val thickness = 5.dp.toPx()
    drawRect(
        color,
        topLeft = Offset(0f, size.height - thickness * 0.4f),
        size = Size(size.width + 6.dp.toPx(), thickness),
    )
}

/** Estouro de quadrinho (zigue-zague em volta do círculo), para selos. */
class StarburstShape(private val points: Int = 14, private val innerRatio: Float = 0.78f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cx = size.width / 2
        val cy = size.height / 2
        val path = Path()
        val vertices = points * 2
        for (i in 0 until vertices) {
            val angle = 2 * PI * i / vertices - PI / 2
            val ratio = if (i % 2 == 0) 1f else innerRatio
            val x = cx + (cx * ratio * cos(angle)).toFloat()
            val y = cy + (cy * ratio * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/** Brilho de 4 pontas com lados côncavos (o "✦" das referências). */
object SparkleShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        // Pontos de controle perto do centro deixam os lados bem côncavos.
        val pinch = 0.06f
        val path = Path().apply {
            moveTo(cx, 0f)
            quadraticBezierTo(cx + w * pinch, cy - h * pinch, w, cy)
            quadraticBezierTo(cx + w * pinch, cy + h * pinch, cx, h)
            quadraticBezierTo(cx - w * pinch, cy + h * pinch, 0f, cy)
            quadraticBezierTo(cx - w * pinch, cy - h * pinch, cx, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}
