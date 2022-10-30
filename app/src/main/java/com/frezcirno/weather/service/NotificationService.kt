package com.frezcirno.weather.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.preference.PreferenceManager
import androidx.core.app.JobIntentService
import com.frezcirno.weather.R
import com.frezcirno.weather.activity.WeatherActivity
import com.frezcirno.weather.internet.Request
import com.frezcirno.weather.internet.isNetworkAvailable
import com.frezcirno.weather.model.Log.e
import com.frezcirno.weather.model.Log.i
import com.frezcirno.weather.model.WeatherInfo
import com.frezcirno.weather.preferences.Prefs
import com.frezcirno.weather.service.NotificationService
import com.frezcirno.weather.utils.Constants
import java.io.IOException

class NotificationService : JobIntentService() {
    var prefs: Prefs? = null
    var builder: Notification.Builder? = null
    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    override fun onHandleWork(intent: Intent) {
        if (!isNetworkAvailable(this)) {
            return
        }
        i("In", "Notification Service Alarm")
        val intent = newIntent(this)
        val pendingIntent = PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        prefs = Prefs(this)
        val intervalMillis = prefs!!.time.toLong()
        if (Prefs(this).notifs) {
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                intervalMillis,
                pendingIntent
            )
        } else {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        val city = prefs!!.city
        val units = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(Constants.PREF_TEMPERATURE_UNITS, Constants.METRIC)
        try {
            val weather = Request(this).getItems(city, units)
            if (Prefs(this).notifs) weatherNotification(weather!!)
        } catch (e: IOException) {
            e(TAG, "Error get weather", e)
        }
    }

    private fun weatherNotification(weather: WeatherInfo) {
        val intent = Intent(this, WeatherActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val temperatureScale = if (PreferenceManager.getDefaultSharedPreferences(this).getString(
                Constants.PREF_TEMPERATURE_UNITS, Constants.METRIC
            ) == Constants.METRIC
        ) getString(R.string.c) else getString(R.string.f)
        val speedScale = if (PreferenceManager.getDefaultSharedPreferences(this).getString(
                Constants.PREF_TEMPERATURE_UNITS, Constants.METRIC
            ) == Constants.METRIC
        ) getString(R.string.mps) else getString(R.string.mph)
        val temperature = getString(
            R.string.temperature,
            getString(R.string.pref_temp_header),
            weather.main.temp,
            temperatureScale
        )
        val city = getString(R.string.city, weather.name + ", " + weather.sys.country)
        val wind = getString(R.string.wind_, weather.wind.speed, speedScale)
        val humidity = getString(R.string.humidity, weather.main.humidity)
        val pressure = getString(R.string.pressure, weather.main.pressure)
        val data = """
            $city
            $temperature
            $wind
            $humidity
            $pressure
            """.trimIndent()
        val notificationManager = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val id = "w01"
            val name = "Weather"
            val importance = NotificationManager.IMPORTANCE_MIN
            val desc = "Enable Hourly Weather Notifications"
            val channel = NotificationChannel(id, name, importance)
            channel.description = desc
            notificationManager.createNotificationChannel(channel)
            builder = Notification.Builder(this, id)
        } else builder = Notification.Builder(this)
        builder!!.setAutoCancel(false)
        builder!!.setContentTitle(
            Math.round(weather.main.temp).toString() + temperatureScale + " at " + weather.name
        )
        builder!!.setContentText(data)
        builder!!.style = Notification.BigTextStyle().bigText(data)
        builder!!.setSmallIcon(R.drawable.ic_notification_icon)
        builder!!.setContentIntent(pendingIntent)
        builder!!.setColor(Color.RED)
        val notification = builder!!.build()
        notificationManager.notify(0, notification)
    }

    companion object {
        private const val TAG = "NotificationsService"

        @JvmStatic
        fun enqueueWork(context: Context?, intent: Intent?) {
            enqueueWork(context!!, NotificationService::class.java, 0x01, intent!!)
        }

        fun newIntent(context: Context?): Intent {
            i("Trigger", "newIntent")
            return Intent(context, NotificationService::class.java)
        }
    }
}