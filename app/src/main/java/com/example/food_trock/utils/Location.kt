package com.example.food_trock.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.example.food_trock.DataManager
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.*

class MyLocation (var context: Context, var activity: Activity) {
    private var locationProviderClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
    private fun isLocationEnabled() : Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }

    fun getLocation() {
        if (!isLocationEnabled()) {
            activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else {
            fetchLocation()
        }
    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 200)
            return
        } else {
            requestLocation()
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun requestLocation() {

        val requestLocation = LocationRequest()
        requestLocation.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        requestLocation.interval = 50
        requestLocation.fastestInterval = 50

        locationProviderClient.requestLocationUpdates(
            requestLocation, callback, Looper.myLooper()
        )
    }

    private val callback = object: LocationCallback() {
        override fun onLocationAvailability(result: LocationAvailability) {
            super.onLocationAvailability(result)
        }

        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)

            val lastLocation = result?.lastLocation
            DataManager.currentLat = lastLocation.latitude.toString()
            DataManager.currentLng = lastLocation.longitude.toString()
        }
    }
}