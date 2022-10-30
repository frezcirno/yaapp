package com.frezcirno.weather.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;

import com.frezcirno.weather.R;
import com.frezcirno.weather.activity.WeatherActivity;
import com.frezcirno.weather.preferences.Prefs;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MapsFragment extends Fragment {

    public View rootView;
    WebView webView;
    BottomNavigationView mBottomBar;
    Prefs prefs;

    public MapsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_maps, container, false);
        webView = (WebView) rootView.findViewById(R.id.webView);
        prefs = new Prefs(getContext());
//        ((WeatherActivity) getActivity()).hideFab();
        loadMap();
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public void loadMap() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("file:///android_asset/map.html?lat=" + prefs.getLatitude() + "&lon=" + prefs.getLongitude() + "&k=2.0" + "&appid=" + prefs.getWeatherKey());
        webView.setInitialScale(1);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        mBottomBar = rootView.findViewById(R.id.bottomBar);
        mBottomBar.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.map_rain) {
                webView.loadUrl("javascript:map.removeLayer(windLayer);map.removeLayer(tempLayer);map.addLayer(rainLayer);");
            } else if (item.getItemId() == R.id.map_wind) {
                webView.loadUrl("javascript:map.removeLayer(rainLayer);map.removeLayer(tempLayer);map.addLayer(windLayer);");
            } else if (item.getItemId() == R.id.map_temperature) {
                webView.loadUrl("javascript:map.removeLayer(windLayer);map.removeLayer(rainLayer);map.addLayer(tempLayer);");
            }
            return true;
        });
    }
}