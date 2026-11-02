package com.yachay.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yachay.app.AppContainer
import com.yachay.app.R
import com.yachay.app.appContainer
import com.yachay.app.data.db.BookEntity
import com.yachay.app.domain.ReaderLevel
import com.yachay.app.domain.ReaderStats
import com.yachay.app.ui.collections.BookActionsSheet
import com.yachay.app.ui.components.AlsoReadingCard
import com.yachay.app.ui.components.ImpactMarks
import com.yachay.app.ui.components.NeoIconButton
import com.yachay.app.ui.components.NeoProgressBar
import com.yachay.app.ui.components.NeoSurface
import com.yachay.app.ui.components.NeoTopBar
import com.yachay.app.ui.components.SectionHeader
import com.yachay.app.ui.components.TopBarSubtitle
import com.yachay.app.ui.reader.rememberBookOpener
import com.yachay.app.ui.theme.Cream
import com.yachay.app.ui.theme.CreamMuted
import com.yachay.app.ui.theme.Halftone
import com.yachay.app.ui.theme.Ink
import com.yachay.app.ui.theme.InkMuted
import com.yachay.app.ui.theme.Lime
import com.yachay.app.ui.theme.Night
import com.yachay.app.ui.theme.Purple
import com.yachay.app.ui.theme.paperGrain
import com.yachay.app.ui.theme.shadeFor
import com.yachay.app.ui.theme.tabular
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(container: AppContainer) : ViewModel() {

    private val dailyGoal = container.dailyGoal

    val stats: StateFlow<ReaderStats> = dailyGoal.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderStats())

    fun chooseAvatar(level: Int) = viewModelScope.launch { dailyGoal.chooseAvatar(level) }

    val recentBooks: StateFlow<List<BookEntity>> = container.bookRepository.observeRecentlyOpened()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** Pergunta da tela: "como estou indo?". Responde com o avatar, o nível e a barra de XP. */
@Composable
fun ProfileScreen(onBack: () -> Unit, onOpenPdf: (Long) -> Unit) {
    val context = LocalContext.current
    val viewModel = viewModel { ProfileViewModel(context.appContainer) }
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val recent by viewModel.recentBooks.collectAsStateWithLifecycle()
    val openBook = rememberBookOpener(onOpenPdf)
    var actionsFor by remember { mutableStateOf<BookEntity?>(null) }

    Scaffold(
        containerColor = Night,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            NeoTopBar(
                title = stringResource(R.string.profile_title),
                navigationIcon = {
                    NeoIconButton(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        onClick = onBack,
                    )
                },
                bottom = { TopBarSubtitle(stringResource(R.string.profile_level_of, stats.level, ReaderLevel.MAX_LEVEL)) },
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
            LevelCard(stats)

            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    value = stats.totalPages,
                    label = pluralStringResource(R.plurals.profile_pages_read, pluralQuantity(stats.totalPages)),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                StatCard(
                    value = stats.goalsMet,
                    label = pluralStringResource(R.plurals.profile_goals_met, pluralQuantity(stats.goalsMet)),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
            Text(
                stringResource(R.string.profile_pages_note),
                style = MaterialTheme.typography.bodySmall,
                color = CreamMuted,
            )

            SectionHeader(
                stringResource(R.string.profile_avatars),
                modifier = Modifier.padding(top = 20.dp),
                count = stats.level,
            )
            Text(
                stringResource(R.string.profile_avatars_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = CreamMuted,
            )
            AvatarGallery(level = stats.level, chosen = stats.avatar, onChoose = viewModel::chooseAvatar)

            SectionHeader(stringResource(R.string.profile_recent_books), modifier = Modifier.padding(top = 20.dp))
            if (recent.isEmpty()) {
                Text(
                    stringResource(R.string.profile_recent_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CreamMuted,
                )
            } else {
                recent.forEach { book ->
                    AlsoReadingCard(
                        book,
                        onClick = { openBook(book) },
                        onLongClick = { actionsFor = book },
                        modifier = Modifier.fillMaxWidth(),
                        fill = true,
                    )
                }
            }
        }
    }

    actionsFor?.let { book ->
        BookActionsSheet(book = book, onDismiss = { actionsFor = null })
    }
}

/** O bloco principal: avatar atual, nível e quanto falta para o próximo avatar. */
@Composable
private fun LevelCard(stats: ReaderStats) {
    NeoSurface(
        modifier = Modifier.fillMaxWidth(),
        color = Purple,
        shadow = 6.dp,
        halftone = Halftone(shadeFor(Purple), Alignment.BottomEnd, spacing = 9.dp, maxDot = 4.dp, reach = 0.6f),
    ) {
        ImpactMarks(Modifier.align(Alignment.TopEnd).padding(10.dp), color = Lime)
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Sobre roxo a sombra é limão (tinta sumiria no escuro).
            Avatar(stats.avatar, size = 88.dp, shadow = 5.dp, shadowColor = Lime)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.profile_level, stats.level),
                    style = MaterialTheme.typography.headlineSmall.tabular(),
                    color = Cream,
                )
                NeoProgressBar(progress = stats.levelFraction, height = 12.dp, showLabel = false, fillColor = Lime)
                Text(
                    if (stats.isMaxLevel) {
                        stringResource(R.string.profile_max_level, stats.xp)
                    } else {
                        stringResource(R.string.profile_xp_of, stats.xpInLevel, stats.xpForNextLevel)
                    },
                    style = MaterialTheme.typography.labelLarge.tabular(),
                    color = Cream,
                )
                if (!stats.isMaxLevel) {
                    Text(
                        pluralStringResource(R.plurals.profile_goals_to_next, stats.goalsToNextLevel, stats.goalsToNextLevel),
                        style = MaterialTheme.typography.bodySmall,
                        color = CreamMuted,
                    )
                }
            }
        }
    }
}

/** O plural pt-BR do Android trata 0 como "one" ("0 página lida"); zero pede o plural. */
private fun pluralQuantity(count: Int): Int = if (count == 0) 2 else count

@Composable
private fun StatCard(value: Int, label: String, modifier: Modifier = Modifier) {
    NeoSurface(modifier = modifier, color = Cream) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium.tabular(), color = Ink)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
        }
    }
}

/**
 * Os 16 avatares em grade 4×4. Desbloqueados são tocáveis e viram a foto de perfil; os
 * bloqueados aparecem apagados, com o nível que os libera.
 */
@Composable
private fun AvatarGallery(level: Int, chosen: Int, onChoose: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..ReaderLevel.MAX_LEVEL).chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { avatarLevel ->
                    val locked = avatarLevel > level
                    val selected = avatarLevel == chosen
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Avatar(
                                avatarLevel,
                                size = null,
                                // O escolhido ganha a sombra limão, como a seleção no resto do app.
                                shadowColor = if (selected) Lime else Purple,
                                locked = locked,
                                onClick = if (locked) null else ({ onChoose(avatarLevel) }),
                                selected = selected,
                            )
                            if (locked) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = Cream)
                            }
                        }
                        Text(
                            if (selected) stringResource(R.string.profile_avatar_in_use) else stringResource(R.string.profile_level_short, avatarLevel),
                            style = MaterialTheme.typography.labelMedium.tabular(),
                            color = when {
                                selected -> Lime
                                locked -> CreamMuted
                                else -> LocalContentColor.current
                            },
                        )
                    }
                }
            }
        }
    }
}
