package com.taskcenter.app.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.taskcenter.app.data.database.entity.Space

/**
 * Wraps Android's Network Service Discovery (NSD) API to:
 *  - Register this device's spaces so others can discover them.
 *  - Discover other TaskCenter devices on the same LAN.
 */
class NsdHelper(context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var resolveListeners = mutableListOf<NsdManager.ResolveListener>()

    var onServiceFound: ((ip: String, port: Int) -> Unit)? = null
    var onServiceLost: ((serviceName: String) -> Unit)? = null

    fun registerService(port: Int, serviceName: String) {
        unregisterService()
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = SERVICE_TYPE
            this.port = port
        }
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) =
                Log.d(TAG, "NSD registered: ${info.serviceName}")
            override fun onRegistrationFailed(info: NsdServiceInfo, code: Int) =
                Log.e(TAG, "NSD registration failed: $code")
            override fun onServiceUnregistered(info: NsdServiceInfo) =
                Log.d(TAG, "NSD unregistered: ${info.serviceName}")
            override fun onUnregistrationFailed(info: NsdServiceInfo, code: Int) =
                Log.e(TAG, "NSD unregistration failed: $code")
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun unregisterService() {
        registrationListener?.let {
            try { nsdManager.unregisterService(it) } catch (e: Exception) { /* ignore */ }
        }
        registrationListener = null
    }

    fun startDiscovery() {
        stopDiscovery()
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(type: String) =
                Log.d(TAG, "NSD discovery started")
            override fun onDiscoveryStopped(type: String) =
                Log.d(TAG, "NSD discovery stopped")
            override fun onStartDiscoveryFailed(type: String, code: Int) =
                Log.e(TAG, "NSD start discovery failed: $code")
            override fun onStopDiscoveryFailed(type: String, code: Int) =
                Log.e(TAG, "NSD stop discovery failed: $code")

            override fun onServiceFound(info: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${info.serviceName} type=${info.serviceType}")
                if (info.serviceType.contains(SERVICE_TYPE.trimEnd('.'))) {
                    resolveService(info)
                }
            }

            override fun onServiceLost(info: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${info.serviceName}")
                onServiceLost?.invoke(info.serviceName)
            }
        }
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try { nsdManager.stopServiceDiscovery(it) } catch (e: Exception) { /* ignore */ }
        }
        discoveryListener = null
    }

    private fun resolveService(info: NsdServiceInfo) {
        val listener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, code: Int) =
                Log.e(TAG, "NSD resolve failed: $code")
            override fun onServiceResolved(info: NsdServiceInfo) {
                val ip = info.host?.hostAddress ?: return
                val port = info.port
                Log.d(TAG, "Resolved: $ip:$port (${info.serviceName})")
                onServiceFound?.invoke(ip, port)
            }
        }
        resolveListeners.add(listener)
        nsdManager.resolveService(info, listener)
    }

    fun tearDown() {
        stopDiscovery()
        unregisterService()
    }

    companion object {
        private const val TAG = "NsdHelper"
        const val SERVICE_TYPE = "_taskcenter._tcp."
    }
}
