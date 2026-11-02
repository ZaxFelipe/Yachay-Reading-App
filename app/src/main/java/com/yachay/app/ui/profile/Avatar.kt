package com.yachay.app.ui.profile

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yachay.app.R
import com.yachay.app.domain.ReaderLevel
import com.yachay.app.ui.components.NeoPressable
import com.yachay.app.ui.theme.Ink
import com.yachay.app.ui.theme.Lime
import com.yachay.app.ui.theme.NeoDefaults
import com.yachay.app.ui.theme.Night
import com.yachay.app.ui.theme.neoBorder
import com.yachay.app.ui.theme.neoShadow

/** Avatares recortados de referencias/ref2.png, um por nível (índice 0 = nível 1). */
private val AvatarDrawables = listOf(
    R.drawable.avatar_01, R.drawable.avatar_02, R.drawable.avatar_03, R.drawable.avatar_04,
    R.drawable.avatar_05, R.drawable.avatar_06, R.drawable.avatar_07, R.drawable.avatar_08,
    R.drawable.avatar_09, R.drawable.avatar_10, R.drawable.avatar_11, R.drawable.avatar_12,
    R.drawable.avatar_13, R.drawable.avatar_14, R.drawable.avatar_15, R.drawable.avatar_16,
).also { check(it.size == ReaderLevel.MAX_LEVEL) }

@DrawableRes
fun avatarFor(level: Int): Int = AvatarDrawables[level.coerceIn(1, ReaderLevel.MAX_LEVEL) - 1]

/** Bloqueado: sem cor e esmaecido, para a silhueta adiantar o que vem sem estragar a surpresa. */
private val LockedFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * Avatar quadrado com contorno e sombra dura, como as capas. Com [onClick] vira um
 * [NeoPressable] (afunda ao tocar); [selected] marca a escolha numa galeria.
 */
@Composable
fun Avatar(
    level: Int,
    modifier: Modifier = Modifier,
    /** null = ocupa a largura disponível (quadrado). */
    size: Dp? = 36.dp,
    shadow: Dp = 3.dp,
    shadowColor: Color = Ink,
    locked: Boolean = false,
    contentDescription: String = stringResource(R.string.profile_avatar_description, level),
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
) {
    val image = @Composable {
        Image(
            painterResource(avatarFor(level)),
            contentDescription = contentDescription,
            modifier = if (size != null) Modifier.size(size) else Modifier.fillMaxWidth().aspectRatio(1f),
            contentScale = ContentScale.Crop,
            colorFilter = if (locked) LockedFilter else null,
            alpha = if (locked) 0.35f else 1f,
        )
    }
    if (onClick != null) {
        NeoPressable(
            onClick = onClick,
            modifier = modifier.semantics { this.selected = selected },
            color = Night,
            shadow = shadow,
            shadowColor = shadowColor,
            role = Role.RadioButton,
        ) { image() }
    } else {
        Box(
            modifier
                .padding(end = shadow, bottom = shadow)
                .neoShadow(shadow, color = shadowColor)
                .background(Night, NeoDefaults.Shape)
                .neoBorder()
                .clip(NeoDefaults.Shape),
        ) { image() }
    }
}

/** Botão do perfil no canto da Biblioteca: o avatar escolhido, que "afunda" ao tocar. */
@Composable
fun ProfileButton(avatar: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Sobre a barra roxa, sombra limão (tinta sumiria no escuro).
    Avatar(
        avatar,
        modifier = modifier,
        size = 44.dp,
        shadowColor = Lime,
        contentDescription = stringResource(R.string.profile_open),
        onClick = onClick,
    )
}
