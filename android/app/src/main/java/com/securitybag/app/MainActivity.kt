package com.securitybag.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.securitybag.app.ui.dashboard.DashboardViewModel
import com.securitybag.app.ui.navigation.BagApp
import com.securitybag.app.ui.theme.BolsoInteligenteTheme

class MainActivity : ComponentActivity() {
    private val dashboardViewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val bagStatus by dashboardViewModel.bagStatus.collectAsState()
            val events by dashboardViewModel.events.collectAsState()
            val connection by dashboardViewModel.connection.collectAsState()

            BolsoInteligenteTheme {
                BagApp(
                    snapshot = bagStatus,
                    events = events,
                    connection = connection,
                    onAdvanceDemo = dashboardViewModel::advanceDemoScenario
                )
            }
        }
    }
}
