package com.mytv.remote.network

import android.content.Context
import com.mytv.remote.proto.RemoteProto.RemoteDirection
import com.mytv.remote.proto.RemoteProto.RemoteKeyCode
import com.mytv.remote.proto.RemoteProto.RemoteKeyInject
import com.mytv.remote.proto.RemoteProto.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

private const val REMOTE_PORT = 6466

/**
 * Holds the long-lived connection used once a TV is paired: sends
 * RemoteKeyInject messages for every button press. Uses the same
 * client certificate that was used during pairing, since the TV
 * whitelists that certificate after a successful pairing exchange.
 */
class RemoteClient(context: Context, private val host: String) {

    private val certManager = CertificateManager(context)
    private var socket: SSLSocket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(certManager.keyManagers(), certManager.trustAllTrustManager(), null)
            val raw = Socket(host, REMOTE_PORT)
            val ssl = sslContext.socketFactory.createSocket(raw, host, REMOTE_PORT, true) as SSLSocket
            ssl.startHandshake()

            socket = ssl
            input = DataInputStream(ssl.inputStream)
            output = DataOutputStream(ssl.outputStream)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendKey(keyCode: RemoteKeyCode, direction: RemoteDirection = RemoteDirection.SHORT) =
        withContext(Dispatchers.IO) {
            val message = RemoteMessage.newBuilder()
                .setRemoteKeyInject(
                    RemoteKeyInject.newBuilder()
                        .setKeyCode(keyCode)
                        .setDirection(direction)
                )
                .build()
            send(message)
        }

    /**
     * Voice/search input is sent as an app-link launch request pointing
     * at a search deep link rather than as individual key presses, since
     * the remote protocol has no "type this word" primitive of its own.
     */
    suspend fun sendSearch(query: String) = withContext(Dispatchers.IO) {
        val message = RemoteMessage.newBuilder()
            .setRemoteAppLinkLaunchRequest(
                com.mytv.remote.proto.RemoteProto.RemoteAppLinkLaunchRequest.newBuilder()
                    .setAppLink("https://www.google.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}")
            )
            .build()
        send(message)
    }

    private fun send(message: RemoteMessage) {
        val out = output ?: return
        val bytes = message.toByteArray()
        out.writeByte(bytes.size)
        out.write(bytes)
        out.flush()
    }

    fun disconnect() {
        runCatching { socket?.close() }
        socket = null
        input = null
        output = null
    }
}
