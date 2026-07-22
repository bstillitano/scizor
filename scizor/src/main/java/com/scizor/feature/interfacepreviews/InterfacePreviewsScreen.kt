package com.scizor.feature.interfacepreviews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scizor.Scizor
import com.scizor.ui.ScizorNavigator

@Composable
internal fun InterfacePreviewsScreen(navigator: ScizorNavigator) {
    val previews = Scizor.interfacePreviews

    if (previews.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No interface previews registered.\nAssign Scizor.interfacePreviews in your app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        previews.forEach { preview ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(end = 32.dp)) {
                            Text(preview.name, style = MaterialTheme.typography.titleMedium)
                            preview.description?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Icon(
                            Icons.Filled.OpenInFull,
                            contentDescription = "Open full screen",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .clickable {
                                    navigator.push(preview.name) {
                                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                            preview.content()
                                        }
                                    }
                                },
                        )
                    }
                    // Inline rendering of the registered component
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        preview.content()
                    }
                }
            }
        }
    }
}
