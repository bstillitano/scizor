package com.scizor.feature.notifications

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scizor.ui.SectionHeader

@Composable
internal fun NotificationTesterScreen() {
    val context = LocalContext.current
    var title by remember { mutableStateOf("Scizor test") }
    var body by remember { mutableStateOf("Hello from Scizor") }
    var hasPermission by remember { mutableStateOf(NotificationTester.hasPermission(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        SectionHeader("Notification", modifier = Modifier.padding(start = 12.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Body") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
        if (hasPermission) {
            Button(
                onClick = { NotificationTester.fire(context, title, body) },
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                Text("Send notification")
            }
        } else {
            Text(
                "Notification permission is required to post test notifications.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Button(
                onClick = { launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Text("Grant permission")
            }
        }
    }
}
