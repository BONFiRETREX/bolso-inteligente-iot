package com.securitybag.app.data.repository

import com.securitybag.app.data.fake.FakeBagStatusRepository
import com.securitybag.app.data.supabase.SupabaseBagStatusRepository
import com.securitybag.app.data.supabase.SupabaseConfiguration
import com.securitybag.app.domain.repository.BagStatusRepository

/**
 * Selecciona la fuente real únicamente cuando la compilación contiene la
 * configuración pública necesaria. Sin ella, la app conserva un modo demo
 * explícito y no simula una conexión inexistente.
 */
object BagRepositoryFactory {
    fun create(): BagStatusRepository =
        if (SupabaseConfiguration.isConfigured) {
            SupabaseBagStatusRepository()
        } else {
            FakeBagStatusRepository()
        }
}
