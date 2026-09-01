package com.scizor.core

import androidx.core.content.FileProvider

/**
 * Scizor's own [FileProvider] subclass.
 *
 * The library manifest previously declared `androidx.core.content.FileProvider`
 * directly. That works, but it means Scizor and the host app declare a provider of
 * the same class, which makes manifest merging harder to reason about and turns any
 * future change on either side into a merge question. Declaring our own subclass
 * removes the ambiguity — the two providers are now distinct classes with distinct
 * authorities, and neither can be mistaken for the other.
 *
 * The authority is unchanged (`${applicationId}.scizor.fileprovider`), so URIs
 * already granted to external viewers keep working.
 *
 * The paths come from the `android.support.FILE_PROVIDER_PATHS` meta-data on the
 * manifest declaration, which stays the single source of truth for them.
 */
internal class ScizorFileProvider : FileProvider()
