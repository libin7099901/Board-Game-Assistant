package com.example.tabletopcompanion.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.charset.StandardCharsets

class NsdHelper(private val context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val resolveListenerMap = mutableMapOf<String, NsdManager.ResolveListener>()

    val discoveredServices: MutableStateFlow<List<NsdServiceInfo>> = MutableStateFlow(emptyList())

    companion object {
        const val SERVICE_TYPE = "_tabletopcompanion._tcp."
        const val SERVICE_NAME_PREFIX = "TabletopCompanion_"
        private const val TAG = "NsdHelper"
    }

    fun registerService(serviceNameSuffix: String, port: Int, attributes: Map<String, String>) {
        unregisterService() // Ensure no previous service is registered

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME_PREFIX + serviceNameSuffix
            this.serviceType = SERVICE_TYPE
            this.port = port
            attributes.forEach { (key, value) ->
                // NsdServiceInfo.setAttribute(String, String) is fine for API 24+
                setAttribute(key, value)
            }
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                Log.i(TAG, "Service registered: ${nsdServiceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service registration failed: $errorCode for ${serviceInfo.serviceName}")
                // Potentially try again or notify UI
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Log.i(TAG, "Service unregistered: ${arg0.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service unregistration failed: $errorCode for ${serviceInfo.serviceName}")
            }
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering service: ${e.message}", e)
        }
    }

    fun unregisterService() {
        registrationListener?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering service: ${e.message}", e)
            }
            registrationListener = null
        }
    }

    fun startDiscovery() {
        stopDiscovery() // Ensure no previous discovery is active

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.i(TAG, "Service discovery started for type: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.i(TAG, "Service found: ${service.serviceName}, type: ${service.serviceType}")
                if (service.serviceType == SERVICE_TYPE) {
                    // Avoid resolving services we are hosting ourselves if serviceName is known
                    // For now, resolve all found services of our type
                    if (!resolveListenerMap.containsKey(service.serviceName)) {
                        val resolveListener = object : NsdManager.ResolveListener {
                            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                                Log.i(TAG, "Service resolved: ${serviceInfo.serviceName}, host: ${serviceInfo.host}, port: ${serviceInfo.port}")
                                val currentList = discoveredServices.value.toMutableList()
                                currentList.removeAll { it.serviceName == serviceInfo.serviceName } // Remove old entry if exists
                                currentList.add(serviceInfo)
                                discoveredServices.value = currentList.toList()
                                resolveListenerMap.remove(serviceInfo.serviceName) // Clean up listener
                            }

                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                                Log.e(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                                resolveListenerMap.remove(serviceInfo.serviceName) // Clean up listener
                            }
                        }
                        resolveListenerMap[service.serviceName] = resolveListener
                        nsdManager.resolveService(service, resolveListener)
                    }
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.w(TAG, "Service lost: ${service.serviceName}")
                val currentList = discoveredServices.value.toMutableList()
                currentList.removeAll { it.serviceName == service.serviceName }
                discoveredServices.value = currentList.toList()
                resolveListenerMap.remove(service.serviceName) // Clean up listener if it was resolving
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(TAG, "Service discovery stopped for type: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery start failed for $serviceType: $errorCode")
                stopDiscovery() // Clean up
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery stop failed for $serviceType: $errorCode")
            }
        }
        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting discovery: ${e.message}", e)
        }
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping discovery: ${e.message}", e)
            }
            discoveryListener = null
        }
        // Clear any pending resolve listeners
        resolveListenerMap.keys.forEach { serviceName ->
            // NsdManager doesn't have a "cancelResolve" method.
            // Removing from map is the best we can do here.
            // The callbacks might still fire but their effects will be minimal if listeners are cleared.
        }
        resolveListenerMap.clear()
        // Optionally clear discovered services list or leave it as is for UI to decide
        // discoveredServices.value = emptyList() // Uncomment if services should clear on stop
    }
}
