package com.frezcirno.weather.activity

import android.content.Context
import com.frezcirno.weather.app.MyContextWrapper
import com.frezcirno.weather.preferences.Prefs
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.frezcirno.weather.R
import com.frezcirno.weather.fragment.FirstLaunchFragment

class FirstLaunch : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val context: Context = MyContextWrapper.wrap(newBase, Prefs(newBase).language)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_launch)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment, FirstLaunchFragment())
            .commit()
        setSupportActionBar(toolbar)
    }
}