package com.scizor.core

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val title by remember {
        androidx.compose.runtime.derivedStateOf { navigator.current?.title ?: "Scizor" }
    }

    BackHandler(enabled = true) {
        if (!navigator.pop()) onClose()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = title, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    val atRoot = navigator.stack.isEmpty()
                    CircleButton(
                        onClick = { if (!navigator.pop()) onClose() },
                    ) {
                        Icon(
                            imageVector = if (atRoot) {
                                Icons.Filled.Close
                            } else {
                                Icons.AutoMirrored.Filled.ArrowBack
                            },
                            contentDescription = if (atRoot) "Close" else "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val destination = navigator.current
            if (destination == null) {
                MenuScreen(navigator = navigator)
            } else {
                destination.content()
            }
        }
    }
}

@Composable
private fun CircleButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CircleShape,
        shadowElevation = 1.dp,
        modifier = Modifier
            .padding(start = 8.dp)
            .size(36.dp),
    ) {
        IconButton(onClick = onClick) { content() }
    }
}
