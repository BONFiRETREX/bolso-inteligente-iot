package com.securitybag.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.securitybag.app.domain.model.AlarmType
import com.securitybag.app.domain.model.BagEvent
import com.securitybag.app.domain.model.BagSnapshot
import com.securitybag.app.domain.model.ConnectionMode
import com.securitybag.app.domain.model.ConnectionStatus
import com.securitybag.app.domain.model.EventSeverity
import com.securitybag.app.domain.model.TrackedItem
import com.securitybag.app.ui.theme.BolsoInteligenteTheme
import com.securitybag.app.ui.theme.CriticalContainer
import com.securitybag.app.ui.theme.OnCriticalContainer
import com.securitybag.app.ui.theme.OnSuccessContainer
import com.securitybag.app.ui.theme.OnWarningContainer
import com.securitybag.app.ui.theme.Success
import com.securitybag.app.ui.theme.SuccessContainer
import com.securitybag.app.ui.theme.WarningContainer

@Composable
fun DashboardScreen(
    snapshot: BagSnapshot,
    events: List<BagEvent>,
    connection: ConnectionStatus,
    onOpenHistory: () -> Unit,
    onAdvanceDemo: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Header(snapshot = snapshot)
        }
        if (!connection.isDemo && connection.mode != ConnectionMode.LIVE) {
            item {
                ConnectionNotice(connection = connection)
            }
        }
        item {
            ProtectionCard(snapshot = snapshot)
        }
        item {
            StatusSummary(snapshot = snapshot)
        }
        item {
            SectionLabel(text = "Objetos esenciales")
        }
        item {
            ItemStatusCard(
                item = snapshot.keys,
                icon = Icons.Outlined.Key
            )
        }
        item {
            ItemStatusCard(
                item = snapshot.wallet,
                icon = Icons.Outlined.AccountBalanceWallet
            )
        }
        item {
            AlertCard(snapshot = snapshot)
        }
        item {
            RecentActivity(
                events = events,
                onOpenHistory = onOpenHistory
            )
        }
        if (connection.isDemo) {
            item {
                DemoControl(onAdvanceDemo = onAdvanceDemo)
            }
        }
    }
}

