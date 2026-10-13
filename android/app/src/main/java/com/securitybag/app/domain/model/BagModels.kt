package com.securitybag.app.domain.model

/**
 * Estado que llega desde el dispositivo o, temporalmente, desde la fuente simulada.
 * La alarma se conserva como dato explícito porque una apertura queda enclavada
 * hasta que el sistema sea desarmado.
 */
data class BagSnapshot(
    val armed: Boolean,
    val bagOpen: Boolean,
    val keys: TrackedItem,
    val wallet: TrackedItem,
    val alarm: BagAlarm,
    val updatedLabel: String
) {
    val allItemsReady: Boolean
        get() = keys.isReady && wallet.isReady

    val isSecure: Boolean
        get() = armed && !bagOpen && allItemsReady && alarm.type == AlarmType.NONE
}

data class TrackedItem(
    val name: String,
    val isPresent: Boolean,
    val isRfidVerified: Boolean
) {
    val isReady: Boolean
        get() = isPresent && isRfidVerified
}

data class BagAlarm(
    val type: AlarmType,
    val isLatched: Boolean = false
)

enum class AlarmType {
    NONE,
    MISSING_OBJECT,
    OPENING_DETECTED
}

data class BagEvent(
    val id: String,
    val title: String,
    val description: String,
    val timeLabel: String,
    val severity: EventSeverity
)

enum class EventSeverity {
    INFO,
    SUCCESS,
    WARNING,
    CRITICAL
}

/**
 * Indica de dónde provienen los datos que se muestran. Así la interfaz nunca
 * presenta como telemetría real un escenario local de demostración.
 */
data class ConnectionStatus(
    val mode: ConnectionMode,
    val detail: String? = null
) {
    val isDemo: Boolean
        get() = mode == ConnectionMode.DEMO
}

enum class ConnectionMode {
    DEMO,
    CONNECTING,
    WAITING_FOR_TELEMETRY,
    LIVE,
    ERROR
}
