package com.example.maps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationUtils {
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        return suspendCancellableCoroutine { cont ->
            fusedClient.lastLocation.addOnSuccessListener { location ->
                cont.resume(location)
            }
        }
    }
}
