package com.mytv.remote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mytv.remote.model.RemoteButton

/**
 * Simple checklist + up/down reordering. No drag library dependency
 * needed for a first version — arrows are enough for "move mute next
 * to volume" style tweaks, and it's easy to follow with no explanation.
 */
@Composable
fun CustomizeScreen(
    currentLayout: List<RemoteButton>,
    onLayoutChanged: (List<RemoteButton>) -> Unit,
    onDone: () -> Unit
) {
    val allButtons = RemoteButton.entries
    val enabledIds = currentLayout.map { it.id }.toMutableList()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Customize buttons") }) },
        bottomBar = {
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Done")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(currentLayout) { button ->
                ButtonRow(
                    button = button,
                    enabled = true,
                    onToggle = {
                        onLayoutChanged(currentLayout.filterNot { it.id == button.id })
                    },
                    onMoveUp = {
                        val i = currentLayout.indexOf(button)
                        if (i > 0) onLayoutChanged(currentLayout.toMutableList().apply {
                            add(i - 1, removeAt(i))
                        })
                    },
                    onMoveDown = {
                        val i = currentLayout.indexOf(button)
                        if (i < currentLayout.size - 1) onLayoutChanged(currentLayout.toMutableList().apply {
                            add(i + 1, removeAt(i))
                        })
                    }
                )
            }

            val available = allButtons.filterNot { it.id in enabledIds }
            if (available.isNotEmpty()) {
                item {
                    Text(
                        "Add a button",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items(available) { button ->
                    ButtonRow(
                        button = button,
                        enabled = false,
                        onToggle = { onLayoutChanged(currentLayout + button) },
                        onMoveUp = {},
                        onMoveDown = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun ButtonRow(
    button: RemoteButton,
    enabled: Boolean,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    ListItem(
        headlineContent = { Text(button.label) },
        leadingContent = {
            Checkbox(checked = enabled, onCheckedChange = { onToggle() })
        },
        trailingContent = {
            if (enabled) {
                Row {
                    IconButton(onClick = onMoveUp) { Text("↑") }
                    IconButton(onClick = onMoveDown) { Text("↓") }
                }
            }
        }
    )
    Divider()
}
