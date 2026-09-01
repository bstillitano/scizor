<a href="https://github.com/bstillitano/Scyther"><img src=".github/scyther-banner.svg" alt="Looking for the iOS version? Get Scyther for iOS" width="100%" /></a>

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
  - [Migrating from v0.1.0](#migrating-from-v010)
- [Quick Start](#quick-start)
- [Usage](#usage)
  - [Network Logging](#network-logging)
  - [Ktor](#ktor)
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
- [Disabled Features](#disabled-features)
- [Permissions](#permissions)
- [Menu Invocation](#menu-invocation)
- [Production Safety](#production-safety)
  - [Shipping Scizor in a release build](#shipping-scizor-in-a-release-build)
  - [What Scizor adds to your manifest](#what-scizor-adds-to-your-manifest)
- [API Reference](#api-reference)
- [License](#license)

## Features

The debug menu mirrors the iOS Scyther layout, grouped into sections.

### Device & Application
- Device OS version, API level, manufacturer, model, hardware, and device ID
- App name, package, version, build number, and install date

### Networking
- **Network Logger** — an OkHttp interceptor (and a Ktor client plugin) that captures every
  request/response, with headers, body, status, and timing. Pretty-prints JSON and XML,
  renders image responses inline, decodes GraphQL operations (including batched requests),
  and exports any request as a runnable `curl` command
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

Scizor's menu is built on Material 3 **Expressive**, whose `SegmentedListItem` gives the
grouped rows their look. Those components live only in `material3` 1.5.0-alpha, so Scizor
depends on a prerelease Material 3 and your app will resolve it too — see the note below
before adopting. Your app must build against:

| Tool | Version |
|---|---|
| Kotlin | 2.2+ |
| Android Gradle Plugin | 9.1+ |
| Gradle | 9.3+ |
| `compileSdk` | 37 |
| Jetpack Compose | 1.12.0 (stable) |
| Compose Material 3 | 1.5.0-alpha27 (prerelease) |
| JDK | 17 |

The menu renders on every device down to `minSdk` 24 — this is a **build-time** requirement,
not a runtime one.

`compileSdk` 37, AGP 9.1 and Gradle 9.3 are what this repository builds with, and an AAR
published from a newer `compileSdk` cannot be consumed by an older one. If you need Scizor on
an older toolchain, that is a change to this repository's build configuration.

**The Material 3 row is different, and worth understanding before you adopt.** Scizor declares
`material3` as an `implementation` dependency, which lands in its published POM at `runtime`
scope. Gradle resolves version conflicts by taking the **highest** version in the graph, so a
consumer on stable Material 3 who adds Scizor gets `1.5.0-alpha27` across their whole debug
variant — a different Material 3 in debug than in release. You cannot pin it back down either:
forcing `1.4.0` makes Scizor's compiled calls to `SegmentedListItem` fail at runtime with
`NoSuchMethodError`.

If that trade is not one you want, `v0.1.0` builds against stable Material 3 1.4.0 and is
unaffected. It reproduces the segmented look on the stable `ListItem` instead of using the
Expressive component. When `material3` 1.5.0 ships stable this distinction disappears.

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

Then add the toolkit:

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.github.bstillitano.scizor:scizor:v0.2.0")
}
```

That is the recommended wiring, and it is safe by default: `Scizor.start()` **refuses to run
in a non-debuggable build** unless you explicitly opt in. Your code calls Scizor from ordinary
`main` source, it compiles in every variant, and the toolkit stays inert in release. See
[Shipping Scizor in a release build](#shipping-scizor-in-a-release-build) for the opt-in and
for trimming what ships.

The trade is that the artifact is present in your release APK — about 1 MB, plus
[Scizor's manifest entries](#what-scizor-adds-to-your-manifest), which include the
`SYSTEM_ALERT_WINDOW` permission that appears in a Play Store listing. Both are removable.

### Keeping it out of release builds entirely

If you want nothing at all in your shipped app, use `debugImplementation` instead:

```kotlin
dependencies {
    debugImplementation("com.github.bstillitano.scizor:scizor:v0.2.0")
}
```

Now the artifact is not on the release classpath, so the debugging UI, the Logcat reader, the
network buffers and the manifest entries cannot be in your shipped app at all.

The cost is that the code which *calls* Scizor has to be debug-only too, or your release build
will not compile. The usual shape is a small initialiser with a release stub:

```kotlin
// src/debug/java/com/example/DebugTools.kt
fun Application.initDebugTools() {
    Scizor.start(this)
    Scizor.featureFlags.register(FeatureFlag("new_checkout", "New checkout", defaultValue = false))
}
```

```kotlin
// src/release/java/com/example/DebugTools.kt
fun Application.initDebugTools() = Unit
```

```kotlin
// src/main/java/com/example/MyApp.kt — compiles in both variants
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initDebugTools()
    }
}
```

### Migrating from v0.1.0

**Breaking change: the `scizor-no-op` artifact no longer exists.** Delete its line, or your
release build will fail to resolve it:

```diff
 dependencies {
     debugImplementation("com.github.bstillitano.scizor:scizor:v0.1.0")
-    releaseImplementation("com.github.bstillitano.scizor:scizor-no-op:v0.1.0")
 }
```

What replaces it depends on why you had it:

- **You wanted your Scizor calls to keep compiling in release** — which is what the no-op was
  for. Change `debugImplementation` to `implementation`. The real artifact is then on both
  classpaths, and [`Scizor.start()` refuses to run](#shipping-scizor-in-a-release-build) in a
  non-debuggable build, so the toolkit stays inert unless you pass `allowProductionBuilds = true`.
  This is the closest equivalent to what you had.
- **You wanted Scizor out of release builds entirely.** Keep `debugImplementation`. If your
  release build now fails to *compile* because `main` source references `Scizor`, move those
  calls into a debug-only source set as shown above.

The no-op was a hand-mirrored copy of every public symbol, policed by a compile-time parity
file and published as a second artifact. The `start()` gate covers the case it existed for at
a fraction of the cost, so it is gone.

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

With the default `debugImplementation` wiring these calls belong in a debug-only source set —
see [Installation](#installation).

## Usage

### Network Logging

```kotlin
OkHttpClient.Builder()
    .addInterceptor(Scizor.network.interceptor())
    .build()
```

Every request is captured into the **Network Logger** screen. Tap a transaction to see its
headers, body, status, and timing, and to copy it as a `curl` command.

Retrofit needs nothing extra — it sits on OkHttp, so the interceptor above already covers it.

### Ktor

There are two routes, and which one you need depends only on your engine.

**On the OkHttp engine — nothing new to add.** The engine exposes the underlying OkHttp
client, so the interceptor you already have is all it takes. No extra dependency, no plugin:

```kotlin
HttpClient(OkHttp) {
    engine { addInterceptor(Scizor.network.interceptor()) }
}
```

**On any other engine (CIO, Android, Java)** there is no OkHttp client to hook, so install
Scizor's Ktor plugin instead:

```kotlin
HttpClient(CIO) {
    install(Scizor.network.ktorPlugin())
}
```

It records the same fields as the interceptor — method, URL, headers, request and response
bodies, status, duration, content type, GraphQL operation details — into the same Network
Logger screen. The response body is captured through Ktor's `ResponseObserver`, which tees
the response channel, so your code still receives a completely unread stream; capture never
alters or fails the call it observes.

`ktor-client-core` is a `compileOnly` dependency of Scizor, so Scizor never puts Ktor on your
classpath and apps that don't use Ktor pay nothing for it. `ktorPlugin()` requires Ktor 3.x
to already be a dependency of your app — which it is, if you are calling it.

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

`overridesEnabled` defaults to `false`, matching Scyther. While it's off, every flag follows
its registered default and the Feature Flags screen says so, rather than showing toggles that
do nothing — a tester turns it on once, from the screen or via `Scizor.featureFlags.overridesEnabled
= true`. For hosts that keep their own feature-flag manager and want Scizor to own only the
flags UI, set `Scizor.featureFlags.onOverrideChanged`: it's invoked after any override changes,
with the affected key (`null` for a reset-all or an `overridesEnabled` toggle), so the host can
react to a flip instead of only reading `isEnabled`/`overrideState` on demand.

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

`DeveloperOption` is a sealed hierarchy with one subtype per row shape. Every subtype
takes an optional `subtitle` and `icon` (a `ScizorIcon.Vector` or `ScizorIcon.Resource`).

```kotlin
Scizor.developerOptions = listOf(
    // A runnable action. Set `dismissOnClick = true` for actions that navigate into
    // the host app (a deep link, a screen, a permission prompt) so the menu closes
    // itself first — shorthand for calling `Scizor.dismiss()` as the first line of
    // `onClick`.
    DeveloperOption.Action(title = "Reset onboarding") { resetOnboarding() },
    DeveloperOption.Action(title = "Open profile", dismissOnClick = true) { openProfile() },

    // A read-only label/value pair.
    DeveloperOption.Value(title = "Build", value = BuildConfig.VERSION_NAME),

    // Pushes a Composable onto the menu's navigation stack.
    DeveloperOption.Screen(title = "Sandbox") { SandboxScreen() },

    // An on/off switch backed by the host's own store. `checked` is a lambda, not a
    // `Boolean`, so the host's store stays the source of truth — the row re-reads it
    // on every recomposition instead of snapshotting whatever was true when it was
    // registered. `checked` must be a stable, cheap, side-effect-free read: it is
    // called on every composition, so back-to-back calls must return the same value —
    // never derive it from a clock, `Random`, or anything else that changes per call,
    // or the row will keep recomposing itself. `onCheckedChange` is assumed synchronous
    // with `checked`: the row writes through and immediately re-reads `checked` to show
    // what was actually stored, so a host that persists asynchronously will briefly
    // show the old value.
    DeveloperOption.Toggle(
        title = "Bypass PIN rules",
        checked = { MyDebugStore.bypassPinRules },
        onCheckedChange = { MyDebugStore.bypassPinRules = it },
    ),
)
```

Call `Scizor.dismiss()` directly to close the menu from anywhere, or set `dismissOnClick` on an
`Action` for the common case above.

Give every developer option a **unique title**. A row's identity is derived from its title, so
two options sharing one collide — most visibly with `Toggle`, where the duplicate row ends up
showing the other one's state.

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

## Disabled Features

`Scizor.disabledFeatures` hides built-in menu entries by id. The default (all built-ins
enabled) is right for a debug-only dependency; it stops being right for a host shipping the
real `scizor` artifact in a signed QA build that leaves the building — a Logcat reader, an
editable Preferences Browser, and a Keystore Browser are a different risk conversation there.
Set it once, before or after `Scizor.start()`:

```kotlin
Scizor.disabledFeatures = setOf("keystore", "console", "preferences")
```

| Id | Feature |
|---|---|
| `network` | Network Logger |
| `servers` | Server Configuration |
| `environment_variables` | Environment Variables |
| `feature_flags` | Feature Flags |
| `preferences` | Preferences Browser |
| `cookies` | Cookie Browser |
| `file_browser` | File Browser |
| `database_browser` | Database Browser |
| `keystore` | Keystore Browser |
| `location` | Location Spoofer |
| `console` | Console Logger |
| `deep_link` | Deep Link Tester |
| `crash_logs` | Crash Logs |
| `notification_logger` | Notification Logger |
| `notification_tester` | Notification Tester |
| `fonts` | Fonts |
| `interface_previews` | Interface Previews |
| `grid_overlay` | Grid Overlay |
| `fps_counter` | FPS Counter |
| `touch_visualiser` | Touch Visualiser |
| `appearance` | Appearance |

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

With the default `debugImplementation` wiring all of this is debug-only and never reaches
your release build. If you ship the real artifact in release, see
[What Scizor adds to your manifest](#what-scizor-adds-to-your-manifest).

## Menu Invocation

By default, shaking the device opens the menu. Configure it:

```kotlin
Scizor.invocationGesture = ScizorGesture.SHAKE          // default
Scizor.invocationGesture = ScizorGesture.FLOATING_BUTTON
Scizor.invocationGesture = ScizorGesture.NONE           // open manually via Scizor.show()
```

## Production Safety

Wiring Scizor with `debugImplementation` is the strongest guarantee there is: the artifact is
not on the release classpath, so the debugging UI, the Logcat reader, the network buffers and
Scizor's manifest entries are not in your shipped app at all. That is the default, and for
most apps it is the end of the subject.

Scizor used to ship a second artifact, `scizor-no-op`, so that release builds could still
compile against the API. It has been removed — see
[Migrating from v0.1.0](#migrating-from-v010).

### Shipping Scizor in a release build

Sometimes the real toolkit in a non-debug build is the point: a signed QA build that leaves
the building, or a production app that unlocks the menu behind a hidden gesture. Depend on it
unconditionally:

```kotlin
dependencies {
    implementation("com.github.bstillitano.scizor:scizor:v0.2.0")
}
```

`Scizor.start()` will still refuse to run. It starts only when the app is **debuggable** —
true for debug builds and for internal-distribution builds deliberately signed as debuggable,
false for anything shipped through the Play Store. When it refuses, nothing is captured,
installed or observed: no Logcat capture, no crash handler, no overlays, no shake detector.
It writes a warning to Logcat saying why, so a build where "nothing happens" is never a
mystery.

Opt in deliberately:

```kotlin
Scizor.start(this, allowProductionBuilds = true)
Scizor.disabledFeatures = setOf("keystore", "console", "preferences")
```

[`Scizor.disabledFeatures`](#disabled-features) exists for exactly this case — a Logcat
reader, an editable Preferences Browser and a Keystore Browser are a different risk
conversation in a build that leaves the building. This mirrors Scyther's
`Scyther.start(allowProductionBuilds:)` on iOS.

### What Scizor adds to your manifest

Scizor's library manifest is merged into your app's, so shipping the real artifact means
shipping these. With one artifact this is **opt-out rather than absent by default**:

| Entry | Why it is there | What a user or reviewer sees |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` permission | the grid, FPS and touch overlays draw over the whole screen | "Display over other apps", including a line in your Play Store listing's permission list |
| `INTERNET`, `ACCESS_NETWORK_STATE` | OpenStreetMap tiles for the Location Spoofer map | nothing (almost every app declares these) |
| `ScizorNotificationListenerService` | feeds the Notification Logger | an entry under Settings → Apps → Special app access → Notification access |
| `ScizorActivity` | hosts the menu | nothing — not exported, no launcher entry |
| `ScizorFileProvider` | shares files out of the File Browser | nothing |

The first and third are the user-visible ones. Drop them with a manifest merger override,
scoped to the release source set so your debug builds keep them:

```xml
<!-- app/src/release/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission
        android:name="android.permission.SYSTEM_ALERT_WINDOW"
        tools:node="remove" />
    <service
        android:name="com.scizor.feature.notifications.ScizorNotificationListenerService"
        tools:node="remove" />

</manifest>
```

Removing the service disables the **Notification Logger** — there is nothing left to receive
the notifications. Removing the permission disables the **Grid Overlay**, **FPS Counter** and
**Touch Visualiser**, which have no window to draw into without it. Hide them from the menu
too, so they are not offered and then broken:

```kotlin
Scizor.disabledFeatures = setOf(
    "notification_logger", "grid_overlay", "fps_counter", "touch_visualiser",
)
```

None of this applies to the default `debugImplementation` wiring — the library manifest is
never merged into a release build in the first place.

### Where Scizor's own settings live

Scizor persists its settings — feature flag overrides, the selected server, menu pins, the
spoofed location — to **device-protected storage** (`/data/user_de/0/<pkg>/files`) rather than
the credential-encrypted `filesDir` your app uses. Apps routinely wipe their own storage on
sign-out, with `context.filesDir.deleteRecursively()` or
`prefs.edit().clear().apply()`; neither reaches device-protected storage, so a QA build's
overrides survive signing out and back in. This mirrors Scyther, which keeps its state in a
named `UserDefaults` suite for the same reason — a persistent domain separate from the one the
host app clears.

A full container wipe still removes it: `ActivityManager.clearApplicationUserData()`, or the
user choosing "Clear storage" in system settings. That is a deliberate reset rather than an
accidental one, and Scyther has the same limit.

## API Reference

| Symbol | Purpose |
|---|---|
| `Scizor.start(app, allowProductionBuilds)` | Initialise the toolkit; refuses in a non-debuggable build unless overridden |
| `Scizor.show()` | Open the menu manually |
| `Scizor.dismiss()` | Close the menu if it is open; no-op otherwise |
| `Scizor.invocationGesture` | `SHAKE` / `FLOATING_BUTTON` / `NONE` |
| `Scizor.network.interceptor()` | OkHttp interceptor for logging |
| `Scizor.network.ktorPlugin()` | Ktor client plugin for logging (non-OkHttp engines) |
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
| `Scizor.disabledFeatures` | Ids of built-in features to hide from the menu |
| `Scizor.wrapAppearance(base)` | Apply the appearance font-scale override in `attachBaseContext` |

## License

See [LICENSE](LICENSE).
