package com.scizor.feature.keystore

import java.security.KeyStore
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.security.cert.X509Certificate

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

    /** Best-effort attributes for an alias (certificate + public-key details). */
    fun details(alias: String): List<Pair<String, String>> = runCatching {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        buildList {
            add("Alias" to alias)
            keyStore.getCreationDate(alias)?.let { add("Created" to it.toString()) }
            val cert = keyStore.getCertificate(alias) as? X509Certificate
            if (cert != null) {
                add("Subject" to cert.subjectDN.name)
                add("Issuer" to cert.issuerDN.name)
                add("Valid from" to cert.notBefore.toString())
                add("Valid until" to cert.notAfter.toString())
                add("Signature alg" to cert.sigAlgName)
                val key = cert.publicKey
                add("Key algorithm" to key.algorithm)
                keySize(key)?.let { add("Key size" to "$it bits") }
            }
        }
    }.getOrDefault(listOf("Alias" to alias))

    private fun keySize(key: java.security.PublicKey): Int? = when (key) {
        is RSAPublicKey -> key.modulus.bitLength()
        is ECPublicKey -> key.params.curve.field.fieldSize
        else -> null
    }
}
