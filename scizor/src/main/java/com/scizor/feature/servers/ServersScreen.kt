package com.scizor.feature.servers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
internal fun ServersScreen(viewModel: ServersViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.environments.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No server environments configured.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(state.environments, key = { it.name }) { environment ->
            val selected = environment.name == state.selectedName
            ListItem(
                headlineContent = { Text(environment.name) },
                supportingContent = { Text(environment.baseUrl) },
                leadingContent = {
                    RadioButton(selected = selected, onClick = { viewModel.select(environment) })
                },
                modifier = Modifier.selectable(
                    selected = selected,
                    onClick = { viewModel.select(environment) },
                ),
            )
            HorizontalDivider()
        }
    }
}
