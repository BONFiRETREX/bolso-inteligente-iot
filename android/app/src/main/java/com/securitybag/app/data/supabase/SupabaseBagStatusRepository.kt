package com.securitybag.app.data.supabase

import com.securitybag.app.domain.model.AlarmType
import com.securitybag.app.domain.model.BagAlarm
import com.securitybag.app.domain.model.BagEvent
import com.securitybag.app.domain.model.BagSnapshot
import com.securitybag.app.domain.model.ConnectionMode
import com.securitybag.app.domain.model.ConnectionStatus
import com.securitybag.app.domain.model.EventSeverity
import com.securitybag.app.domain.model.TrackedItem
import com.securitybag.app.domain.repository.BagStatusRepository
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.selectAsFlow
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Fuente de datos de producción: obtiene la primera lectura de PostgREST y
 * conserva el estado actualizado con Supabase Realtime.
 *
 * Las políticas RLS de la migración restringen la fila visible a un solo
 * bag_id. El filtrado local es una segunda protección y no sustituye a RLS.
 */
@OptIn(SupabaseExperimental::class)
class SupabaseBagStatusRepository : BagStatusRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val supabase = createSupabaseClient(
        supabaseUrl = SupabaseConfiguration.url,
        supabaseKey = SupabaseConfiguration.publishableKey
    ) {
        install(Postgrest)
        install(Realtime)
    }

    private val mutableBagStatus = MutableStateFlow(remotePlaceholder())
    private val mutableEvents = MutableStateFlow<List<BagEvent>>(emptyList())
    private val mutableConnection = MutableStateFlow(
        ConnectionStatus(
            mode = ConnectionMode.CONNECTING,
            detail = "Conectando con Supabase Realtime"
        )
    )

    override val bagStatus: StateFlow<BagSnapshot> = mutableBagStatus.asStateFlow()
    override val events: StateFlow<List<BagEvent>> = mutableEvents.asStateFlow()
    override val connection: StateFlow<ConnectionStatus> = mutableConnection.asStateFlow()

    init {
        observeStatus()
        observeEvents()
    }

    private fun observeStatus() {
        scope.launch {
            try {
                supabase.from(STATUS_TABLE)
                    .selectAsFlow(BagStatusDto::bagId)
                    .collect { rows ->
                        val status = rows.firstOrNull { it.bagId == SupabaseConfiguration.bagId }
                        if (status == null) {
                            mutableConnection.value = ConnectionStatus(
                                mode = ConnectionMode.WAITING_FOR_TELEMETRY,
                                detail = "Esperando la primera lectura del ESP32"
                            )
                        } else {
                            mutableBagStatus.value = status.toDomain()
                            mutableConnection.value = ConnectionStatus(
                                mode = ConnectionMode.LIVE,
                                detail = "Actualización recibida mediante Realtime"
                            )
                        }
                    }
            } catch (error: Throwable) {
                handleObservationFailure(error)
            }
        }
    }

    private fun observeEvents() {
        scope.launch {
            try {
                supabase.from(EVENTS_TABLE)
                    .selectAsFlow(BagEventDto::id)
                    .collect { rows ->
                        mutableEvents.value = rows
                            .asSequence()
                            .filter { it.bagId == SupabaseConfiguration.bagId }
                            .sortedByDescending { it.createdAt }
                            .map(BagEventDto::toDomain)
                            .toList()
                    }
            } catch (error: Throwable) {
                handleObservationFailure(error)
            }
        }
    }

    private fun handleObservationFailure(error: Throwable) {
        if (error is CancellationException) {
            throw error
        }

        mutableConnection.value = ConnectionStatus(
            mode = ConnectionMode.ERROR,
            detail = "No fue posible conectar con Supabase. Revise la red y la configuración."
        )
    }

    override fun advanceDemoScenario() = Unit

    override fun stop() {
        scope.cancel()
    }

    private companion object {
        const val STATUS_TABLE = "bag_status"
        const val EVENTS_TABLE = "bag_events"
    }
}

