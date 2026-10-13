package com.securitybag.app.data.supabase

import com.securitybag.app.BuildConfig

object SupabaseConfiguration {
    val url: String
        get() = BuildConfig.SUPABASE_URL.trim().removeSuffix("/")

    val publishableKey: String
        get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()

    val bagId: String
        get() = BuildConfig.SUPABASE_BAG_ID.trim().ifBlank { DEFAULT_BAG_ID }

    val isConfigured: Boolean
        get() = url.startsWith("https://") && publishableKey.isNotBlank()

    const val DEFAULT_BAG_ID = "bolso-demo-001"
}
