package com.securitybag.app.data.fake

import com.securitybag.app.domain.model.AlarmType
import com.securitybag.app.domain.model.BagAlarm
import com.securitybag.app.domain.model.BagEvent
import com.securitybag.app.domain.model.BagSnapshot
import com.securitybag.app.domain.model.ConnectionMode
import com.securitybag.app.domain.model.ConnectionStatus
import com.securitybag.app.domain.model.EventSeverity
import com.securitybag.app.domain.model.TrackedItem
import com.securitybag.app.domain.repository.BagStatusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Datos locales para presentar y probar la interfaz sin red ni credenciales.
 * El botón de demostración recorre escenarios representativos del ESP32.
 */
class FakeBagStatusRepository : BagStatusRepository {
    private val scenarios = listOf(
        DemoScenario(
            snapshot = BagSnapshot(
                armed = true,
                bagOpen = false,
                keys = TrackedItem("Llaves", isPresent = true, isRfidVerified = true),
                wallet = TrackedItem("Billetera", isPresent = true, isRfidVerified = true),
                alarm = BagAlarm(AlarmType.NONE),
                updatedLabel = "Actualizado hace unos segundos"
            ),
            events = listOf(
                event("1", "Sistema armado", "Bolso cerrado y objetos verificados.", "10:42", EventSeverity.SUCCESS),
                event("2", "Billetera verificada", "RFID y sensor ultrasónico confirmados.", "10:41", EventSeverity.INFO),
                event("3", "Llaves verificadas", "RFID y sensor ultrasónico confirmados.", "10:40", EventSeverity.INFO)
            )
        ),
        DemoScenario(
            snapshot = BagSnapshot(
                armed = true,
                bagOpen = false,
                keys = TrackedItem("Llaves", isPresent = true, isRfidVerified = true),
                wallet = TrackedItem("Billetera", isPresent = false, isRfidVerified = true),
                alarm = BagAlarm(AlarmType.MISSING_OBJECT),
                updatedLabel = "Actualizado hace unos segundos"
            ),
            events = listOf(
                event("4", "Billetera retirada", "El sensor ya no detecta la billetera.", "10:44", EventSeverity.WARNING),
                event("1", "Sistema armado", "Bolso cerrado y objetos verificados.", "10:42", EventSeverity.SUCCESS),
                event("2", "Billetera verificada", "La identificación RFID previa se conserva.", "10:41", EventSeverity.INFO)
            )
        ),
        DemoScenario(
            snapshot = BagSnapshot(
                armed = true,
                bagOpen = true,
                keys = TrackedItem("Llaves", isPresent = true, isRfidVerified = true),
                wallet = TrackedItem("Billetera", isPresent = true, isRfidVerified = true),
                alarm = BagAlarm(AlarmType.OPENING_DETECTED, isLatched = true),
                updatedLabel = "Actualizado hace unos segundos"
            ),
            events = listOf(
                event("5", "Apertura detectada", "La alarma queda activa hasta desarmar.", "10:46", EventSeverity.CRITICAL),
                event("1", "Sistema armado", "Bolso cerrado y objetos verificados.", "10:42", EventSeverity.SUCCESS),
                event("3", "Llaves verificadas", "RFID y sensor ultrasónico confirmados.", "10:40", EventSeverity.INFO)
            )
        ),
        DemoScenario(
            snapshot = BagSnapshot(
                armed = false,
                bagOpen = true,
                keys = TrackedItem("Llaves", isPresent = true, isRfidVerified = true),
                wallet = TrackedItem("Billetera", isPresent = true, isRfidVerified = true),
                alarm = BagAlarm(AlarmType.NONE),
                updatedLabel = "Actualizado hace unos segundos"
            ),
            events = listOf(
                event("6", "Sistema desarmado", "La alarma fue reiniciada manualmente.", "10:47", EventSeverity.INFO),
                event("5", "Apertura detectada", "Evento registrado antes de desarmar.", "10:46", EventSeverity.CRITICAL),
                event("1", "Sistema armado", "Bolso cerrado y objetos verificados.", "10:42", EventSeverity.SUCCESS)
            )
        )
    )

    private var scenarioIndex = 0
    private val mutableBagStatus = MutableStateFlow(scenarios.first().snapshot)
    private val mutableEvents = MutableStateFlow(scenarios.first().events)
    private val mutableConnection = MutableStateFlow(
        ConnectionStatus(
            mode = ConnectionMode.DEMO,
            detail = "Estados locales de demostración"
        )
    )

    override val bagStatus: StateFlow<BagSnapshot> = mutableBagStatus.asStateFlow()
    override val events: StateFlow<List<BagEvent>> = mutableEvents.asStateFlow()
    override val connection: StateFlow<ConnectionStatus> = mutableConnection.asStateFlow()

    override fun advanceDemoScenario() {
        scenarioIndex = (scenarioIndex + 1) % scenarios.size
        val next = scenarios[scenarioIndex]
        mutableBagStatus.value = next.snapshot
        mutableEvents.value = next.events
    }

    fun advanceScenario() = advanceDemoScenario()

    override fun stop() = Unit

    private data class DemoScenario(
        val snapshot: BagSnapshot,
        val events: List<BagEvent>
    )

    private companion object {
        fun event(
            id: String,
            title: String,
            description: String,
            timeLabel: String,
            severity: EventSeverity
        ) = BagEvent(
            id = id,
            title = title,
            description = description,
            timeLabel = timeLabel,
            severity = severity
        )
    }
}
