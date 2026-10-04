@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.mytv.remote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mytv.remote.model.RemoteButton

/**
 * Deliberately plain: a scrollable grid of big, clearly-labelled
 * buttons. No dashboards, no ad banners, nothing to configure to just
 * turn the TV on and change the volume.
 */
@Composable
fun RemoteScreen(
    tvName: String,
    connected: Boolean,
    layout: List<RemoteButton>,
    onButtonPress: (RemoteButton) -> Unit,
    onMicPress: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (connected) tvName else "Not connected") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Customize buttons")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onMicPress) {
                Icon(Icons.Filled.Mic, contentDescription = "Voice search")
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(layout) { button ->
                RemoteButtonCell(button = button, onClick = { onButtonPress(button) })
            }
        }
    }
}

@Composable
private fun RemoteButtonCell(button: RemoteButton, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(button.label, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