@Composable
private fun ConnectionNotice(connection: ConnectionStatus) {
    val isError = connection.mode == ConnectionMode.ERROR
    val container = if (isError) {
        CriticalContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val content = if (isError) {
        OnCriticalContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = content
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isError) "Conexión no disponible" else "Telemetría pendiente",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = connection.detail ?: "Esperando una actualización.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun Header(snapshot: BagSnapshot) {
    Column {
        Text(
            text = if (snapshot.armed) "Estado del bolso" else "Sistema en espera",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = snapshot.updatedLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProtectionCard(snapshot: BagSnapshot) {
    val presentation = when {
        snapshot.alarm.type == AlarmType.OPENING_DETECTED -> HeroPresentation(
            title = "APERTURA DETECTADA",
            message = "La alarma está activa y se mantiene hasta desarmar el sistema.",
            icon = Icons.Outlined.WarningAmber,
            container = CriticalContainer,
            content = OnCriticalContainer
        )

        snapshot.alarm.type == AlarmType.MISSING_OBJECT -> HeroPresentation(
            title = "REVISIÓN NECESARIA",
            message = "Hay un objeto esencial que no está en su compartimiento.",
            icon = Icons.Outlined.WarningAmber,
            container = WarningContainer,
            content = OnWarningContainer
        )

        snapshot.isSecure -> HeroPresentation(
            title = "TODO PROTEGIDO",
            message = "Bolso cerrado, armado y con los objetos verificados.",
            icon = Icons.Outlined.CheckCircle,
            container = SuccessContainer,
            content = OnSuccessContainer
        )

        snapshot.armed -> HeroPresentation(
            title = "ARMADO CON VERIFICACIÓN PENDIENTE",
            message = "Revise los objetos antes de alejarse del bolso.",
            icon = Icons.Outlined.Security,
            container = WarningContainer,
            content = OnWarningContainer
        )

        else -> HeroPresentation(
            title = "SISTEMA DESARMADO",
            message = "El monitoreo visual continúa, pero no se activa la alarma.",
            icon = Icons.Outlined.LockOpen,
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = presentation.container,
            contentColor = presentation.content
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = presentation.content.copy(alpha = 0.12f),
                contentColor = presentation.content,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    imageVector = presentation.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = presentation.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = presentation.message,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun StatusSummary(snapshot: BagSnapshot) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusPill(
            label = "Sistema",
            value = if (snapshot.armed) "Armado" else "Desarmado",
            icon = if (snapshot.armed) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
            container = if (snapshot.armed) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            content = if (snapshot.armed) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(1f)
        )
        StatusPill(
            label = "Bolso",
            value = if (snapshot.bagOpen) "Abierto" else "Cerrado",
            icon = if (snapshot.bagOpen) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
            container = if (snapshot.bagOpen) CriticalContainer else SuccessContainer,
            content = if (snapshot.bagOpen) OnCriticalContainer else OnSuccessContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatusPill(
    label: String,
    value: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "$label: $value",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelMedium)
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ItemStatusCard(
    item: TrackedItem,
    icon: ImageVector
) {
    val ready = item.isReady
    val container = if (ready) SuccessContainer else MaterialTheme.colorScheme.surface
    val content = if (ready) OnSuccessContainer else MaterialTheme.colorScheme.onSurface
    val stateText = when {
        item.isReady -> "Protegido"
        !item.isPresent -> "No detectado"
        else -> "Pendiente de identificación"
    }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = content
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (ready) {
                        content.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    contentColor = if (ready) content else MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stateText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = content.copy(alpha = 0.78f)
                    )
                }
                StateChip(
                    text = if (ready) "OK" else "REVISAR",
                    success = ready
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = content.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(12.dp))
            StatusLine(
                label = "Presencia física",
                value = if (item.isPresent) "Detectada por ultrasónico" else "No detectada",
                complete = item.isPresent
            )
            Spacer(modifier = Modifier.height(8.dp))
            StatusLine(
                label = "Verificación RFID",
                value = if (item.isRfidVerified) "Identidad confirmada" else "Pendiente",
                complete = item.isRfidVerified
            )
        }
    }
}

@Composable
private fun StateChip(text: String, success: Boolean) {
    Surface(
        color = if (success) SuccessContainer else WarningContainer,
        contentColor = if (success) OnSuccessContainer else OnWarningContainer,
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun StatusLine(label: String, value: String, complete: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (complete) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = if (complete) Success else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlertCard(snapshot: BagSnapshot) {
    val missingObjects = listOfNotNull(
        if (!snapshot.keys.isPresent) "las llaves" else null,
        if (!snapshot.wallet.isPresent) "la billetera" else null
    ).joinToString(" y ")

    val presentation = when {
        snapshot.alarm.type == AlarmType.OPENING_DETECTED -> AlertPresentation(
            title = "Alerta de apertura",
            message = if (snapshot.alarm.isLatched) {
                "La alarma permanece activa hasta desarmar el sistema."
            } else {
                "Se detectó una apertura mientras el sistema estaba armado."
            },
            icon = Icons.Outlined.WarningAmber,
            container = CriticalContainer,
            content = OnCriticalContainer
        )

        snapshot.alarm.type == AlarmType.MISSING_OBJECT -> AlertPresentation(
            title = "Objeto faltante",
            message = "Revise $missingObjects antes de continuar.",
            icon = Icons.Outlined.WarningAmber,
            container = WarningContainer,
            content = OnWarningContainer
        )

        snapshot.isSecure -> AlertPresentation(
            title = "Sin alertas activas",
            message = "El bolso cumple las condiciones de protección.",
            icon = Icons.Outlined.CheckCircle,
            container = SuccessContainer,
            content = OnSuccessContainer
        )

        !snapshot.armed -> AlertPresentation(
            title = "Sistema desarmado",
            message = "No hay alarma activa. El estado mostrado es solo informativo.",
            icon = Icons.Outlined.LockOpen,
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurfaceVariant
        )

        else -> AlertPresentation(
            title = "Verificación pendiente",
            message = "Un objeto necesita presencia física y confirmación RFID.",
            icon = Icons.Outlined.WarningAmber,
            container = WarningContainer,
            content = OnWarningContainer
        )
    }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = presentation.container,
            contentColor = presentation.content
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = presentation.icon,
                contentDescription = presentation.title,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = presentation.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = presentation.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun RecentActivity(
    events: List<BagEvent>,
    onOpenHistory: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel(text = "Actividad reciente")
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenHistory) {
                Text("Ver historial")
            }
        }
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                events.take(3).forEachIndexed { index, event ->
                    EventRow(event = event)
                    if (index < events.take(3).lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: BagEvent) {
    val (icon, tint) = when (event.severity) {
        EventSeverity.SUCCESS -> Icons.Outlined.CheckCircle to Success
        EventSeverity.WARNING -> Icons.Outlined.WarningAmber to MaterialTheme.colorScheme.tertiary
        EventSeverity.CRITICAL -> Icons.Outlined.WarningAmber to MaterialTheme.colorScheme.error
        EventSeverity.INFO -> Icons.Outlined.Security to MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = event.timeLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DemoControl(onAdvanceDemo: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "Modo demostración",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Estos estados son locales; todavía no se envían órdenes al ESP32.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = onAdvanceDemo,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cambiar escenario de prueba")
            }
        }
    }
}

private data class HeroPresentation(
    val title: String,
    val message: String,
    val icon: ImageVector,
    val container: Color,
    val content: Color
)

private data class AlertPresentation(
    val title: String,
    val message: String,
    val icon: ImageVector,
    val container: Color,
    val content: Color
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DashboardPreview() {
    BolsoInteligenteTheme {
        DashboardScreen(
            snapshot = BagSnapshot(
                armed = true,
                bagOpen = false,
                keys = TrackedItem("Llaves", isPresent = true, isRfidVerified = true),
                wallet = TrackedItem("Billetera", isPresent = true, isRfidVerified = true),
                alarm = com.securitybag.app.domain.model.BagAlarm(AlarmType.NONE),
                updatedLabel = "Actualizado hace unos segundos"
            ),
            events = listOf(
                BagEvent(
                    id = "preview",
                    title = "Sistema armado",
                    description = "Bolso cerrado y objetos verificados.",
                    timeLabel = "10:42",
                    severity = EventSeverity.SUCCESS
                )
            ),
            connection = ConnectionStatus(ConnectionMode.DEMO),
            onOpenHistory = {},
            onAdvanceDemo = {}
        )
    }
}
