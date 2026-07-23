package com.scizor.feature.network

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.scizor.ui.TopBarMenuItem
import com.scizor.ui.rememberSearchQuery
import com.scizor.ui.rememberTopBarActionMenu

/** A full-screen, selectable, searchable text reader with highlight + share. */
@Composable
internal fun TextReaderScreen(text: String) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val query = rememberSearchQuery("Search")
    val highlightColor = MaterialTheme.colorScheme.tertiary

    rememberTopBarActionMenu(
        icon = Icons.Filled.Share,
        description = "Copy or share",
        items = listOf(
            TopBarMenuItem("Copy", Icons.Filled.ContentCopy) { clipboard.setText(AnnotatedString(text)) },
            TopBarMenuItem("Share", Icons.Filled.Share) { share(context, text) },
        ),
    )

    val annotated = remember(text, query, highlightColor) { highlight(text, query, highlightColor) }
    val matches = remember(text, query) {
        if (query.isBlank()) 0 else Regex(Regex.escape(query), RegexOption.IGNORE_CASE).findAll(text).count()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (query.isNotBlank()) {
            Text(
                "$matches match${if (matches == 1) "" else "es"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        SelectionContainer(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = annotated,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

private fun highlight(text: String, query: String, color: Color): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        Regex(Regex.escape(query), RegexOption.IGNORE_CASE).findAll(text).forEach { m ->
            addStyle(SpanStyle(background = color.copy(alpha = 0.4f)), m.range.first, m.range.last + 1)
        }
    }
}

private fun share(context: android.content.Context, text: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
