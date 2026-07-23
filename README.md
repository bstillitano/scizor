<table width="100%">
<tr><td align="center">

**📱 Looking for the iOS version?**

Scizor is the Android port of **Scyther** — the original iOS debugging toolkit.

<a href="https://github.com/bstillitano/Scyther"><img src="https://img.shields.io/badge/Get%20Scyther%20for%20iOS-000000?style=for-the-badge&logo=apple&logoColor=white" alt="Scyther for iOS" /></a>

</td></tr>
</table>

<p align="center">
  <img width="200" height="200" src="Scizor.png">
</p>

# Scizor

![platform-badge](https://img.shields.io/badge/platform-Android-green)
![language-badge](https://img.shields.io/badge/kotlin-2.2-blue)
![ui-badge](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)

A comprehensive Android debugging toolkit that helps you cut through bugs in your Android app.
Scizor gives developers, QA testers, and backend engineers an in-app debug menu — one shake
away — for inspecting network traffic, flipping feature flags, switching environments, browsing
preferences, and reading logs. It is the Android counterpart to the iOS
[Scyther](https://github.com/bstillitano/Scyther) library.

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Usage](#usage)
  - [Network Logging](#network-logging)
  - [Feature Flags](#feature-flags)
  - [Server Configuration](#server-configuration)
  - [Preferences Browser](#preferences-browser)
  - [Cookie Logging](#cookie-logging)
  - [Console Logger](#console-logger)
  - [Custom Developer Options](#custom-developer-options)
  - [Environment Variables](#environment-variables)
  - [Interface Previews](#interface-previews)
  - [Deep Link Presets](#deep-link-presets)
  - [Custom Databases](#custom-databases)
  - [Appearance Font Scale](#appearance-font-scale)
- [Permissions](#permissions)
- [Menu Invocation](#menu-invocation)
- [Production Safety](#production-safety)
- [API Reference](#api-reference)
- [License](#license)

## Features

The debug menu mirrors the iOS Scyther layout, grouped into sections.

### Device & Application
- Device OS version, API level, manufacturer, model, hardware, and device ID
- App name, package, version, build number, and install date

### Networking
- **Network Logger** — an OkHttp interceptor that captures every request/response, with
  headers, body, status, and timing. Pretty-prints JSON and XML, renders image responses
  inline, decodes GraphQL operations (including batched requests), and exports any request
  as a runnable `curl` command
- **Server Configuration** — switch between environments (e.g. development, staging,
  production), each with its own base URL and variables
- **Environment Variables** — surface any key/value pairs you want visible
- **IP Address** — the device's current public IP

### Data
- **Feature Flags** — register defaults in code, override (On / Off / Remote) at runtime,
  pin the ones you use most
- **Preferences Browser** — view and edit `SharedPreferences` (including editable string
  sets); also exposes Scizor's own settings, read-only
- **Cookie Browser** — cookies seen in captured traffic, logged by the host, or read from a
  WebView; delete individually or clear all
- **File Browser** — browse the app sandbox, preview images/text, and share or open any file
- **Database Browser** — browse SQLite/Room databases: tables, schema, indexes, a raw SQL
  editor, and typed record add/edit/delete (with NULL, integer/real, and base64 BLOB support).
  Register custom, non-SQLite sources via an adapter

### Security
- **Keystore Browser** — inspect AndroidKeyStore aliases and certificate details; delete
  entries or clear the store

### System Tools
- **Location Spoofer** — mock GPS to a preset city, a custom coordinate, or a moving route,
  with a live OpenStreetMap map
- **Console Logger** — live Logcat output, filterable by level and text
- **Deep Link Tester** — fire URLs/schemes from presets, history, or a QR scan
- **Crash Logs** — captured uncaught exceptions with a searchable stack trace, copy, and share

### Notifications
- **Notification Logger** — logs notifications posted on the device (via notification access)
- **Notification Tester** — compose and post/schedule local test notifications
- **FCM Token** — the current Firebase Cloud Messaging token, if supplied

### UI/UX
- **Fonts** — browse and preview app and system fonts
- **Interface Previews** — render host-registered Composables live in the menu
- **Grid Overlay**, **FPS Counter**, **Touch Visualiser** — system overlays that draw over the
  whole screen
- **Appearance** — force light/dark theme, an app-wide font scale, and a high-contrast flag

## Requirements

- **Runtime:** Android 7.0 (API 24)+
- **UI:** Jetpack Compose

### Build-time toolchain

Scizor's menu is built with Material 3 Expressive (the real `SegmentedListItem`), which
currently pulls a recent toolchain. Your app must build against:

| Tool | Version |
|---|---|
| Kotlin | 2.2+ |
| Android Gradle Plugin | 9.1+ |
| Gradle | 9.3+ |
| `compileSdk` | 37 |
| Jetpack Compose | 1.12.0-beta02 |
| Compose Material 3 | 1.5.0-alpha24 |
| JDK | 17 |

The menu renders on every device down to `minSdk` 24 — this is a **build-time** requirement,
not a runtime one.

## Installation

Scizor is distributed via [JitPack](https://jitpack.io). Add the repository:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Then depend on the full toolkit for debug builds and the no-op artifact for release builds:

```kotlin
// app/build.gradle.kts
dependencies {
    debugImplementation("com.github.bstillitano.scizor:scizor:<tag>")
    releaseImplementation("com.github.bstillitano.scizor:scizor-no-op:<tag>")
}
```

The `scizor-no-op` artifact exposes the identical public API as no-ops, so your code compiles
and runs unchanged in release with zero debugging overhead.

## Quick Start

Initialise Scizor once, in your `Application`:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Scizor.start(this)
    }
}
```

Add the network interceptor to your OkHttp client:

```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(Scizor.network.interceptor())
    .build()
```

Now **shake the device** (or call `Scizor.show()`) to open the debug menu.

## Usage

### Network Logging

```kotlin
OkHttpClient.Builder()
    .addInterceptor(Scizor.network.interceptor())
    .build()
```

Every request is captured into the **Network Logger** screen. Tap a transaction to see its
headers, body, status, and timing, and to copy it as a `curl` command.

### Feature Flags

```kotlin
Scizor.featureFlags.register(
    FeatureFlag(key = "new_checkout", title = "New checkout flow", defaultValue = false),
)

if (Scizor.featureFlags.isEnabled("new_checkout")) {
    // ...
}
```

Toggle flags from the **Feature Flags** screen. Overrides persist across launches; "Reset to
default" clears them. In release builds, `isEnabled` returns the registered default.

### Server Configuration

```kotlin
Scizor.servers.configure(
    listOf(
        ServerEnvironment("Development", "https://dev.api.example.com"),
        ServerEnvironment("Staging", "https://staging.api.example.com"),
        ServerEnvironment("Production", "https://api.example.com"),
    ),
)

val baseUrl = Scizor.servers.baseUrl()
```

Pick the active environment from the **Server Configuration** screen; the selection persists.

### Preferences Browser

Open the **Preferences Browser** screen to inspect and edit any `SharedPreferences` file — no
code required. String sets are editable, and Scizor's own settings appear as a read-only store.

### Cookie Logging

Cookies from traffic captured by the network interceptor appear automatically. To surface
cookies from another source (a native stack, a WebView), feed them to the browser:

```kotlin
Scizor.cookies.log(name = "session", value = "abc123", domain = "example.com", secure = true)
Scizor.cookies.captureWebView("https://example.com")   // reads the WebView cookie store
```

### Console Logger

The **Console Logger** screen streams live Logcat output scoped to your app, with level and
text filters.

### Custom Developer Options

```kotlin
Scizor.developerOptions = listOf(
    DeveloperOption(title = "Reset onboarding") { resetOnboarding() },
)
```

### Environment Variables

```kotlin
Scizor.environmentVariables = mapOf(
    "BUILD_TYPE" to BuildConfig.BUILD_TYPE,
    "API_BASE_URL" to Scizor.servers.baseUrl(),
)
```

### Interface Previews

Register Composables to inspect them live in the menu. Each may carry an optional description:

```kotlin
Scizor.interfacePreviews = listOf(
    InterfacePreview("Primary button", "The app's main call-to-action") {
        Button(onClick = {}) { Text("Click me") }
    },
)
```

### Deep Link Presets

One-tap deep links shown in the Deep Link Tester:

```kotlin
Scizor.deepLinkPresets = listOf(
    DeepLinkPreset("Home", "myapp://home"),
    DeepLinkPreset("Profile", "myapp://user/42"),
)
```

The tester's QR scanner appears automatically when the optional
`com.google.android.gms:play-services-code-scanner` dependency is on the classpath
(add it via `debugImplementation`).

### Custom Databases

The Database Browser lists the app's SQLite files automatically. To browse a
non-SQLite store (Realm, an in-memory cache, a remote snapshot) alongside them,
register a read-only adapter:

```kotlin
Scizor.databaseAdapters = listOf(
    object : ScizorDatabaseAdapter {
        override val name = "Realm"
        override val tables = listOf("User", "Session")
        override fun columns(table: String) = listOf("id", "name")
        override fun count(table: String) = realm.count(table)
        override fun rows(table: String, limit: Int, offset: Int) = realm.page(table, limit, offset)
    },
)
```

Adapter-backed databases appear under **Custom databases** and are read-only.

### Appearance Font Scale

The Appearance screen can force an app-wide font scale. To let it take effect, wrap
your activities' base context:

```kotlin
override fun attachBaseContext(base: Context) {
    super.attachBaseContext(Scizor.wrapAppearance(base))
}
```

## Permissions

Scizor requests everything it needs at runtime, from within the menu — nothing is required in
your app's manifest. A few tools rely on a permission or system setting the user grants on
first use:

- **Grid / FPS / Touch overlays** — "Display over other apps" (`SYSTEM_ALERT_WINDOW`), so the
  overlay can draw over the whole screen
- **Notification Logger** — notification access, granted from the screen's settings shortcut
- **Location Spoofer** — the app must be selected as the device's mock-location app in
  Developer options
- **Notification Tester** — POST_NOTIFICATIONS on Android 13+

All of these are debug-only; the `scizor-no-op` release artifact declares none of them.

## Menu Invocation

By default, shaking the device opens the menu. Configure it:

```kotlin
Scizor.invocationGesture = ScizorGesture.SHAKE          // default
Scizor.invocationGesture = ScizorGesture.FLOATING_BUTTON
Scizor.invocationGesture = ScizorGesture.NONE           // open manually via Scizor.show()
```

## Production Safety

Depending on `scizor-no-op` in release builds guarantees the debugging UI, Logcat reader, and
network buffers are never included in your shipped app. Feature flags and server configuration
still resolve to their registered defaults, so any host logic that reads them keeps working.

## API Reference

| Symbol | Purpose |
|---|---|
| `Scizor.start(app)` | Initialise the toolkit |
| `Scizor.show()` | Open the menu manually |
| `Scizor.invocationGesture` | `SHAKE` / `FLOATING_BUTTON` / `NONE` |
| `Scizor.network.interceptor()` | OkHttp interceptor for logging |
| `Scizor.featureFlags` | `register`, `isEnabled`, `override` |
| `Scizor.servers` | `configure`, `select`, `baseUrl` |
| `Scizor.preferences` | Read/edit `SharedPreferences` |
| `Scizor.cookies` | `log(...)`, `captureWebView(url)` — feed cookies to the browser |
| `Scizor.console` | Logcat capture |
| `Scizor.developerOptions` | Custom menu entries |
| `Scizor.environmentVariables` | Read-only key/value display |
| `Scizor.fcmToken` | FCM token shown in the Notifications section |
| `Scizor.interfacePreviews` | Host Composables to preview (name + optional description) |
| `Scizor.deepLinkPresets` | One-tap deep links for the tester |
| `Scizor.databaseAdapters` | Read-only custom database sources for the browser |
| `Scizor.wrapAppearance(base)` | Apply the appearance font-scale override in `attachBaseContext` |

## License

See [LICENSE](LICENSE).
