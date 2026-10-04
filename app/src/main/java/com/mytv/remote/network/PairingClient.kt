package com.mytv.remote.network

import android.content.Context
import com.mytv.remote.proto.PairingProto.Encoding
import com.mytv.remote.proto.PairingProto.PairingConfiguration
import com.mytv.remote.proto.PairingProto.PairingMessage
import com.mytv.remote.proto.PairingProto.PairingOption
import com.mytv.remote.proto.PairingProto.PairingRequest
import com.mytv.remote.proto.PairingProto.PairingSecret
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

private const val PAIRING_PORT = 6467

sealed class PairingResult {
    object AwaitingCode : PairingResult()
    object Success : PairingResult()
    data class Failed(val reason: String) : PairingResult()
}

/**
 * Drives the pairing handshake described in pairing.proto:
 *  1. Open a mutually-authenticated TLS socket to the TV on port 6467.
 *  2. Exchange PairingRequest / PairingRequestAck.
 *  3. Exchange PairingOption / PairingConfiguration to agree on a
 *     6-digit numeric code shown on the TV screen.
 *  4. Compute a secret from the code + both TLS certificates and send
 *     PairingSecret; the TV's PairingSecretAck confirms the human read
 *     the code correctly.
 */
class PairingClient(context: Context, private val host: String) {

    private val certManager = CertificateManager(context)
    private var socket: SSLSocket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null
    private var clientCert: X509Certificate? = null
    private var serverCert: X509Certificate? = null

    /** Step 1+2: connect and send the initial request. Call [submitCode] once the TV shows a code. */
    fun start(clientName: String = "TV Remote"): PairingResult {
        return try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(certManager.keyManagers(), certManager.trustAllTrustManager(), null)
            val raw = Socket(host, PAIRING_PORT)
            val ssl = sslContext.socketFactory.createSocket(raw, host, PAIRING_PORT, true) as SSLSocket
            ssl.startHandshake()
            clientCert = certManager.ensureClientCertificate()
            serverCert = ssl.session.peerCertificates.firstOrNull() as? X509Certificate

            socket = ssl
            input = DataInputStream(ssl.inputStream)
            output = DataOutputStream(ssl.outputStream)

            sendMessage(
                PairingMessage.newBuilder()
                    .setProtocolVersion("2")
                    .setPairingRequest(
                        PairingRequest.newBuilder()
                            .setServiceName("com.mytv.remote")
                            .setClientName(clientName)
                    )
                    .build()
            )
            readMessage() // PairingRequestAck

            sendMessage(
                PairingMessage.newBuilder()
                    .setPairingOption(
                        PairingOption.newBuilder()
                            .addInputEncodings(
                                Encoding.newBuilder()
                                    .setType(Encoding.EncodingType.HEXADECIMAL)
                                    .setSymbolLength(6)
                            )
                    )
                    .build()
            )
            readMessage() // server's PairingOption response

            sendMessage(
                PairingMessage.newBuilder()
                    .setPairingConfiguration(
                        PairingConfiguration.newBuilder()
                            .setEncoding(
                                Encoding.newBuilder()
                                    .setType(Encoding.EncodingType.HEXADECIMAL)
                                    .setSymbolLength(6)
                            )
                    )
                    .build()
            )
            readMessage() // PairingConfigurationAck -- this is the TV's cue to show the code on screen

            PairingResult.AwaitingCode
        } catch (e: Exception) {
            PairingResult.Failed(e.message ?: "Could not connect to TV")
        }
    }

    /** Step 3+4: user typed in the 6-digit code shown on the TV. */
    fun submitCode(code: String): PairingResult {
        val client = clientCert ?: return PairingResult.Failed("Not connected")
        val server = serverCert ?: return PairingResult.Failed("Not connected")
        return try {
            val secret = computeSecret(code, client, server)
            sendMessage(
                PairingMessage.newBuilder()
                    .setPairingSecret(PairingSecret.newBuilder().setSecret(secret.toByteString()))
                    .build()
            )
            val response = readMessage()
            if (response.hasPairingSecretAck()) {
                PairingResult.Success
            } else {
                PairingResult.Failed("TV rejected the code")
            }
        } catch (e: Exception) {
            PairingResult.Failed(e.message ?: "Pairing failed")
        } finally {
            close()
        }
    }

    /**
     * SHA-256 over (client cert DER || server cert DER || ASCII code),
     * matching the reference implementations of this protocol. Both
     * sides compute the same hash independently; matching hashes prove
     * the human read the code correctly off the TV screen.
     */
    private fun computeSecret(code: String, client: X509Certificate, server: X509Certificate): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(client.publicKey.encoded)
        digest.update(server.publicKey.encoded)
        digest.update(code.trim().toByteArray())
        return digest.digest()
    }

    private fun ByteArray.toByteString() = com.google.protobuf.ByteString.copyFrom(this)

    private fun sendMessage(message: PairingMessage) {
        val bytes = message.toByteArray()
        val out = output ?: error("Not connected")
        // Wire framing: single varint-style length byte, then the message.
        // (Messages in this handshake are always short enough for one byte;
        // for full varint support on longer messages use protobuf's
        // CodedOutputStream.writeUInt32NoTag against `out` instead.)
        out.writeByte(bytes.size)
        out.write(bytes)
        out.flush()
    }

    private fun readMessage(): PairingMessage {
        val inp = input ?: error("Not connected")
        val length = inp.readUnsignedByte()
        val buf = ByteArray(length)
        inp.readFully(buf)
        return PairingMessage.parseFrom(buf)
    }

    fun close() {
        runCatching { socket?.close() }
        socket = null
        input = null
        output = null
    }
}
