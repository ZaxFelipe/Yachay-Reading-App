package com.yachay.app.ui.reader.epub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yachay.app.R
import com.yachay.app.domain.LineSpacing
import com.yachay.app.domain.ReaderPreferences
import com.yachay.app.domain.ReadingTheme
import com.yachay.app.domain.PageMargins
import com.yachay.app.domain.ReaderFont
import com.yachay.app.domain.TextAlignment
import com.yachay.app.ui.reader.EpubTextControls
import com.yachay.app.ui.reader.ReadingThemeChips
import com.yachay.app.ui.theme.Cream
import com.yachay.app.ui.theme.Ink
import com.yachay.app.ui.theme.InkMuted
import com.yachay.app.ui.theme.NeoDefaults

/**
 * Sheet "Aa" do leitor EPUB (creme, como a sheet do livro). Sem scrim escuro: a página
 * continua visível acima, para ver a mudança de fonte acontecer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    preferences: ReaderPreferences,
    onFontScale: (Float) -> Unit,
    onLineSpacing: (LineSpacing) -> Unit,
    onFont: (ReaderFont) -> Unit,
    onMargins: (PageMargins) -> Unit,
    onTextAlign: (TextAlignment) -> Unit,
    onTheme: (ReadingTheme) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Cream,
        contentColor = Ink,
        scrimColor = Color.Transparent,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 48.dp, height = 6.dp)
                    .background(Ink, NeoDefaults.Shape),
            )
        },
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EpubTextControls(
                preferences = preferences,
                onFontScale = onFontScale,
                onLineSpacing = onLineSpacing,
                onFont = onFont,
                onMargins = onMargins,
                onTextAlign = onTextAlign,
                noteColor = InkMuted,
            )

            Text(
                stringResource(R.string.settings_reading_theme),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            ReadingThemeChips(preferences.theme, onTheme)
        }
    }
}
