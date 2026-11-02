package com.yachay.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yachay.app.ui.navigation.YachayNavHost
import com.yachay.app.ui.theme.YachayTheme

/**
 * O app tem um tema só (escuro), independente do modo do celular: a barra de status fica sobre o
 * topo roxo e a de navegação sobre o fundo Night — ícones claros nas duas.
 */
val YachayStatusBar = SystemBarStyle.dark(Color.TRANSPARENT)
val YachayNavigationBar = SystemBarStyle.dark(Color.TRANSPARENT)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = YachayStatusBar, navigationBarStyle = YachayNavigationBar)
        setContent {
            YachayTheme {
                YachayNavHost()
            }
        }
    }
}
