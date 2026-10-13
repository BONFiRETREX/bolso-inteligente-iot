package com.securitybag.app.domain.repository

import com.securitybag.app.domain.model.BagEvent
import com.securitybag.app.domain.model.BagSnapshot
import com.securitybag.app.domain.model.ConnectionStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato estable para la interfaz. En la siguiente etapa, la implementación
 * de Supabase Realtime sustituirá a la fuente simulada sin cambiar los composables.
 */
interface BagStatusRepository {
    val bagStatus: StateFlow<BagSnapshot>
    val events: StateFlow<List<BagEvent>>
    val connection: StateFlow<ConnectionStatus>

    fun advanceDemoScenario()

    fun stop()
}
