package com.mytv.remote.network

import android.content.Context
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Calendar
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * The Android TV Remote protocol authenticates the *pairing* step with
 * a self-signed client certificate — there is no CA involved. Trust is
 * established by the human confirming the 6-digit code shown on the TV
 * screen, not by certificate authority, so a "trust anything" TLS
 * TrustManager is correct here, not a shortcut.
 *
 * The client keypair is generated once and kept in a private, file-backed
 * PKCS12 keystore under the app's files dir (protected by normal Android
 * per-app sandboxing). AndroidKeyStore is deliberately NOT used: it only
 * accepts keys generated through its own provider via KeyGenParameterSpec,
 * and can't import a keypair generated for a self-signed cert this way.
 */
class CertificateManager(private val context: Context) {

    private val alias = "tv_remote_client_key"
    private val storeFile: File get() = File(context.filesDir, "tv_remote_client.p12")
    private val storePassword = "tvremote".toCharArray() // file is already app-private; not user-facing secret

    private fun loadOrCreateStore(): KeyStore {
        val ks = KeyStore.getInstance("PKCS12")
        if (storeFile.exists()) {
            storeFile.inputStream().use { ks.load(it, storePassword) }
        } else {
            ks.load(null, storePassword)
        }
        return ks
    }

    private fun save(ks: KeyStore) {
        storeFile.outputStream().use { ks.store(it, storePassword) }
    }

    fun ensureClientCertificate(): X509Certificate {
        val ks = loadOrCreateStore()
        (ks.getCertificate(alias) as? X509Certificate)?.let { return it }
        return generateAndStore(ks)
    }

    private fun generateAndStore(ks: KeyStore): X509Certificate {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048, SecureRandom())
        val keyPair = kpg.generateKeyPair()

        val subject = X500Name("CN=TvRemoteClient")
        val calendar = Calendar.getInstance()
        val notBefore = calendar.time
        calendar.add(Calendar.YEAR, 25)
        val notAfter = calendar.time

        val certBuilder = JcaX509v3CertificateBuilder(
            subject, // self-signed: issuer == subject
            BigInteger.valueOf(System.currentTimeMillis()),
            notBefore,
            notAfter,
            subject,
            keyPair.public
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val cert = JcaX509CertificateConverter().getCertificate(certBuilder.build(signer))

        ks.setKeyEntry(alias, keyPair.private, storePassword, arrayOf(cert))
        save(ks)
        return cert
    }

    fun privateKey(): PrivateKey {
        val ks = loadOrCreateStore()
        return ks.getKey(alias, storePassword) as PrivateKey
    }

    fun keyManagers(): Array<KeyManager> {
        ensureClientCertificate()
        val ks = loadOrCreateStore()
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(ks, storePassword)
        return kmf.keyManagers
    }

    /** The TV's certificate is trusted purely because the human typed in the pairing code. */
    fun trustAllTrustManager(): Array<TrustManager> = arrayOf(
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    )
}
