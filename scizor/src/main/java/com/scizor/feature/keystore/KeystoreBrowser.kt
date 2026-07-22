package com.scizor.feature.keystore

import java.security.KeyStore

/** An entry in the Android Keystore. */
internal data class KeystoreEntry(
    val alias: String,
    val type: String,
    val created: Long,
)

/**
 * Read-only view of the AndroidKeyStore aliases. Key material can never be
 * extracted, so this lists alias, entry type, and creation date only.
 */
internal object KeystoreBrowser {

    fun entries(): List<KeystoreEntry> = runCatching {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.aliases().toList().map { alias ->
            val type = when {
                keyStore.isKeyEntry(alias) -> "Key"
                keyStore.isCertificateEntry(alias) -> "Certificate"
                else -> "Unknown"
            }
            KeystoreEntry(
                alias = alias,
                type = type,
                created = keyStore.getCreationDate(alias)?.time ?: 0L,
            )
        }.sortedBy { it.alias }
    }.getOrDefault(emptyList())
}
