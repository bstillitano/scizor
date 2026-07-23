@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.scizor.feature.fonts

import android.graphics.Typeface
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
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
import com.scizor.ui.EmptyState
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.Icons
import com.scizor.ui.SegmentInset
import com.scizor.ui.rememberSearchQuery
import com.scizor.ui.scizorSegmentedColors

private sealed interface FontRow {
    data class Header(val title: String) : FontRow
    data class Entry(val info: FontInfo, val indexInGroup: Int, val groupCount: Int) : FontRow
}

@Composable
internal fun FontsScreen() {
    val context = LocalContext.current
    val families = remember { FontsBrowser.families() }
    val appFonts = remember { FontsBrowser.appFonts(context) }
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val query = rememberSearchQuery("Search fonts")

    val rows = remember(families, appFonts, query) {
        buildList {
            val matchedApp = appFonts.filter { query.isBlank() || it.name.contains(query, true) }
            if (matchedApp.isNotEmpty()) {
                add(FontRow.Header("App fonts (${matchedApp.size})"))
                matchedApp.forEachIndexed { i, f -> add(FontRow.Entry(f, i, matchedApp.size)) }
            }
            families.forEach { family ->
                val matched = family.fonts.filter {
                    query.isBlank() || it.name.contains(query, true) || family.name.contains(query, true)
                }
                if (matched.isNotEmpty()) {
                    add(FontRow.Header("${family.name} (${matched.size})"))
                    matched.forEachIndexed { i, f -> add(FontRow.Entry(f, i, matched.size)) }
                }
            }
        }
    }

    if (rows.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.TextFields,
            title = if (query.isBlank()) "No fonts found" else "No fonts match “$query”",
            description = if (query.isBlank()) null else "Try a different search term.",
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        itemsIndexed(rows) { _, row ->
            when (row) {
                is FontRow.Header -> SectionHeader(row.title)
                is FontRow.Entry -> SegmentedListItem(
                    modifier = Modifier.padding(horizontal = SegmentInset),
                    shapes = ListItemDefaults.segmentedShapes(index = row.indexInGroup, count = row.groupCount),
                    colors = scizorSegmentedColors(),
                    supportingContent = {
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
                    },
                    content = {
                        Text(row.info.name, color = MaterialTheme.colorScheme.primary)
                    },
                )
            }
        }
    }
}
