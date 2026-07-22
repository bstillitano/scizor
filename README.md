<p align="center">
  <img width="200" height="200" src="Scizor.png">
</p>

# Scizor

![platform-badge](https://img.shields.io/badge/platform-Android-green)
![language-badge](https://img.shields.io/badge/kotlin-2.0-blue)
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
  - [Console Logger](#console-logger)
  - [Custom Developer Options](#custom-developer-options)
  - [Environment Variables](#environment-variables)
- [Menu Invocation](#menu-invocation)
- [Production Safety](#production-safety)
- [API Reference](#api-reference)
- [License](#license)

## Features

### Device & Application Info
- Device model, manufacturer, Android version, and API level
- Package name, app version, build number, and debuggable flag

### Networking
- **Network Logging** — an OkHttp interceptor that captures every request/response
- **Request Details** — headers, body, status, and timing
- **cURL Export** — copy any captured request as a runnable `curl` command
- **Server Configuration** — switch between development, staging, and production environments

### Data
- **Feature Flags** — register defaults in code, override at runtime from the menu
- **Preferences Browser** — view and edit `SharedPreferences` values

### Diagnostics
- **Console Logger** — live Logcat output, filterable by level and text

### Extensibility
- **Custom Developer Options** — add your own actions to the menu
- **Environment Variables** — surface any key/value pairs you want visible

## Requirements

- Android 7.0 (API 24)+
- Kotlin 2.0+
- Jetpack Compose

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
code required.

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

### Appearance Font Scale

The Appearance screen can force an app-wide font scale. To let it take effect, wrap
your activities' base context:

```kotlin
override fun attachBaseContext(base: Context) {
    super.attachBaseContext(Scizor.wrapAppearance(base))
}
```

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
| `Scizor.console` | Logcat capture |
| `Scizor.developerOptions` | Custom menu entries |
| `Scizor.environmentVariables` | Read-only key/value display |
| `Scizor.interfacePreviews` | Host Composables to preview (name + optional description) |
| `Scizor.deepLinkPresets` | One-tap deep links for the tester |
| `Scizor.wrapAppearance(base)` | Apply the appearance font-scale override in `attachBaseContext` |

## License

See [LICENSE](LICENSE).
