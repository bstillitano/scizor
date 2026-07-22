package com.scizor.feature.featureflags

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
internal fun FeatureFlagsScreen(viewModel: FeatureFlagsViewModel = viewModel()) {
    val flags by viewModel.flags.collectAsStateWithLifecycle()

    if (flags.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No feature flags registered.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(flags, key = { it.key }) { flag ->
            ListItem(
                headlineContent = { Text(flag.title) },
                supportingContent = {
                    val label = if (flag.overridden) "overridden" else "default"
                    Text("${flag.key}  ·  $label")
                },
                trailingContent = {
                    Switch(
                        checked = flag.enabled,
                        onCheckedChange = { viewModel.toggle(flag.key, it) },
                    )
                },
            )
            if (flag.overridden) {
                TextButton(
                    onClick = { viewModel.reset(flag.key) },
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text("Reset to default")
                }
            }
            HorizontalDivider()
        }
    }
}
