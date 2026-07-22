package com.scizor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

/**
 * Wraps a row with a long-press copy menu. [content] receives an `onLongClick`
 * to attach to its clickable modifier; the menu offers each `label -> text`
 * option, copying [text] to the clipboard on tap.
 */
@Composable
internal fun CopyMenuHost(
    options: List<Pair<String, String>>,
    content: @Composable (onLongClick: () -> Unit) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var open by remember { mutableStateOf(false) }
    Box {
        content { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (label, text) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        clipboard.setText(AnnotatedString(text))
                        open = false
                    },
                )
            }
        }
    }
}
