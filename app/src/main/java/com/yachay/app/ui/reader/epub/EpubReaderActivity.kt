package com.yachay.app.ui.reader.epub

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.yachay.app.R
import com.yachay.app.YachayNavigationBar
import com.yachay.app.YachayStatusBar
import com.yachay.app.appContainer
import com.yachay.app.domain.ReadingProgress
import com.yachay.app.domain.ReadingTheme
import com.yachay.app.ui.reader.colors
import com.yachay.app.ui.components.NeoIconButton
import com.yachay.app.ui.components.NeoProgressBar
import com.yachay.app.ui.components.NeoTopBar
import com.yachay.app.ui.theme.YachayTheme
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Color as ReadiumColor
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.navigator.util.DirectionalNavigationAdapter
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

/**
 * Leitor EPUB. Fica numa activity própria (não numa rota Compose) porque o navegador do
 * Readium é um Fragment que precisa de FragmentFactory instalada antes de `super.onCreate`,
 * inclusive quando o sistema recria a activity.
 */
@OptIn(ExperimentalReadiumApi::class, FlowPreview::class)
class EpubReaderActivity : AppCompatActivity() {

    private val container by lazy { appContainer }
    private var bookId: Long = -1
    private val progress = MutableStateFlow(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        bookId = intent.getLongExtra(EXTRA_BOOK_ID, -1)
        val session = container.epubSessions[bookId]
        if (session == null) {
            // Processo morreu com o leitor aberto: a publicação não está mais em memória.
            super.onCreate(null)
            finish()
            return
        }

        supportFragmentManager.fragmentFactory = EpubNavigatorFactory(session.publication)
            .createFragmentFactory(
                initialLocator = session.initialLocator,
                initialPreferences = session.initialTheme.epubPreferences(),
            )

        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = YachayStatusBar, navigationBarStyle = YachayNavigationBar)
        setContentView(R.layout.activity_epub_reader)

        val root = findViewById<View>(R.id.reader_root)
        root.setBackgroundColor(session.initialTheme.colors().page.toArgb())
        val readerContainer = findViewById<FragmentContainerView>(R.id.reader_container)
        ViewCompat.setOnApplyWindowInsetsListener(readerContainer) { view, insets ->
            view.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                add(R.id.reader_container, EpubNavigatorFragment::class.java, Bundle(), NAVIGATOR_TAG)
            }
        }
        val navigator = supportFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as EpubNavigatorFragment

        // Toques nas bordas viram página; arrastar já funciona por padrão.
        navigator.addInputListener(DirectionalNavigationAdapter(navigator))

        findViewById<ComposeView>(R.id.reader_top_bar).setContent {
            YachayTheme {
                val value by progress.collectAsStateWithLifecycle()
                NeoTopBar(
                    title = session.title,
                    navigationIcon = {
                        NeoIconButton(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            onClick = ::finish,
                        )
                    },
                    bottom = { NeoProgressBar(progress = value) },
                )
            }
        }

        lifecycleScope.launch {
            container.bookRepository.markOpened(bookId)
        }
        // Mudou o tema em Ajustes? Aplica na hora (página e faixa sob a barra de navegação).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                container.settings.readingTheme.collect { theme ->
                    navigator.submitPreferences(theme.epubPreferences())
                    root.setBackgroundColor(theme.colors().page.toArgb())
                    // A barra de navegação fica sobre a página: ícones escuros no papel, claros à noite.
                    WindowCompat.getInsetsController(window, root).isAppearanceLightNavigationBars =
                        theme == ReadingTheme.PAPER
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigator.currentLocator.collect { progress.value = ReadingProgress.forEpub(it.locations.totalProgression) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigator.currentLocator.debounce(SAVE_DEBOUNCE_MS).collect(::saveProgress)
            }
        }
    }

    override fun onStop() {
        // Garante a última posição mesmo que o debounce não tenha disparado.
        (supportFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as? EpubNavigatorFragment)
            ?.currentLocator?.value?.let(::saveProgress)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) container.epubSessions.close(bookId)
    }

    private fun saveProgress(locator: Locator) {
        val id = bookId
        // appScope: a gravação precisa terminar mesmo se a activity estiver sendo destruída.
        container.appScope.launch {
            container.bookRepository.saveProgress(
                id,
                ReadingProgress.forEpub(locator.locations.totalProgression),
                locator.toJSON().toString(),
            )
        }
    }

    /** Cores do tema de leitura no formato do Readium (o tema LIGHT/DARK cuida dos links e seleção). */
    private fun ReadingTheme.epubPreferences(): EpubPreferences {
        val colors = colors()
        return EpubPreferences(
            theme = if (this == ReadingTheme.NIGHT) Theme.DARK else Theme.LIGHT,
            backgroundColor = ReadiumColor(colors.page.toArgb()),
            textColor = ReadiumColor(colors.text.toArgb()),
        )
    }

    companion object {
        private const val EXTRA_BOOK_ID = "bookId"
        private const val NAVIGATOR_TAG = "epub_navigator"
        private const val SAVE_DEBOUNCE_MS = 500L

        fun intent(context: Context, bookId: Long): Intent =
            Intent(context, EpubReaderActivity::class.java).putExtra(EXTRA_BOOK_ID, bookId)
    }
}
