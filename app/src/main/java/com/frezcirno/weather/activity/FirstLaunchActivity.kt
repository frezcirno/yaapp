package com.frezcirno.weather.activity

import android.content.Context
import com.frezcirno.weather.app.MyContextWrapper
import com.frezcirno.weather.preferences.MyPreference
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.frezcirno.weather.R
import com.frezcirno.weather.fragment.FirstLaunchFragment

class FirstLaunchActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val context: Context = MyContextWrapper.wrap(newBase, MyPreference(newBase).language)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_launch)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportFragmentManager.beginTransaction()
            .add(R.id.fragment, FirstLaunchFragment())
            .commit()
    }
}