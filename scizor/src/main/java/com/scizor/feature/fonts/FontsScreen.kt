package com.scizor.feature.fonts

import android.graphics.Typeface
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.scizor.ui.SectionHeader

private sealed interface FontRow {
    data class Header(val title: String) : FontRow
    data class Font(val info: FontInfo) : FontRow
}

@Composable
internal fun FontsScreen() {
    val context = LocalContext.current
    val families = remember { FontsBrowser.families() }
    val appFonts = remember { FontsBrowser.appFonts(context) }
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    val rows = remember(families, appFonts) {
        buildList {
            if (appFonts.isNotEmpty()) {
                add(FontRow.Header("App fonts (${appFonts.size})"))
                appFonts.forEach { add(FontRow.Font(it)) }
            }
            families.forEach { family ->
                add(FontRow.Header("${family.name} (${family.fonts.size})"))
                family.fonts.forEach { add(FontRow.Font(it)) }
            }
        }
    }

    if (rows.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No fonts found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(rows.size) { index ->
            when (val row = rows[index]) {
                is FontRow.Header -> SectionHeader(row.title)
                is FontRow.Font -> Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        text = row.info.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    AndroidView(
                        factory = { ctx ->
                            TextView(ctx).apply {
                                textSize = 22f
                                text = "AaBbCc  0123"
                                setTextColor(textColor)
                            }
                        },
                        update = { view ->
                            runCatching {
                                view.typeface = if (row.info.isAsset) {
                                    Typeface.createFromAsset(context.assets, row.info.path)
                                } else {
                                    Typeface.createFromFile(row.info.path)
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
