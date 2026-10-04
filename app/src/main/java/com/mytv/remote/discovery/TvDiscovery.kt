package com.mytv.remote.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow

data class DiscoveredTv(
    val name: String,
    val host: String,
    val port: Int
)

/**
 * Android TV / Google TV devices advertise the pairing service over
 * mDNS as "_androidtvremote2._tcp". Any device running that OS answers
 * here regardless of the TV's physical brand (Sony, TCL, Hisense,
 * Nvidia Shield, Chromecast with Google TV, etc.) since they all share
 * the same system-level remote service.
 */
class TvDiscovery(context: Context) {

    private val nsdManager = context.applicationContext
        .getSystemService(Context.NSD_SERVICE) as NsdManager

    private val serviceType = "_androidtvremote2._tcp."

    fun discover(): Flow<DiscoveredTv> = callbackFlow {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String?) {}
            override fun onDiscoveryStopped(serviceType: String?) {}
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}

            override fun onServiceFound(service: NsdServiceInfo) {
                nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}
                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val host = serviceInfo.host?.hostAddress ?: return
                        trySend(
                            DiscoveredTv(
                                name = serviceInfo.serviceName,
                                host = host,
                                port = serviceInfo.port
                            )
                        )
                    }
                })
            }

            override fun onServiceLost(service: NsdServiceInfo) {}
        }

        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
        awaitClose { nsdManager.stopServiceDiscovery(listener) }
    }
}
