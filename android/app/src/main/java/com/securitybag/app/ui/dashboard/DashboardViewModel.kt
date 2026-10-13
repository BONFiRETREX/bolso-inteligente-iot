package com.securitybag.app.ui.dashboard

import androidx.lifecycle.ViewModel
import com.securitybag.app.data.repository.BagRepositoryFactory
import com.securitybag.app.domain.model.BagEvent
import com.securitybag.app.domain.model.BagSnapshot
import com.securitybag.app.domain.model.ConnectionStatus
import com.securitybag.app.domain.repository.BagStatusRepository
import kotlinx.coroutines.flow.StateFlow

class DashboardViewModel : ViewModel() {
    private val repository: BagStatusRepository = BagRepositoryFactory.create()

    val bagStatus: StateFlow<BagSnapshot> = repository.bagStatus
    val events: StateFlow<List<BagEvent>> = repository.events
    val connection: StateFlow<ConnectionStatus> = repository.connection

    fun advanceDemoScenario() {
        repository.advanceDemoScenario()
    }

    override fun onCleared() {
        repository.stop()
        super.onCleared()
    }
}
