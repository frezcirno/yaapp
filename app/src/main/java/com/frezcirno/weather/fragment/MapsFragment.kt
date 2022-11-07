package com.frezcirno.weather.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import com.frezcirno.weather.R
import com.frezcirno.weather.preferences.MyPreference
import com.google.android.material.bottomnavigation.BottomNavigationView

class MapsFragment : Fragment() {
    private lateinit var rootView: View
    private lateinit var webView: WebView
    private lateinit var mBottomBar: BottomNavigationView
    private lateinit var myPreference: MyPreference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_maps, container, false)
        webView = rootView.findViewById(R.id.webView)
        myPreference = MyPreference(requireContext())
        //        ((WeatherActivity) getActivity()).hideFab();
        loadMap()
        return rootView
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadMap() {
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("file:///android_asset/map.html?lat=${myPreference.latitude}&lon=${myPreference.longitude}&k=2.0&appid=${myPreference.weatherKey}")
        webView.setInitialScale(1)
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        mBottomBar = rootView.findViewById(R.id.bottomBar)
        mBottomBar.setOnItemSelectedListener { item: MenuItem ->
            if (item.itemId == R.id.map_rain) {
                webView.loadUrl("javascript:map.removeLayer(windLayer);map.removeLayer(tempLayer);map.addLayer(rainLayer);")
            } else if (item.itemId == R.id.map_wind) {
                webView.loadUrl("javascript:map.removeLayer(rainLayer);map.removeLayer(tempLayer);map.addLayer(windLayer);")
            } else if (item.itemId == R.id.map_temperature) {
                webView.loadUrl("javascript:map.removeLayer(windLayer);map.removeLayer(rainLayer);map.addLayer(tempLayer);")
            }
            true
        }
    }
}