package com.securitybag.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.securitybag.app.domain.model.BagEvent
import com.securitybag.app.domain.model.EventSeverity
import com.securitybag.app.domain.model.ConnectionMode
import com.securitybag.app.domain.model.ConnectionStatus
import com.securitybag.app.ui.theme.Success

@Composable
fun HistoryScreen(
    events: List<BagEvent>,
    connection: ConnectionStatus
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Historial de eventos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = historySubtitle(connection.mode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(items = events, key = { it.id }) { event ->
            HistoryEventCard(event = event)
        }
    }
}

private fun historySubtitle(mode: ConnectionMode): String = when (mode) {
    ConnectionMode.DEMO -> "Registro mostrado desde los datos de prueba actuales."
    ConnectionMode.LIVE -> "Registro recibido desde Supabase Realtime."
    ConnectionMode.CONNECTING -> "Conectando con el historial remoto."
    ConnectionMode.WAITING_FOR_TELEMETRY -> "Aún no hay eventos enviados por el ESP32."
    ConnectionMode.ERROR -> "No se pudo cargar el historial remoto."
}

@Composable
private fun HistoryEventCard(event: BagEvent) {
    val (icon, tint, label) = when (event.severity) {
        EventSeverity.SUCCESS -> Triple(
            Icons.Outlined.CheckCircle,
            Success,
            "Correcto"
        )

        EventSeverity.WARNING -> Triple(
            Icons.Outlined.WarningAmber,
            MaterialTheme.colorScheme.tertiary,
            "Advertencia"
        )

        EventSeverity.CRITICAL -> Triple(
            Icons.Outlined.WarningAmber,
            MaterialTheme.colorScheme.error,
            "Crítico"
        )

        EventSeverity.INFO -> Triple(
            Icons.Outlined.Security,
            MaterialTheme.colorScheme.primary,
            "Información"
        )
    }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = event.timeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
