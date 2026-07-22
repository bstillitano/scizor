package com.scizor.sample

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scizor.Scizor
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Scizor.wrapAppearance(newBase))
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(Scizor.network.interceptor())
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SampleContent(
                        onOpenScizor = { Scizor.show() },
                        onMakeRequest = { makeSampleRequest() },
                        onLogMessages = { logSampleMessages() },
                        onTriggerCrash = { throw RuntimeException("Sample crash triggered from Scizor") },
                    )
                }
            }
        }
    }

    private fun makeSampleRequest() {
        thread {
            runCatching {
                val request = Request.Builder()
                    .url("https://httpbin.org/get")
                    .build()
                client.newCall(request).execute().use { it.body?.string() }
            }
        }
    }

    private fun logSampleMessages() {
        android.util.Log.d("ScizorSample", "Debug: computed cart total")
        android.util.Log.i("ScizorSample", "Info: user tapped log button")
        android.util.Log.w("ScizorSample", "Warning: cache nearly full")
        android.util.Log.e("ScizorSample", "Error: sample failure for demo")
    }
}

@Composable
private fun SampleContent(
    onOpenScizor: () -> Unit,
    onMakeRequest: () -> Unit,
    onLogMessages: () -> Unit,
    onTriggerCrash: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Scizor Sample",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Shake the device or tap below to open the debug menu.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        Button(onClick = onOpenScizor) {
            Text("Open Scizor")
        }
        Button(
            onClick = onMakeRequest,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text("Make sample HTTP request")
        }
        Button(
            onClick = onLogMessages,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text("Log sample messages")
        }
        Button(
            onClick = onTriggerCrash,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text("Trigger test crash")
        }
    }
}
