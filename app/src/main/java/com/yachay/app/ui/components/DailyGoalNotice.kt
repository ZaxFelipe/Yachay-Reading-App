package com.yachay.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.yachay.app.R
import com.yachay.app.domain.DailyGoalProgress
import com.yachay.app.ui.theme.Cream
import com.yachay.app.ui.theme.Ink
import com.yachay.app.ui.theme.Lime
import com.yachay.app.ui.theme.NeoDefaults
import com.yachay.app.ui.theme.neoBorder
import com.yachay.app.ui.theme.tabular

/**
 * Aviso da meta do dia: "Quase lá!" (creme, discreto) ao passar de 60% e "Meta batida!" (limão,
 * o destaque) ao completar — com o nível novo quando a meta desbloqueou um avatar
 * ([unlockedLevel]) ou a sequência quando ela chegou a um múltiplo de 7 ([streakBonus]);
 * o avatar tem prioridade. O texto segue [progress]. Fica num [Popup] logo abaixo de onde é
 * chamado (a barra do leitor), para não empurrar o layout da página.
 */
@Composable
fun DailyGoalNotice(visible: Boolean, progress: DailyGoalProgress, unlockedLevel: Int? = null, streakBonus: Int? = null) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    // O popup só existe enquanto aparece ou some (a animação de saída precisa dele).
    if (!state.currentState && !state.targetState) return

    // Abaixo da barra superior: o espaço até a borda inferior dela (padding + contorno) + folga.
    val gap = with(LocalDensity.current) { 24.dp.roundToPx() }
    val position = remember(gap) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                return IntOffset(x.coerceIn(0, maxOf(0, windowSize.width - popupContentSize.width)), anchorBounds.bottom + gap)
            }
        }
    }
    Popup(popupPositionProvider = position, properties = PopupProperties(focusable = false)) {
        AnimatedVisibility(
            visibleState = state,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
        ) {
            val done = progress.isDone
            Row(
                Modifier
                    .background(if (done) Lime else Cream, NeoDefaults.Shape)
                    .neoBorder(width = 2.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Sparkle(extent = 14.dp, color = if (done) Ink else Lime, outline = if (done) null else Ink)
                Text(
                    if (done && unlockedLevel != null) {
                        stringResource(R.string.daily_goal_level_up, unlockedLevel)
                    } else if (done && streakBonus != null) {
                        stringResource(R.string.daily_goal_streak, streakBonus)
                    } else if (done) {
                        stringResource(R.string.daily_goal_done, progress.pagesRead)
                    } else {
                        stringResource(R.string.daily_goal_almost_there, progress.pagesRead, progress.target)
                    },
                    style = (if (done) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium).tabular(),
                    color = Ink,
                )
            }
        }
    }
}
