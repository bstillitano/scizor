package com.scizor.core

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
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
    val title by remember {
        androidx.compose.runtime.derivedStateOf { navigator.current?.title ?: "Scizor" }
    }

    BackHandler(enabled = true) {
        if (!navigator.pop()) onClose()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = title)
                },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val stateHolder = rememberSaveableStateHolder()
            val depth = navigator.stack.size
            var previousDepth by remember { mutableIntStateOf(depth) }
            val forward = depth >= previousDepth
            SideEffect { previousDepth = depth }

            AnimatedContent(
                targetState = navigator.current,
                transitionSpec = {
                    val enter = slideInHorizontally(tween(280)) { full -> if (forward) full else -full } +
                        fadeIn(tween(280))
                    val exit = slideOutHorizontally(tween(280)) { full -> if (forward) -full else full } +
                        fadeOut(tween(280))
                    enter togetherWith exit
                },
                label = "scizor-nav",
            ) { destination ->
                // Key by stable id (root = -1) so each screen's scroll/UI state is
                // retained across push/pop instead of resetting to the top.
                stateHolder.SaveableStateProvider(destination?.id ?: ROOT_STATE_KEY) {
                    if (destination == null) {
                        MenuScreen(navigator = navigator)
                    } else {
                        destination.content()
                    }
                }
            }
        }
    }
}

private const val ROOT_STATE_KEY = -1
