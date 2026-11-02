package com.yachay.app.ui.reader.epub

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.material.icons.filled.Search
import com.yachay.app.ui.reader.BookSearch
import com.yachay.app.ui.reader.SearchSheet
import com.yachay.app.ui.theme.Lime
import org.readium.r2.navigator.Decoration
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.ui.platform.LocalContext
import com.yachay.app.data.db.AnnotationType
import com.yachay.app.ui.reader.AnnotationsSheet
import com.yachay.app.ui.reader.BookmarkButton
import com.yachay.app.ui.reader.HighlightActionsSheet
import kotlinx.coroutines.flow.combine
import org.readium.r2.navigator.DecorableNavigator
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
import com.yachay.app.data.goals.PageRecorded
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
    private val lastNotice = MutableStateFlow<PageRecorded?>(null)
    private lateinit var annotations: EpubAnnotations

    private lateinit var search: BookSearch<Locator>

    /** Destaque tocado na página (abre a sheet "Remover destaque"). */
    private val tappedHighlight = MutableStateFlow<Long?>(null)

    private val navigator: EpubNavigatorFragment?
        get() = supportFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as? EpubNavigatorFragment

    /** "Destacar" no menu de seleção de texto (o resto do menu do sistema continua lá). */
    private val selectionCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.add(Menu.NONE, MENU_HIGHLIGHT, 0, R.string.highlight_action)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            if (item.itemId != MENU_HIGHLIGHT) return false
            lifecycleScope.launch { highlightSelection() }
            mode.finish()
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        bookId = intent.getLongExtra(EXTRA_BOOK_ID, -1)
        val session = container.epubSessions[bookId]
        if (session == null) {
            // Processo morreu com o leitor aberto: a publicação não está mais em memória.
            super.onCreate(null)
            finish()
            return
        }

        annotations = EpubAnnotations(bookId, session.publication, container.annotations)
        search = BookSearch(lifecycleScope, epubSearchEngine(applicationContext, session.publication, annotations))
        supportFragmentManager.fragmentFactory = EpubNavigatorFactory(session.publication)
            .createFragmentFactory(
                initialLocator = session.initialLocator,
                initialPreferences = session.initialPreferences.toEpubPreferences(),
                configuration = EpubNavigatorFragment.Configuration {
                    selectionActionModeCallback = selectionCallback
                },
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
        navigator.addDecorationListener(
            EpubAnnotations.HIGHLIGHTS_GROUP,
            object : DecorableNavigator.Listener {
                override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean {
                    tappedHighlight.value = EpubAnnotations.annotationId(event.decoration.id)
                    return true
                }
            },
        )

        findViewById<ComposeView>(R.id.reader_top_bar).setContent {
            YachayTheme {
                val value by progress.collectAsStateWithLifecycle()
                val goal by container.dailyGoal.progress.collectAsStateWithLifecycle(initialValue = DailyGoalProgress(0))
                val showGoalNotice by goalNotice.collectAsStateWithLifecycle()
                val notice by lastNotice.collectAsStateWithLifecycle()
                val preferences by container.settings.readerPreferences
                    .collectAsStateWithLifecycle(initialValue = session.initialPreferences)
                var showTextSettings by rememberSaveable { mutableStateOf(false) }
                var showAnnotations by rememberSaveable { mutableStateOf(false) }
                var showSearch by rememberSaveable { mutableStateOf(false) }
                val searchQuery by search.query.collectAsStateWithLifecycle()
                val searchState by search.state.collectAsStateWithLifecycle()
                val entries by annotations.entries.collectAsStateWithLifecycle(initialValue = emptyList())
                val locator by navigator.currentLocator.collectAsStateWithLifecycle()
                val bookmarkHere = annotations.bookmarkAt(entries, locator)
                val tapped by tappedHighlight.collectAsStateWithLifecycle()
                NeoTopBar(
                    title = session.title,
                    stackTitle = true,
                    navigationIcon = {
                        NeoIconButton(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            onClick = ::finish,
                        )
                    },
                    actions = {
                        NeoIconButton(
                            Icons.Filled.Search,
                            contentDescription = stringResource(R.string.search_open),
                            onClick = { showSearch = true },
                        )
                        BookmarkButton(marked = bookmarkHere != null, onClick = { toggleBookmark(bookmarkHere) })
                        NeoIconButton(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.annotations_open),
                            onClick = { showAnnotations = true },
                        )
                        TextSettingsButton(onClick = { showTextSettings = true })
                    },
                    bottom = {
                        Box {
                            NeoProgressBar(progress = value, goal = goal)
                            DailyGoalNotice(visible = showGoalNotice, progress = goal, unlockedLevel = notice?.unlockedLevel, streakBonus = notice?.streakBonus)
                        }
                    },
                )
                if (showSearch) {
                    SearchSheet(
                        query = searchQuery,
                        onQueryChange = { search.query.value = it },
                        state = searchState,
                        onOpen = { hit ->
                            showSearch = false
                            lifecycleScope.launch { showSearchResult(hit.target) }
                        },
                        onDismiss = { showSearch = false },
                    )
                }
                if (showAnnotations) {
                    val context = LocalContext.current
                    AnnotationsSheet(
                        bookmarks = annotations.items(context, entries, AnnotationType.BOOKMARK),
                        highlights = annotations.items(context, entries, AnnotationType.HIGHLIGHT),
                        onOpen = { id ->
                            entries.firstOrNull { it.annotation.id == id }?.let { navigator.go(it.locator, animated = false) }
                            showAnnotations = false
                        },
                        onRemove = { id -> container.appScope.launch { annotations.remove(id) } },
                        onDismiss = { showAnnotations = false },
                    )
                }
                tapped?.let { id ->
                    HighlightActionsSheet(
                        text = entries.firstOrNull { it.annotation.id == id }?.annotation?.text,
                        onRemove = {
                            container.appScope.launch { annotations.remove(id) }
                            tappedHighlight.value = null
                        },
                        onDismiss = { tappedHighlight.value = null },
                    )
                }
                if (showTextSettings) {
                    val settings = container.settings
                    // appScope: a escolha é gravada mesmo se o leitor fechar logo em seguida.
                    ReaderSettingsSheet(
                        preferences = preferences,
                        onFontScale = { container.appScope.launch { settings.setFontScale(it) } },
                        onLineSpacing = { container.appScope.launch { settings.setLineSpacing(it) } },
                        onFont = { container.appScope.launch { settings.setFont(it) } },
                        onMargins = { container.appScope.launch { settings.setMargins(it) } },
                        onTextAlign = { container.appScope.launch { settings.setTextAlign(it) } },
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
        // Destaques pintados na página; o tema muda a cor. O Readium reposiciona ao re-paginar
        // (mudar a fonte), porque o destaque é ancorado no texto do locator.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(annotations.entries, container.settings.readingTheme, ::Pair).collect { (entries, theme) ->
                    navigator.applyDecorations(annotations.decorations(entries, theme), EpubAnnotations.HIGHLIGHTS_GROUP)
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
                    if (recorded.shouldNotify) showGoalNotice(recorded)
                }
            }
        }
    }

    private fun showGoalNotice(recorded: PageRecorded) = lifecycleScope.launch {
        lastNotice.value = recorded
        goalNotice.value = true
        delay(NOTICE_MS)
        goalNotice.value = false
    }

    /** Vai até o resultado e pinta o termo por alguns segundos (grupo de decoração próprio). */
    private suspend fun showSearchResult(locator: Locator) {
        val navigator = navigator ?: return
        navigator.go(locator, animated = false)
        val tint = Lime.toArgb()
        navigator.applyDecorations(
            listOf(Decoration(id = "search", locator = locator, style = Decoration.Style.Highlight(tint = tint, isActive = true))),
            SEARCH_GROUP,
        )
        delay(SEARCH_HIGHLIGHT_MS)
        navigator.applyDecorations(emptyList(), SEARCH_GROUP)
    }

    private suspend fun highlightSelection() {
        val navigator = navigator ?: return
        val selection = navigator.currentSelection() ?: return
        val text = selection.locator.text.highlight?.trim().orEmpty()
        navigator.clearSelection()
        if (text.isEmpty()) return
        // appScope: grava mesmo se o leitor fechar logo em seguida.
        container.appScope.launch { annotations.addHighlight(selection.locator, text) }
    }

    private fun toggleBookmark(existing: EpubAnnotations.Entry?) {
        val locator = navigator?.currentLocator?.value ?: return
        container.appScope.launch {
            if (existing != null) annotations.remove(existing.annotation.id) else annotations.addBookmark(locator)
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

    companion object {
        private const val EXTRA_BOOK_ID = "bookId"
        private const val NAVIGATOR_TAG = "epub_navigator"
        private const val SAVE_DEBOUNCE_MS = 500L
        private const val NOTICE_MS = 3_000L
        private const val MENU_HIGHLIGHT = 1
        private const val SEARCH_GROUP = "search"
        private const val SEARCH_HIGHLIGHT_MS = 4_000L

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
