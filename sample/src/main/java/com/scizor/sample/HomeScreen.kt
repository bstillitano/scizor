package com.scizor.sample

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scizor.Scizor
import com.scizor.feature.featureflags.FeatureFlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var requestCount by remember { mutableIntStateOf(0) }
    var graphQLCount by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var counts by remember { mutableStateOf(SampleDatabase.counts(context)) }

    fun io(block: () -> Unit) = scope.launch(Dispatchers.IO) { block() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Section(
                "Scizor Demo",
                rows = listOf(
                    SampleRow.Action("Open Scizor menu") { Scizor.show() },
                    SampleRow.Info("Or tap the floating 🐞 button (shake also works on a device)"),
                ),
            )
        }

        item {
            Section(
                "Network Requests",
                rows = listOf(
                    SampleRow.Action("Make sample request", enabled = !loading, loading = loading) {
                        loading = true
                        io {
                            SampleNetwork.get("https://jsonplaceholder.typicode.com/posts/1")
                            onMain { requestCount++; loading = false }
                        }
                    },
                    SampleRow.Action("Make multiple requests", enabled = !loading) {
                        loading = true
                        io {
                            SampleNetwork.restUrls.forEach { SampleNetwork.get(it); onMain { requestCount++ } }
                            onMain { loading = false }
                        }
                    },
                    SampleRow.Label("Requests made", requestCount.toString()),
                ),
            )
        }

        item {
            Section(
                "GraphQL Demo",
                footer = "Calls graphqlzero.almansi.me. Open Scizor → Networking → Network Logger to see the " +
                    "operation name and type.",
                rows = listOf(
                    SampleRow.Action("Run GraphQL query", enabled = !loading) {
                        io { SampleNetwork.graphQLQuery(); onMain { graphQLCount++ } }
                    },
                    SampleRow.Action("Run GraphQL mutation", enabled = !loading) {
                        io { SampleNetwork.graphQLMutation(); onMain { graphQLCount++ } }
                    },
                    SampleRow.Action("Run both operations", enabled = !loading) {
                        io {
                            SampleNetwork.graphQLQuery()
                            SampleNetwork.graphQLMutation()
                            onMain { graphQLCount += 2 }
                        }
                    },
                    SampleRow.Label("GraphQL calls", graphQLCount.toString()),
                ),
            )
        }

        item {
            Section(
                "Preferences Demo",
                rows = listOf(
                    SampleRow.Action("Write sample data") { writeSamplePrefs(context) },
                    SampleRow.Action("Clear sample data") { clearSamplePrefs(context) },
                ),
            )
        }

        item {
            Section(
                "Feature Flags Demo",
                rows = listOf(
                    SampleRow.Action("Register sample toggles") { registerSampleToggles() },
                ),
            )
        }

        item {
            Section(
                "Database Demo",
                footer = "Open Scizor → Data → Database Browser to inspect the demo.db tables.",
                rows = listOf(
                    SampleRow.Action("Add more records") {
                        SampleDatabase.addRandomRecords(context)
                        counts = SampleDatabase.counts(context)
                    },
                    SampleRow.Action("Clear all records", destructive = true) {
                        SampleDatabase.clear(context)
                        counts = SampleDatabase.counts(context)
                    },
                    SampleRow.Label("Users", counts.users.toString()),
                    SampleRow.Label("Posts", counts.posts.toString()),
                    SampleRow.Label("Products", counts.products.toString()),
                ),
            )
        }

        item {
            Section(
                "Crash Testing",
                footer = "This crashes the app. Reopen it to see the crash in Scizor → System Tools → Crash Logs.",
                rows = listOf(
                    SampleRow.Action("Trigger test crash", destructive = true) {
                        onMain { throw RuntimeException("Test crash triggered from Scizor sample") }
                    },
                ),
            )
        }
    }
}

private fun registerSampleToggles() {
    listOf(
        "new_onboarding_flow" to true,
        "dark_mode_v2" to false,
        "experimental_feature" to false,
        "show_beta_badge" to true,
        "enable_analytics" to true,
        "use_new_api" to false,
    ).forEach { (key, value) ->
        Scizor.featureFlags.register(
            FeatureFlag(key, key.replace('_', ' ').replaceFirstChar { it.uppercase() }, value),
        )
    }
}

private fun writeSamplePrefs(context: Context) {
    context.getSharedPreferences("example_prefs", Context.MODE_PRIVATE).edit()
        .putString("example_username", "John Doe")
        .putInt("example_age", 42)
        .putBoolean("example_premium_user", true)
        .putFloat("example_pi_value", 3.14159f)
        .putStringSet("example_tags", setOf("Kotlin", "Android", "Scizor"))
        .putLong("example_last_login", System.currentTimeMillis())
        .apply()
}

private fun clearSamplePrefs(context: Context) {
    context.getSharedPreferences("example_prefs", Context.MODE_PRIVATE).edit().clear().apply()
}
