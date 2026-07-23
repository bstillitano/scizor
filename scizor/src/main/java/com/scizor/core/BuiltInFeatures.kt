package com.scizor.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import com.scizor.feature.appearance.AppearanceScreen
import com.scizor.feature.console.ConsoleScreen
import com.scizor.feature.cookies.CookiesScreen
import com.scizor.feature.crashlogs.CrashLogsScreen
import com.scizor.feature.custom.EnvironmentVariablesScreen
import com.scizor.feature.databasebrowser.DatabaseBrowserScreen
import com.scizor.feature.deeplink.DeepLinkTesterScreen
import com.scizor.feature.featureflags.FeatureFlagsScreen
import com.scizor.feature.filebrowser.FileBrowserScreen
import com.scizor.feature.fonts.FontsScreen
import com.scizor.feature.interfacepreviews.InterfacePreviewsScreen
import com.scizor.feature.interfacetools.FpsCounterScreen
import com.scizor.feature.interfacetools.GridOverlayScreen
import com.scizor.feature.interfacetools.TouchVisualiserScreen
import com.scizor.feature.keystore.KeystoreScreen
import com.scizor.feature.location.LocationSpooferScreen
import com.scizor.feature.network.NetworkScreen
import com.scizor.feature.notifications.NotificationLoggerScreen
import com.scizor.feature.notifications.NotificationTesterScreen
import com.scizor.feature.preferences.PreferencesScreen
import com.scizor.feature.servers.ServersScreen

/**
 * Registers Scizor's built-in feature screens into the [FeatureRegistry].
 * Called once from [com.scizor.Scizor.start]. Sections mirror Scyther's grouping.
 * Device & Application facts are rendered inline by the menu, not as an entry.
 */
internal fun registerBuiltInFeatures() {
    // Networking
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "network",
            title = "Network Logger",
            subtitle = "Inspect HTTP and GraphQL traffic",
            icon = Icons.Filled.CompareArrows,
            section = "Networking",
            screen = { NetworkScreen(it) },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "servers",
            title = "Server Configuration",
            subtitle = "Switch backend environment",
            icon = Icons.Filled.Dns,
            section = "Networking",
            screen = { ServersScreen() },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "environment_variables",
            title = "Environment Variables",
            subtitle = "Host-provided key/value pairs",
            icon = Icons.Filled.Code,
            section = "Networking",
            screen = { EnvironmentVariablesScreen() },
        ),
    )

    // Data
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "feature_flags",
            title = "Feature Flags",
            subtitle = "Override flags at runtime",
            icon = Icons.Filled.Flag,
            section = "Data",
            screen = { FeatureFlagsScreen() },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "preferences",
            title = "Preferences Browser",
            subtitle = "View and edit SharedPreferences",
            icon = Icons.Filled.Tune,
            section = "Data",
            screen = { PreferencesScreen(it) },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "cookies",
            title = "Cookie Browser",
            subtitle = "Cookies seen in captured traffic",
            icon = Icons.Filled.Cookie,
            section = "Data",
            screen = { CookiesScreen(it) },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "file_browser",
            title = "File Browser",
            subtitle = "Browse the app's sandbox files",
            icon = Icons.Filled.Folder,
            section = "Data",
            screen = { FileBrowserScreen(it) },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "database_browser",
            title = "Database Browser",
            subtitle = "Browse and query SQLite databases",
            icon = Icons.Filled.Storage,
            section = "Data",
            screen = { DatabaseBrowserScreen(it) },
        ),
    )

    // Security
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "keystore",
            title = "Keystore Browser",
            subtitle = "Inspect AndroidKeyStore entries",
            icon = Icons.Filled.Key,
            section = "Security",
            screen = { KeystoreScreen(it) },
        ),
    )

    // System Tools
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "location",
            title = "Location Spoofer",
            subtitle = "Mock GPS location and routes",
            icon = Icons.Filled.LocationOn,
            section = "System Tools",
            screen = { LocationSpooferScreen() },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "console",
            title = "Console Logger",
            subtitle = "Live Logcat output",
            icon = Icons.Filled.Terminal,
            section = "System Tools",
            screen = { ConsoleScreen() },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "deep_link",
            title = "Deep Link Tester",
            subtitle = "Fire URLs and schemes",
            icon = Icons.Filled.Link,
            section = "System Tools",
            screen = { DeepLinkTesterScreen() },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "crash_logs",
            title = "Crash Logs",
            subtitle = "Captured uncaught exceptions",
            icon = Icons.Filled.BugReport,
            section = "System Tools",
            screen = { CrashLogsScreen(it) },
        ),
    )

    // Notifications
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "notification_logger",
            title = "Notification Logger",
            subtitle = "Log posted notifications",
            icon = Icons.Filled.ListAlt,
            section = "Notifications",
            screen = { NotificationLoggerScreen(it) },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "notification_tester",
            title = "Notification Tester",
            subtitle = "Post a local test notification",
            icon = Icons.Filled.Notifications,
            section = "Notifications",
            screen = { NotificationTesterScreen() },
        ),
    )

    // UI/UX — order mirrors Scyther: Fonts, Interface Previews, then the overlay tools,
    // then Appearance. Slow animations / view frames / view sizes are inline toggles
    // added by the menu view model.
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "fonts",
            title = "Fonts",
            subtitle = "Browse app and system fonts",
            icon = Icons.Filled.TextFields,
            section = "UI/UX",
            screen = { FontsScreen() },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "interface_previews",
            title = "Interface Previews",
            subtitle = "Host-registered components",
            icon = Icons.Filled.FormatShapes,
            section = "UI/UX",
            screen = { InterfacePreviewsScreen(it) },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "grid_overlay",
            title = "Grid Overlay",
            subtitle = "Overlay a spacing grid",
            icon = Icons.Filled.GridView,
            section = "UI/UX",
            screen = { GridOverlayScreen() },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "fps_counter",
            title = "FPS Counter",
            subtitle = "Frame-rate meter overlay",
            icon = Icons.Filled.Speed,
            section = "UI/UX",
            screen = { FpsCounterScreen() },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "touch_visualiser",
            title = "Touch Visualiser",
            subtitle = "Show every touch on screen",
            icon = Icons.Filled.TouchApp,
            section = "UI/UX",
            screen = { TouchVisualiserScreen() },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "appearance",
            title = "Appearance",
            subtitle = "Theme, font scale, contrast",
            icon = Icons.Filled.DarkMode,
            section = "UI/UX",
            screen = { AppearanceScreen() },
        ),
    )
}
