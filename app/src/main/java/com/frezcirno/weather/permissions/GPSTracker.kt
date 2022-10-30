package com.frezcirno.weather.permissions

import android.app.Service
import android.content.Context
import com.frezcirno.weather.model.Log.i
import com.frezcirno.weather.model.Log.d
import android.location.LocationListener
import android.location.LocationManager
import com.frezcirno.weather.R
import com.afollestad.materialdialogs.MaterialDialog
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.provider.Settings

class GPSTracker(private val mContext: Context) : LocationListener {
    private var isGPSEnabled = false
    private var isNetworkEnabled = false
    private var canGetLocation = false
    private var location: Location? = null
    private var latitude = 0.0
    private var longitude = 0.0

    // Declaring a Location Manager
    private var locationManager: LocationManager? = null

    init {
        getLocation()
    }

    private fun getLocation(): Location? {
        try {
            locationManager = mContext
                .getSystemService(Service.LOCATION_SERVICE) as LocationManager

            // getting GPS status
            isGPSEnabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)

            // getting network status
            isNetworkEnabled = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (!isGPSEnabled && !isNetworkEnabled) {
                i("Cant", "Perform Action") // no network provider is enabled
            } else {
                canGetLocation = true
                // First get location from Network Provider
                if (isNetworkEnabled) {
                    locationManager!!.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
                    )
                    d("Network", "Network")
                    if (locationManager != null) {
                        location = locationManager!!
                            .getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        if (location != null) {
                            latitude = location!!.latitude
                            longitude = location!!.longitude
                        }
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager!!.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
                        )
                        d("GPS Enabled", "GPS Enabled")
                        if (locationManager != null) {
                            location = locationManager!!
                                .getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            if (location != null) {
                                latitude = location!!.latitude
                                longitude = location!!.longitude
                            }
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        return location
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     */
    fun stopUsingGPS() {
        if (locationManager != null) try {
            locationManager!!.removeUpdates(this@GPSTracker)
        } catch (ex: SecurityException) {
            ex.printStackTrace()
        }
    }

    /**
     * Function to get latitude
     */
    fun getLatitude(): String {
        if (location != null) {
            latitude = location!!.latitude
        }

        // return latitude
        return java.lang.Double.toString(latitude)
    }

    /**
     * Function to get longitude
     */
    fun getLongitude(): String {
        if (location != null) {
            longitude = location!!.longitude
        }

        // return longitude
        return java.lang.Double.toString(longitude)
    }

    /**
     * Function to check GPS/wifi enabled
     * @return boolean
     */
    fun canGetLocation(): Boolean {
        return canGetLocation
    }

    /**
     * Function to show settings alert dialog
     * On pressing Settings button will launch Settings Options
     */
    fun showSettingsAlert() {
        MaterialDialog(mContext).show {
            title(R.string.enable_gps)
            message(R.string.enable_gps_content)
            positiveButton(R.string.enable) {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                mContext.startActivity(intent)
            }
            negativeButton(android.R.string.cancel) {
                dismiss()
            }
        }
    }

    override fun onLocationChanged(location: Location) {}
    override fun onProviderDisabled(provider: String) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

    companion object {
        // The minimum distance to change Updates in meters
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 10 // 10 meters

        // The minimum time between updates in milliseconds
        private const val MIN_TIME_BW_UPDATES = (1000 * 60 // 1 minute
                ).toLong()
    }
}