package com.novacorp.inmonode_app.core.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityMonitor @Inject constructor(@ApplicationContext context: Context) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)

    val isOnline: Flow<Boolean> = callbackFlow {
        val lock = Any()
        val tracker = DefaultNetworkTracker<Network>()
        var reported = false
        fun report(change: DefaultNetworkTracker<Network>.() -> Unit) {
            synchronized(lock) {
                reported = true
                tracker.change()
                trySend(tracker.isOnline)
            }
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { report { onAvailable(network, isValidated(network)) } }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                report { onCapabilitiesChanged(network, capabilities.isValidatedInternet()) }
            }
            override fun onLost(network: Network) { report { onLost(network) } }
        }
        manager.registerDefaultNetworkCallback(callback)
        // The first value comes from activeNetwork only while no callback has arrived; callbacks always win.
        synchronized(lock) {
            if (!reported) manager.activeNetwork?.let { tracker.onAvailable(it, isValidated(it)) }
            trySend(tracker.isOnline)
        }
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun isValidated(network: Network): Boolean =
        manager.getNetworkCapabilities(network)?.isValidatedInternet() == true
}

private fun NetworkCapabilities.isValidatedInternet(): Boolean =
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
