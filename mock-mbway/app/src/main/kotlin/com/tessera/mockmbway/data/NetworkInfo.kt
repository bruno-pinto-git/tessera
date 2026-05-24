package com.tessera.mockmbway.data

import android.util.Log
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkInfo {

    private const val TAG = "NetworkInfo"

    fun localIPv4(): String? = try {
        NetworkInterface.getNetworkInterfaces()
            .toList()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress
    } catch (e: Exception) {
        Log.e(TAG, "Failed to read local IPv4", e)
        null
    }
}
