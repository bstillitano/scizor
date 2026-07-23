package com.scizor.feature.cookies

/** No-op mirror of the real [CookieLog]. */
object CookieLog {

    @Suppress("UNUSED_PARAMETER")
    fun log(
        name: String,
        value: String,
        domain: String,
        path: String? = null,
        secure: Boolean = false,
        httpOnly: Boolean = false,
        sameSite: String? = null,
        expires: String? = null,
    ) = Unit

    @Suppress("UNUSED_PARAMETER")
    fun captureWebView(url: String) = Unit

    fun clear() = Unit
}
