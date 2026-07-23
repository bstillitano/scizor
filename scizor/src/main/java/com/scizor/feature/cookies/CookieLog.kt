package com.scizor.feature.cookies

/**
 * Host-facing API for recording cookies so they appear in Scizor's Cookie Browser.
 *
 * Android has no enumerable global cookie store, so beyond the cookies Scizor sees in
 * captured HTTP traffic, the host app can register its own — from its cookie jar, a
 * `Set-Cookie` header, or a WebView.
 *
 * Reach it via [com.scizor.Scizor.cookies].
 */
object CookieLog {

    /** Records a single cookie. */
    fun log(
        name: String,
        value: String,
        domain: String,
        path: String? = null,
        secure: Boolean = false,
        httpOnly: Boolean = false,
        sameSite: String? = null,
        expires: String? = null,
    ) {
        CookieBrowser.log(name, value, domain, path, secure, httpOnly, sameSite, expires)
    }

    /** Records every cookie the WebView holds for [url] (via `android.webkit.CookieManager`). */
    fun captureWebView(url: String) {
        CookieBrowser.captureWebView(url)
    }

    /** Clears the host-logged cookies (captured-traffic cookies are unaffected). */
    fun clear() {
        CookieBrowser.clearLogged()
    }
}
