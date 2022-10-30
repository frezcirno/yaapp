package com.frezcirno.weather.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.frezcirno.weather.R
import com.frezcirno.weather.cards.AboutAdapter
import com.frezcirno.weather.databinding.ActivityAboutBinding
import com.frezcirno.weather.model.AboutModel

class AboutActivity : AppCompatActivity() {
    lateinit var binding: ActivityAboutBinding
    private var cards: ArrayList<AboutModel> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_about)
        binding = ActivityAboutBinding.inflate(layoutInflater)

        setSupportActionBar(binding.toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        cards.add(AboutModel(0))
        cards.add(AboutModel(1))
        cards.add(AboutModel(2))
        cards.add(AboutModel(3))
        cards.add(AboutModel(4))
        binding.aboutList.setHasFixedSize(true)
        binding.aboutList.layoutManager = LinearLayoutManager(this)
        binding.aboutList.adapter = AboutAdapter(cards)
    }
}