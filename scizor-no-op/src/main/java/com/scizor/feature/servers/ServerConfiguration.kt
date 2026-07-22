package com.scizor.feature.servers

/** No-op mirror of the real [ServerEnvironment]. */
data class ServerEnvironment(
    val name: String,
    val baseUrl: String,
    val variables: Map<String, String> = emptyMap(),
)

/**
 * No-op mirror of the real [ServerConfiguration]. Selection can't be changed in
 * release, so [selected] and [baseUrl] resolve to the first configured
 * environment (the production default).
 */
object ServerConfiguration {

    private var environments: List<ServerEnvironment> = emptyList()

    fun configure(environments: List<ServerEnvironment>) {
        this.environments = environments
    }

    fun all(): List<ServerEnvironment> = environments

    val selected: ServerEnvironment?
        get() = environments.firstOrNull()

    @Suppress("UNUSED_PARAMETER")
    fun select(environment: ServerEnvironment) = Unit

    fun baseUrl(): String = selected?.baseUrl.orEmpty()
}