@Serializable
private data class BagStatusDto(
    @SerialName("bag_id") val bagId: String,
    val armed: Boolean,
    @SerialName("bag_open") val bagOpen: Boolean,
    @SerialName("keys_present") val keysPresent: Boolean,
    @SerialName("keys_rfid_verified") val keysRfidVerified: Boolean,
    @SerialName("wallet_present") val walletPresent: Boolean,
    @SerialName("wallet_rfid_verified") val walletRfidVerified: Boolean,
    @SerialName("alarm_type") val alarmType: String,
    @SerialName("alarm_latched") val alarmLatched: Boolean,
    @SerialName("keys_echo_us") val keysEchoUs: Long? = null,
    @SerialName("wallet_echo_us") val walletEchoUs: Long? = null,
    @SerialName("device_timestamp_ms") val deviceTimestampMs: Long? = null,
    @SerialName("updated_at") val updatedAt: String
) {
    fun toDomain(): BagSnapshot = BagSnapshot(
        armed = armed,
        bagOpen = bagOpen,
        keys = TrackedItem(
            name = "Llaves",
            isPresent = keysPresent,
            isRfidVerified = keysRfidVerified
        ),
        wallet = TrackedItem(
            name = "Billetera",
            isPresent = walletPresent,
            isRfidVerified = walletRfidVerified
        ),
        alarm = BagAlarm(
            type = alarmType.toAlarmType(),
            isLatched = alarmLatched
        ),
        updatedLabel = updatedAt.toUpdatedLabel()
    )
}

@Serializable
private data class BagEventDto(
    val id: String,
    @SerialName("bag_id") val bagId: String,
    @SerialName("event_type") val eventType: String,
    val title: String,
    val description: String,
    val severity: String,
    @SerialName("created_at") val createdAt: String
) {
    fun toDomain(): BagEvent = BagEvent(
        id = id,
        title = title,
        description = description,
        timeLabel = createdAt.toClockLabel(),
        severity = severity.toEventSeverity()
    )
}

private fun remotePlaceholder(): BagSnapshot = BagSnapshot(
    armed = false,
    bagOpen = false,
    keys = TrackedItem("Llaves", isPresent = false, isRfidVerified = false),
    wallet = TrackedItem("Billetera", isPresent = false, isRfidVerified = false),
    alarm = BagAlarm(AlarmType.NONE),
    updatedLabel = "Esperando telemetría del ESP32"
)

private fun String.toAlarmType(): AlarmType = when (lowercase(Locale.ROOT)) {
    "missing_object" -> AlarmType.MISSING_OBJECT
    "opening_detected" -> AlarmType.OPENING_DETECTED
    else -> AlarmType.NONE
}

private fun String.toEventSeverity(): EventSeverity = when (uppercase(Locale.ROOT)) {
    "SUCCESS" -> EventSeverity.SUCCESS
    "WARNING" -> EventSeverity.WARNING
    "CRITICAL" -> EventSeverity.CRITICAL
    else -> EventSeverity.INFO
}

private fun String.toUpdatedLabel(): String {
    val instant = runCatching { Instant.parse(this) }.getOrNull()
        ?: return "Actualización recibida"
    val seconds = Duration.between(instant, Instant.now()).seconds.coerceAtLeast(0)

    return when {
        seconds < 60 -> "Actualizado hace unos segundos"
        seconds < 3600 -> "Actualizado hace " + seconds / 60 + " min"
        seconds < 86400 -> "Actualizado hace " + seconds / 3600 + " h"
        else -> "Actualizado el " + toClockLabel()
    }
}

private fun String.toClockLabel(): String {
    val instant = runCatching { Instant.parse(this) }.getOrNull()
        ?: return "—"
    return DateTimeFormatter
        .ofPattern("HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)
}
