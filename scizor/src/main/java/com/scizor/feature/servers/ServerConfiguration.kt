package com.scizor.feature.servers

import com.scizor.Scizor

/** A named backend environment the app can point at. */
data class ServerEnvironment(
    val name: String,
    val baseUrl: String,
)

/**
 * Lets the host switch between backend environments at runtime. The selected
 * environment persists (by name) via the Scizor store under
 * `scizor_selected_server`, so the host app can read [baseUrl] on next launch.
 */
object ServerConfiguration {

    private var environments: List<ServerEnvironment> = emptyList()

    fun configure(environments: List<ServerEnvironment>) {
        this.environments = environments
    }

    fun all(): List<ServerEnvironment> = environments

    val selected: ServerEnvironment?
        get() {
            if (environments.isEmpty()) return null
            val storedName = Scizor.storeOrNull()?.string(STORE_KEY)
            return environments.firstOrNull { it.name == storedName } ?: environments.first()
        }

    fun select(environment: ServerEnvironment) {
        Scizor.storeOrNull()?.putString(STORE_KEY, environment.name)
    }

    fun baseUrl(): String = selected?.baseUrl.orEmpty()

    private const val STORE_KEY = "selected_server"
}
