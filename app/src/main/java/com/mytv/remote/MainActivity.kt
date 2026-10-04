package com.mytv.remote

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.mytv.remote.discovery.DiscoveredTv
import com.mytv.remote.discovery.TvDiscovery
import com.mytv.remote.model.RemoteButton
import com.mytv.remote.model.RemotePrefs
import com.mytv.remote.network.PairingClient
import com.mytv.remote.network.PairingResult
import com.mytv.remote.network.RemoteClient
import com.mytv.remote.proto.RemoteProto
import com.mytv.remote.ui.CustomizeScreen
import com.mytv.remote.ui.DiscoveryScreen
import com.mytv.remote.ui.PairingCodeScreen
import com.mytv.remote.ui.RemoteScreen
import com.mytv.remote.voice.VoiceController
import kotlinx.coroutines.launch

private enum class Screen { DISCOVER, ENTER_CODE, REMOTE, CUSTOMIZE }

class MainActivity : ComponentActivity() {

    private lateinit var prefs: RemotePrefs
    private lateinit var discovery: TvDiscovery
    private var pairingClient: PairingClient? = null
    private var remoteClient: RemoteClient? = null
    private lateinit var voiceController: VoiceController

    private val requestMicPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* handled by caller re-tapping mic */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = RemotePrefs(this)
        discovery = TvDiscovery(this)
        voiceController = VoiceController(this)

        setContent {
            MaterialTheme {
                Surface {
                    AppRoot()
                }
            }
        }
    }

    @Composable
    private fun AppRoot() {
        var screen by remember { mutableStateOf(Screen.DISCOVER) }
        var discoveredTvs by remember { mutableStateOf(listOf<DiscoveredTv>()) }
        var scanning by remember { mutableStateOf(true) }
        var pendingHost by remember { mutableStateOf("") }
        var pairingError by remember { mutableStateOf<String?>(null) }
        var connectedHost by remember { mutableStateOf<String?>(null) }
        var connectedName by remember { mutableStateOf("") }
        var layout by remember { mutableStateOf(RemoteButton.DEFAULT_LAYOUT.mapNotNull(RemoteButton::fromId)) }

        // Load saved layout once.
        LaunchedEffect(Unit) {
            prefs.layout.collect { ids ->
                layout = ids.mapNotNull(RemoteButton::fromId)
            }
        }

        // Resume: if we already paired with a TV before, skip straight to it.
        LaunchedEffect(Unit) {
            prefs.lastTv.collect { last ->
                if (last != null && screen == Screen.DISCOVER) {
                    connectedHost = last.first
                    connectedName = last.second
                    screen = Screen.REMOTE
                }
            }
        }

        LaunchedEffect(screen) {
            if (screen == Screen.DISCOVER) {
                scanning = true
                discovery.discover().collect { tv ->
                    if (discoveredTvs.none { it.host == tv.host }) {
                        discoveredTvs = discoveredTvs + tv
                    }
                }
            }
        }

        fun beginPairing(host: String) {
            pendingHost = host
            pairingError = null
            lifecycleScope.launch {
                val client = PairingClient(this@MainActivity, host)
                pairingClient = client
                when (val result = client.start()) {
                    is PairingResult.AwaitingCode -> screen = Screen.ENTER_CODE
                    is PairingResult.Failed -> pairingError = result.reason
                    else -> {}
                }
            }
        }

        fun submitCode(code: String) {
            val client = pairingClient ?: return
            lifecycleScope.launch {
                when (val result = client.submitCode(code)) {
                    is PairingResult.Success -> {
                        prefs.saveLastTv(pendingHost, pendingHost, "default")
                        connectedHost = pendingHost
                        connectedName = pendingHost
                        remoteClient = RemoteClient(this@MainActivity, pendingHost).also { it.connect() }
                        screen = Screen.REMOTE
                    }
                    is PairingResult.Failed -> pairingError = result.reason
                    else -> {}
                }
            }
        }

        fun sendKey(button: RemoteButton) {
            lifecycleScope.launch {
                remoteClient?.sendKey(button.keyCode, RemoteProto.RemoteDirection.SHORT)
            }
        }

        fun startVoice() {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
                return
            }
            voiceController.listen(
                onResult = { text -> lifecycleScope.launch { remoteClient?.sendSearch(text) } },
                onError = { /* surface via a Snackbar in a fuller build */ }
            )
        }

        when (screen) {
            Screen.DISCOVER -> DiscoveryScreen(
                discovered = discoveredTvs,
                scanning = scanning,
                onSelect = { beginPairing(it.host) },
                onManualEntry = { host -> beginPairing(host) }
            )
            Screen.ENTER_CODE -> PairingCodeScreen(
                tvHost = pendingHost,
                error = pairingError,
                onSubmitCode = { submitCode(it) }
            )
            Screen.REMOTE -> RemoteScreen(
                tvName = connectedName,
                connected = connectedHost != null,
                layout = layout,
                onButtonPress = { sendKey(it) },
                onMicPress = { startVoice() },
                onOpenSettings = { screen = Screen.CUSTOMIZE }
            )
            Screen.CUSTOMIZE -> CustomizeScreen(
                currentLayout = layout,
                onLayoutChanged = { newLayout ->
                    layout = newLayout
                    lifecycleScope.launch { prefs.saveLayout(newLayout.map { it.id }) }
                },
                onDone = { screen = Screen.REMOTE }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pairingClient?.close()
        remoteClient?.disconnect()
        voiceController.destroy()
    }
}
