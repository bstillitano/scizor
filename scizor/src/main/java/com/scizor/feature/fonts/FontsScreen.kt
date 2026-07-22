package com.scizor.feature.fonts

import android.graphics.Typeface
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun FontsScreen() {
    val fonts = remember { FontsBrowser.fonts() }
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    if (fonts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No system fonts found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(fonts, key = { it.path }) { font ->
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = font.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                AndroidView(
                    factory = { context ->
                        TextView(context).apply {
                            textSize = 22f
                            text = "AaBbCc  0123"
                            setTextColor(textColor)
                        }
                    },
                    update = { view ->
                        runCatching { view.typeface = Typeface.createFromFile(font.path) }
                    },
                )
            }
            HorizontalDivider()
        }
    }
}
