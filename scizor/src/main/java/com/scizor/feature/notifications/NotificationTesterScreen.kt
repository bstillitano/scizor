@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.scizor.feature.notifications

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scizor.ui.rememberTopBarAction
import com.scizor.ui.rememberTopBarSubtitle
import com.scizor.ui.SectionHeader
import com.scizor.ui.SegmentedColumn
import com.scizor.ui.scizorSegmentedColors

@Composable
internal fun NotificationTesterScreen() {
    val context = LocalContext.current
    val scheduled by NotificationTester.scheduled.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("Scizor test") }
    var body by remember { mutableStateOf("Hello from Scizor") }
    var payload by remember { mutableStateOf("") }
    var sound by remember { mutableStateOf(true) }
    var delay by remember { mutableStateOf("0") }
    var repeat by remember { mutableStateOf("1") }
    var hasPermission by remember { mutableStateOf(NotificationTester.hasPermission(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    // Permission status becomes the app-bar subtitle; a settings cog opens app notification settings.
    rememberTopBarSubtitle(if (hasPermission) "Notifications permitted" else "Permission required")
    rememberTopBarAction(Icons.Filled.Settings, "Notification settings") {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("Content")
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Body") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        OutlinedTextField(
            value = payload,
            onValueChange = { payload = it },
            label = { Text("Payload (big text / data)") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )

        SectionHeader("Options")
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Sound", modifier = Modifier.weight(1f))
            Switch(checked = sound, onCheckedChange = { sound = it })
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = delay,
                onValueChange = { delay = it.filter(Char::isDigit) },
                label = { Text("Delay (s)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            OutlinedTextField(
                value = repeat,
                onValueChange = { repeat = it.filter(Char::isDigit) },
                label = { Text("Repeat count") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }

        if (hasPermission) {
            Button(
                onClick = {
                    NotificationTester.fire(
                        context,
                        TestOptions(
                            title = title,
                            body = body,
                            payload = payload,
                            sound = sound,
                            delaySeconds = delay.toIntOrNull() ?: 0,
                            repeatCount = repeat.toIntOrNull() ?: 1,
                        ),
                    )
                },
                modifier = Modifier.padding(16.dp),
            ) { Text("Send notification") }
        } else {
            Button(
                onClick = { launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.padding(16.dp),
            ) { Text("Grant permission") }
        }

        if (scheduled.isNotEmpty()) {
            SectionHeader("Scheduled")
            SegmentedColumn(items = scheduled) { item, shapes ->
                SegmentedListItem(
                    shapes = shapes,
                    colors = scizorSegmentedColors(),
                    supportingContent = { Text("${item.remaining} remaining") },
                    trailingContent = {
                        OutlinedButton(onClick = { NotificationTester.cancel(item.id) }) { Text("Cancel") }
                    },
                    content = { Text(item.title) },
                )
            }
        }
    }
}
