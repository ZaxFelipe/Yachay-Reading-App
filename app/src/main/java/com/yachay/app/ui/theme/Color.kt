package com.yachay.app.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta medida das ilustrações em /referencias (roxo elétrico + limão + papel creme + tinta).
// Use estes tokens — nunca hex soltos nas telas. Cada cor tem um papel (ver CLAUDE.md).

/** Roxo elétrico: o campo da marca (barras superiores) e o progresso de leitura. */
val Purple = Color(0xFF7F1DF3)

/** Roxo profundo: volume/sombra do roxo; identidade de coleção. */
val PurpleDeep = Color(0xFF5712AD)

/** Verde-limão: o destaque — ação principal, marca, livro concluído. */
val Lime = Color(0xFF92F728)

/** Lilás: brilho suave; identidade de coleção. */
val Lilac = Color(0xFFEBB3EA)

/** Noite: o fundo das telas — roxo quase preto, entre o roxo profundo e a tinta. */
val Night = Color(0xFF1E0A3C)

/** Papel creme: cards, plaquinhas, diálogos; e o texto sobre o fundo Night e sobre os roxos. */
val Cream = Color(0xFFF5E5D2)

/** Texto secundário sobre Night e sobre roxo: creme recuado. */
val CreamMuted = Cream.copy(alpha = 0.72f)

/** Papel mais claro: trilhos e campos, para destacar do fundo creme. */
val Paper = Color(0xFFFFF7EC)

/** Tinta quase preta arroxeada: texto, contornos, sombras duras, seleção. */
val Ink = Color(0xFF0D0517)

/** Texto secundário sobre os cards creme (≈6:1). Sobre Night/roxo use [CreamMuted]. */
val InkMuted = Color(0xFF5E4F6E)

/** Única cor fora das referências: só para ações destrutivas e erro. */
val Danger = Color(0xFFFF4D5E)

/** Limão escurecido: o halftone sobre limão. */
val LimeDeep = Color(0xFF5DB80F)

/** Creme escurecido: halftone, listras e grão sobre o papel. */
val CreamDeep = Color(0xFFE3CDB2)

/** Tom mais escuro da mesma cor, para texturas (halftone) sobre ela — como a sombra impressa. */
fun shadeFor(color: Color): Color = when (color) {
    Purple -> PurpleDeep
    PurpleDeep -> Ink.copy(alpha = 0.45f)
    Night -> PurpleDeep
    Lime -> LimeDeep
    Lilac -> Purple.copy(alpha = 0.35f)
    Cream, Paper -> CreamDeep
    else -> Ink.copy(alpha = 0.18f)
}

/** Cores de fundo das capas sem imagem e das coleções, escolhidas de forma estável pelo id. */
val CoverPlaceholderColors = listOf(Purple, Lime, Lilac, PurpleDeep)

fun placeholderColorFor(id: Long): Color =
    CoverPlaceholderColors[(id % CoverPlaceholderColors.size).toInt()]

/** Texto legível sobre um fundo da paleta: creme sobre os roxos, a noite e a tinta; tinta sobre o resto. */
fun contentColorFor(background: Color): Color =
    if (background == Purple || background == PurpleDeep || background == Night || background == Ink) Cream else Ink

/**
 * Cor da sombra dura de um elemento sobre o fundo Night: roxa (tinta sumiria no escuro);
 * limão quando o próprio elemento é roxo, para não se fundir com a sombra.
 */
fun shadowFor(fill: Color): Color =
    if (fill == Purple || fill == PurpleDeep) Lime else Purple
