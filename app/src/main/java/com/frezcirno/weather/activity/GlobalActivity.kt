package com.frezcirno.weather.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.frezcirno.weather.R
import com.frezcirno.weather.preferences.MyPreference

class GlobalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global)
    }

    override fun onResume() {
        val myPreference = MyPreference(this)
        super.onResume()

        // Jump to FirstLaunch Activity or Weather Activity
        val intent = Intent(
            this@GlobalActivity,
            if (myPreference.launched) {
                WeatherActivity::class.java
            } else {
                FirstLaunchActivity::class.java
            }
        )
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    companion object {
        @JvmField
        var i = 0
    }
}