package com.novacorp.inmonode_app.core.connectivity

/**
 * Follows the default network using only what its callbacks report. Reading `activeNetwork`
 * inside a callback races with the system: during `onLost` it can still return the lost network,
 * which left long-lived screens reporting "online" while the device had no connection.
 */
internal class DefaultNetworkTracker<N : Any> {
    private var network: N? = null
    private var validated = false

    val isOnline: Boolean get() = network != null && validated

    /** A network became the default one; [validated] is what that network reports right now. */
    fun onAvailable(network: N, validated: Boolean) = update(network, validated)

    fun onCapabilitiesChanged(network: N, validated: Boolean) = update(network, validated)

    /** Only the current default network can take the device offline; late losses of older ones are ignored. */
    fun onLost(network: N) {
        if (network == this.network) {
            this.network = null
            validated = false
        }
    }

    private fun update(network: N, validated: Boolean) {
        this.network = network
        this.validated = validated
    }
}
