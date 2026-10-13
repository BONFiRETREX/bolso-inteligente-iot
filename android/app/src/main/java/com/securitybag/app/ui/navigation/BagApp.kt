package com.securitybag.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.securitybag.app.domain.model.BagEvent
import com.securitybag.app.domain.model.BagSnapshot
import com.securitybag.app.domain.model.ConnectionMode
import com.securitybag.app.domain.model.ConnectionStatus
import com.securitybag.app.ui.dashboard.DashboardScreen
import com.securitybag.app.ui.history.HistoryScreen

private const val DASHBOARD_TAB = 0
private const val HISTORY_TAB = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BagApp(
    snapshot: BagSnapshot,
    events: List<BagEvent>,
    connection: ConnectionStatus,
    onAdvanceDemo: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(DASHBOARD_TAB) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bolso Inteligente",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Monitor de seguridad",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            text = connectionBadgeText(connection.mode),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == DASHBOARD_TAB,
                    onClick = { selectedTab = DASHBOARD_TAB },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = "Inicio"
                        )
                    },
                    label = { Text("Inicio") }
                )
                NavigationBarItem(
                    selected = selectedTab == HISTORY_TAB,
                    onClick = { selectedTab = HISTORY_TAB },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "Historial"
                        )
                    },
                    label = { Text("Historial") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                DASHBOARD_TAB -> DashboardScreen(
                    snapshot = snapshot,
                    events = events,
                    connection = connection,
                    onOpenHistory = { selectedTab = HISTORY_TAB },
                    onAdvanceDemo = onAdvanceDemo
                )

                HISTORY_TAB -> HistoryScreen(
                    events = events,
                    connection = connection
                )
            }
        }
    }
}

private fun connectionBadgeText(mode: ConnectionMode): String = when (mode) {
    ConnectionMode.DEMO -> "DATOS DE PRUEBA"
    ConnectionMode.CONNECTING -> "CONECTANDO"
    ConnectionMode.WAITING_FOR_TELEMETRY -> "ESPERANDO ESP32"
    ConnectionMode.LIVE -> "EN VIVO"
    ConnectionMode.ERROR -> "SIN CONEXIÓN"
}
