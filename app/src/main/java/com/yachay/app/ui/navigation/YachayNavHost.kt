package com.yachay.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.yachay.app.R
import com.yachay.app.ui.collections.CollectionDetailScreen
import com.yachay.app.ui.collections.CollectionsScreen
import com.yachay.app.ui.components.NeoPressable
import com.yachay.app.ui.library.LibraryScreen
import com.yachay.app.ui.reader.pdf.PdfReaderScreen
import com.yachay.app.ui.settings.SettingsScreen
import com.yachay.app.ui.theme.Lime
import com.yachay.app.ui.theme.Night
import com.yachay.app.ui.theme.Purple
import com.yachay.app.ui.theme.PurpleDeep
import com.yachay.app.ui.theme.NeoDefaults
import com.yachay.app.ui.theme.Cream

@Composable
fun YachayNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = destination != null && !destination.hasRoute<PdfReaderRoute>()

    Scaffold(
        containerColor = Night,
        bottomBar = {
            if (showBottomBar) NeoBottomBar(destination, navController)
        },
    ) { padding ->
        // A barra de status é tratada por cada NeoTopBar; aqui só reservamos o espaço inferior.
        NavHost(
            navController = navController,
            startDestination = LibraryRoute,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable<LibraryRoute> {
                LibraryScreen(onOpenPdf = { navController.navigate(PdfReaderRoute(it)) })
            }
            composable<CollectionsRoute> {
                CollectionsScreen(onOpenCollection = { navController.navigate(CollectionDetailRoute(it)) })
            }
            composable<SettingsRoute> {
                SettingsScreen()
            }
            composable<CollectionDetailRoute> { entry ->
                val route = entry.toRoute<CollectionDetailRoute>()
                CollectionDetailScreen(
                    collectionId = route.collectionId,
                    onBack = { navController.popBackStack() },
                    onOpenPdf = { navController.navigate(PdfReaderRoute(it)) },
                )
            }
            composable<PdfReaderRoute> { entry ->
                val route = entry.toRoute<PdfReaderRoute>()
                PdfReaderScreen(bookId = route.bookId, onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun NeoBottomBar(destination: NavDestination?, navController: NavHostController) {
    val inCollections = destination?.hasRoute<CollectionsRoute>() == true ||
        destination?.hasRoute<CollectionDetailRoute>() == true
    val inSettings = destination?.hasRoute<SettingsRoute>() == true

    Row(
        Modifier
            .fillMaxWidth()
            .background(Night)
            .drawBehind {
                val stroke = NeoDefaults.BorderWidth.toPx()
                drawLine(Purple, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
            }
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BottomTab(
            label = stringResource(R.string.nav_library),
            icon = Icons.Filled.Home,
            selected = !inCollections && !inSettings,
            selectedColor = Lime,
            modifier = Modifier.weight(1f),
            onClick = { navController.navigateToTab(LibraryRoute) },
        )
        BottomTab(
            label = stringResource(R.string.nav_collections),
            icon = Icons.AutoMirrored.Filled.List,
            selected = inCollections,
            selectedColor = Lime,
            modifier = Modifier.weight(1f),
            onClick = { navController.navigateToTab(CollectionsRoute) },
        )
        BottomTab(
            label = stringResource(R.string.nav_settings),
            icon = Icons.Filled.Settings,
            selected = inSettings,
            selectedColor = Lime,
            modifier = Modifier.weight(1f),
            onClick = { navController.navigateToTab(SettingsRoute) },
        )
    }
}

@Composable
private fun BottomTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    selectedColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NeoPressable(
        onClick = onClick,
        modifier = modifier,
        color = if (selected) selectedColor else PurpleDeep,
        shadow = if (selected) 4.dp else 0.dp,
    ) {
        // Três abas: ícone em cima e rótulo embaixo, para caber em telas estreitas.
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null)
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
