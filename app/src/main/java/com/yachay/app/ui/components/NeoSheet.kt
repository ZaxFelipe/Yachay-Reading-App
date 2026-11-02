package com.yachay.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yachay.app.ui.theme.Cream
import com.yachay.app.ui.theme.Ink
import com.yachay.app.ui.theme.NeoDefaults

/**
 * Sheet creme com alça de tinta, como a do livro e a "Aa". [dimBackground] = false deixa o
 * conteúdo de trás visível (leitores: ver a mudança acontecer na página).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeoSheet(
    onDismiss: () -> Unit,
    dimBackground: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Cream,
        contentColor = Ink,
        scrimColor = if (dimBackground) BottomSheetDefaults.ScrimColor else Color.Transparent,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 48.dp, height = 6.dp)
                    .background(Ink, NeoDefaults.Shape),
            )
        },
        content = content,
    )
}
