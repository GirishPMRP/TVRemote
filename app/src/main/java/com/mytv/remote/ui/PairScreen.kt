package com.mytv.remote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mytv.remote.discovery.DiscoveredTv

@Composable
fun DiscoveryScreen(
    discovered: List<DiscoveredTv>,
    scanning: Boolean,
    onSelect: (DiscoveredTv) -> Unit,
    onManualEntry: (String) -> Unit
) {
    var manualHost by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Find your TV") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (scanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Looking for Android TV / Google TV devices on this WiFi network…")
                Spacer(Modifier.height(16.dp))
            }

            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(discovered) { tv ->
                    ListItem(
                        headlineContent = { Text(tv.name) },
                        supportingContent = { Text(tv.host) },
                        modifier = Modifier.clickableSurface { onSelect(tv) }
                    )
                    Divider()
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Or enter the TV's IP address manually:", style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(
                    value = manualHost,
                    onValueChange = { manualHost = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("192.168.1.50") }
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { if (manualHost.isNotBlank()) onManualEntry(manualHost) }) {
                    Text("Connect")
                }
            }
        }
    }
}

@Composable
fun PairingCodeScreen(
    tvHost: String,
    error: String?,
    onSubmitCode: (String) -> Unit
) {
    var code by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Enter the code on your TV") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text("A 6-character code should now be showing on \"$tvHost\".")
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { if (it.length <= 6) code = it },
                singleLine = true,
                label = { Text("Pairing code") }
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onSubmitCode(code) }, enabled = code.length == 6) {
                Text("Pair")
            }
        }
    }
}

// Small helper so ListItem rows are tappable without pulling in extra imports above.
private fun Modifier.clickableSurface(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
