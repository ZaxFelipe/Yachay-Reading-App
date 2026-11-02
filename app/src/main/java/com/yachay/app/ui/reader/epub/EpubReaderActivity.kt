package com.yachay.app.ui.reader.epub

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.yachay.app.ui.components.NeoPressable
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
import com.yachay.app.domain.DailyGoal
import com.yachay.app.domain.DailyGoalProgress
import com.yachay.app.domain.ReadingProgress
import com.yachay.app.ui.components.DailyGoalNotice
import androidx.compose.foundation.layout.Box
import kotlinx.coroutines.delay
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
    private val goalNotice = MutableStateFlow(false)
    private val unlockedLevel = MutableStateFlow<Int?>(null)

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
                initialPreferences = session.initialPreferences.toEpubPreferences(),
            )

        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = YachayStatusBar, navigationBarStyle = YachayNavigationBar)
        setContentView(R.layout.activity_epub_reader)

        val root = findViewById<View>(R.id.reader_root)
        root.setBackgroundColor(session.initialPreferences.theme.colors().page.toArgb())
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
                val goal by container.dailyGoal.progress.collectAsStateWithLifecycle(initialValue = DailyGoalProgress(0))
                val showGoalNotice by goalNotice.collectAsStateWithLifecycle()
                val newLevel by unlockedLevel.collectAsStateWithLifecycle()
                val preferences by container.settings.readerPreferences
                    .collectAsStateWithLifecycle(initialValue = session.initialPreferences)
                var showTextSettings by rememberSaveable { mutableStateOf(false) }
                NeoTopBar(
                    title = session.title,
                    navigationIcon = {
                        NeoIconButton(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            onClick = ::finish,
                        )
                    },
                    actions = { TextSettingsButton(onClick = { showTextSettings = true }) },
                    bottom = {
                        Box {
                            NeoProgressBar(progress = value, goal = goal)
                            DailyGoalNotice(visible = showGoalNotice, progress = goal, unlockedLevel = newLevel)
                        }
                    },
                )
                if (showTextSettings) {
                    val settings = container.settings
                    // appScope: a escolha é gravada mesmo se o leitor fechar logo em seguida.
                    ReaderSettingsSheet(
                        preferences = preferences,
                        onFontScale = { container.appScope.launch { settings.setFontScale(it) } },
                        onLineSpacing = { container.appScope.launch { settings.setLineSpacing(it) } },
                        onTheme = { container.appScope.launch { settings.setReadingTheme(it) } },
                        onDismiss = { showTextSettings = false },
                    )
                }
            }
        }

        lifecycleScope.launch {
            container.bookRepository.markOpened(bookId)
        }
        // Mudou tema/fonte (na sheet "Aa" ou em Ajustes)? Aplica na hora; o Readium re-pagina
        // mantendo o locator atual, e o progresso se recalcula pelo currentLocator.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                container.settings.readerPreferences.collect { preferences ->
                    val theme = preferences.theme
                    navigator.submitPreferences(preferences.toEpubPreferences())
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Meta do dia: a "página" é a tela onde a pessoa ficou alguns segundos.
                navigator.currentLocator.debounce(DailyGoal.PAGE_DWELL_MS).collect { locator ->
                    val progression = locator.locations.progression ?: return@collect
                    val page = DailyGoal.epubPage(locator.href.toString(), progression)
                    val recorded = container.dailyGoal.recordPage(bookId, page)
                    if (recorded.shouldNotify) showGoalNotice(recorded.unlockedLevel)
                }
            }
        }
    }

    private fun showGoalNotice(level: Int?) = lifecycleScope.launch {
        unlockedLevel.value = level
        goalNotice.value = true
        delay(NOTICE_MS)
        goalNotice.value = false
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

    companion object {
        private const val EXTRA_BOOK_ID = "bookId"
        private const val NAVIGATOR_TAG = "epub_navigator"
        private const val SAVE_DEBOUNCE_MS = 500L
        private const val NOTICE_MS = 3_000L

        fun intent(context: Context, bookId: Long): Intent =
            Intent(context, EpubReaderActivity::class.java).putExtra(EXTRA_BOOK_ID, bookId)
    }
}

/** "Aa" na barra do leitor: abre tamanho da fonte, entrelinha e tema. */
@Composable
private fun TextSettingsButton(onClick: () -> Unit) {
    val description = stringResource(R.string.reader_text_settings)
    NeoPressable(onClick = onClick, shadow = 3.dp, modifier = Modifier.semantics { contentDescription = description }) {
        Text(
            "Aa",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}
