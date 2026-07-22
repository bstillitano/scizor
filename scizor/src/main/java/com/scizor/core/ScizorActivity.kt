package com.scizor.core

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.scizor.ui.MenuScreen
import com.scizor.ui.ScizorNavigator
import com.scizor.ui.ScizorTheme

/**
 * Transparent activity that hosts the Scizor debug menu on top of the host app.
 * Launched by [com.scizor.Scizor.show]. Renders the menu at the root and pushes
 * feature screens onto an in-memory [ScizorNavigator] stack.
 */
class ScizorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScizorTheme {
                ScizorHost(onClose = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScizorHost(onClose: () -> Unit) {
    val navigator = remember { ScizorNavigator() }
    val current by remember { derivedTitle(navigator) }

    BackHandler(enabled = true) {
        if (!navigator.pop()) onClose()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(current) },
                    navigationIcon = {
                        val atRoot = navigator.stack.isEmpty()
                        IconButton(onClick = { if (!navigator.pop()) onClose() }) {
                            Icon(
                                imageVector = if (atRoot) {
                                    Icons.Filled.Close
                                } else {
                                    Icons.AutoMirrored.Filled.ArrowBack
                                },
                                contentDescription = if (atRoot) "Close" else "Back",
                            )
                        }
                    },
                )
            },
        ) { padding ->
            Surface(modifier = Modifier.fillMaxSize().padding(padding)) {
                val destination = navigator.current
                if (destination == null) {
                    MenuScreen(navigator = navigator)
                } else {
                    destination.content()
                }
            }
        }
    }
}

/** Title shown in the top bar: the current screen, or "Scizor" at the root. */
private fun derivedTitle(navigator: ScizorNavigator) =
    androidx.compose.runtime.derivedStateOf {
        navigator.current?.title ?: "Scizor"
    }
