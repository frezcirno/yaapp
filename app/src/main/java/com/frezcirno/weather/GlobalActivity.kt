package com.frezcirno.weather

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.frezcirno.weather.activity.FirstLaunch
import com.frezcirno.weather.activity.WeatherActivity
import com.frezcirno.weather.model.Log
import com.frezcirno.weather.preferences.Preferences
import com.frezcirno.weather.preferences.Prefs

class GlobalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global)
        Log.i("Loaded", "Global")
    }

    override fun onResume() {
        cp = Preferences(this)
        prefs = Prefs(this)
        super.onResume()
        if (!cp!!.prefs.getBoolean("first", true)) {
            prefs!!.setLaunched()
            prefs!!.city = cp!!.city
        }
        super.onResume()
        val intent = if (prefs!!.launched) {
            Intent(this@GlobalActivity, WeatherActivity::class.java)
        } else {
            Intent(this@GlobalActivity, FirstLaunch::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    companion object {
        var cp: Preferences? = null
        var prefs: Prefs? = null

        @JvmField
        var i = 0
    }
}