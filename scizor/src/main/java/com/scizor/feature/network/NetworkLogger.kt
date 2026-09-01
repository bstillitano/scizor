package com.scizor.feature.network

import io.ktor.client.plugins.api.ClientPlugin
import okhttp3.Interceptor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * Captures HTTP traffic recorded by [ScizorInterceptor] into a bounded,
 * newest-first buffer surfaced as a [StateFlow] for the UI.
 *
 * Add the interceptor to your client:
 * ```
 * OkHttpClient.Builder().addInterceptor(Scizor.network.interceptor()).build()
 * ```
 *
 * Ktor clients on a non-OkHttp engine install [ktorPlugin] instead:
 * ```
 * HttpClient(CIO) { install(Scizor.network.ktorPlugin()) }
 * ```
 */
object NetworkLogger {

    private const val MAX_TRANSACTIONS = 1000

    private val idCounter = AtomicLong(0)
    private val buffer = ArrayDeque<NetworkTransaction>(MAX_TRANSACTIONS)

    private val _transactions = MutableStateFlow<List<NetworkTransaction>>(emptyList())
    val transactions: StateFlow<List<NetworkTransaction>> = _transactions.asStateFlow()

    fun interceptor(): Interceptor = ScizorInterceptor()

    /**
     * A Ktor client plugin that records into this logger, for hosts running Ktor on a
     * non-OkHttp engine (CIO, Android, Java). Ktor on the OkHttp engine needs nothing
     * new — pass [interceptor] to the engine instead.
     *
     * `ktor-client-core` is a `compileOnly` dependency of Scizor, so this method is
     * only callable by apps that already have Ktor on their classpath.
     */
    fun ktorPlugin(): ClientPlugin<Unit> = scizorKtorPlugin()

    fun clear() {
        synchronized(buffer) {
            buffer.clear()
            _transactions.value = emptyList()
        }
    }

    fun find(id: Long): NetworkTransaction? =
        synchronized(buffer) { buffer.firstOrNull { it.id == id } }

    internal fun nextId(): Long = idCounter.incrementAndGet()

    internal fun record(transaction: NetworkTransaction) {
        synchronized(buffer) {
            buffer.addLast(transaction)
            while (buffer.size > MAX_TRANSACTIONS) {
                buffer.removeFirst()
            }
            // Newest first for display.
            _transactions.value = buffer.toList().asReversed()
        }
    }
}
