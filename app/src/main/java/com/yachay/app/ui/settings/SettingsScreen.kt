package com.yachay.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yachay.app.AppContainer
import com.yachay.app.BuildConfig
import com.yachay.app.R
import com.yachay.app.appContainer
import com.yachay.app.domain.LineSpacing
import com.yachay.app.domain.ReaderPreferences
import com.yachay.app.domain.ReadingTheme
import com.yachay.app.ui.reader.FontScaleStepper
import com.yachay.app.ui.reader.LineSpacingChips
import com.yachay.app.ui.components.NeoPressable
import com.yachay.app.ui.components.NeoTopBar
import com.yachay.app.ui.components.SectionHeader
import com.yachay.app.ui.components.TopBarSubtitle
import com.yachay.app.ui.reader.colors
import com.yachay.app.ui.theme.Cream
import com.yachay.app.ui.theme.Halftone
import com.yachay.app.ui.theme.Ink
import com.yachay.app.ui.theme.Lime
import com.yachay.app.ui.theme.NeoDefaults
import com.yachay.app.ui.theme.Night
import com.yachay.app.ui.theme.halftone
import com.yachay.app.ui.theme.neoBorder
import com.yachay.app.ui.theme.paperGrain
import com.yachay.app.ui.theme.shadeFor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(container: AppContainer) : ViewModel() {

    private val settings = container.settings

    val readingTheme: StateFlow<ReadingTheme> = settings.readingTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingTheme.Default)

    val readerPreferences: StateFlow<ReaderPreferences> = settings.readerPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderPreferences())

    fun selectReadingTheme(theme: ReadingTheme) = viewModelScope.launch {
        settings.setReadingTheme(theme)
    }

    fun setFontScale(scale: Float) = viewModelScope.launch { settings.setFontScale(scale) }

    fun setLineSpacing(spacing: LineSpacing) = viewModelScope.launch { settings.setLineSpacing(spacing) }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val viewModel = viewModel { SettingsViewModel(context.appContainer) }
    val selected by viewModel.readingTheme.collectAsStateWithLifecycle()
    val reader by viewModel.readerPreferences.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Night,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            NeoTopBar(
                title = stringResource(R.string.settings_title),
                bottom = { TopBarSubtitle(stringResource(R.string.app_version, BuildConfig.VERSION_NAME)) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .paperGrain()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(stringResource(R.string.settings_reading_theme))
            Text(
                stringResource(R.string.settings_reading_theme_body),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = 0.72f),
            )
            Spacer(Modifier.height(4.dp))
            // IntrinsicSize.Min + fillMaxHeight: os dois cards ficam da mesma altura.
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ReadingTheme.entries.forEach { theme ->
                    ThemeOption(
                        theme = theme,
                        selected = theme == selected,
                        onClick = { viewModel.selectReadingTheme(theme) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }

            // Mesma preferência da sheet "Aa" do leitor EPUB.
            SectionHeader(stringResource(R.string.settings_reading_text), modifier = Modifier.padding(top = 20.dp))
            Text(
                stringResource(R.string.settings_reading_text_body),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = 0.72f),
            )
            Text(stringResource(R.string.reader_font_size), style = MaterialTheme.typography.titleMedium)
            FontScaleStepper(reader.fontScale, viewModel::setFontScale)
            Text(
                stringResource(R.string.reader_line_spacing),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            LineSpacingChips(reader.lineSpacing, viewModel::setLineSpacing)
            Text(
                stringResource(R.string.reader_line_spacing_note),
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(alpha = 0.72f),
            )
        }
    }
}

/** Uma opção de tema: prévia de página (no próprio tema) + nome e descrição numa plaquinha creme. */
@Composable
private fun ThemeOption(
    theme: ReadingTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (name, description) = when (theme) {
        ReadingTheme.PAPER -> R.string.theme_paper to R.string.theme_paper_desc
        ReadingTheme.NIGHT -> R.string.theme_night to R.string.theme_night_desc
    }
    NeoPressable(
        onClick = onClick,
        modifier = modifier,
        color = if (selected) Lime else Cream,
        shadow = if (selected) 6.dp else 4.dp,
        role = Role.RadioButton,
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PagePreview(theme)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(name), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (selected) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .background(Ink, NeoDefaults.Shape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Lime, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Text(
                stringResource(description),
                style = MaterialTheme.typography.bodySmall,
                color = Ink.copy(alpha = 0.78f),
            )
        }
    }
}

/** Miniatura de página no tema: "Aa" grande e um trecho de texto, com halftone discreto no canto. */
@Composable
private fun PagePreview(theme: ReadingTheme) {
    val colors = theme.colors()
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .background(colors.page, NeoDefaults.Shape)
            .neoBorder(width = 2.dp),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .halftone(Halftone(shadeFor(colors.page), Alignment.BottomEnd, spacing = 6.dp, maxDot = 2.dp, reach = 0.5f))
                .padding(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Aa", style = MaterialTheme.typography.headlineMedium, color = colors.text)
                Text(
                    stringResource(R.string.settings_sample_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.text,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
