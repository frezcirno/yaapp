package com.frezcirno.weather.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.frezcirno.weather.model.DailyResponse;
import com.frezcirno.weather.model.DataResult;
import com.frezcirno.weather.model.ForecastResponse;
import com.frezcirno.weather.preferences.MyPreference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FetchWeather extends AsyncTask<String, Void, DataResult> {
    private final String LOG_TAG = FetchWeather.class.getSimpleName();
    private final MyPreference preferences;
    private final SharedPreferences sharedPreferences;
    private Uri dayUri, forecastUri;

    public FetchWeather(Context context) {
        preferences = new MyPreference(context);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public DataResult doInBackground(String... params) {
        if (params.length == 1)
            byCity(params);
        else
            byCoord(params);
        try {
            DataResult dataResult = new DataResult();
            dataResult.daily = getWeather();
            dataResult.fort = getFort();

            // This value will be 404 if the request was not successful
            if (dataResult.daily.getCod() != 200 | dataResult.fort.getCod() != 200) {
                Log.e(LOG_TAG, "Execution Failed");
                return null;
            }
            return dataResult;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Execution Failed IO");
            e.printStackTrace();
            return null;
        }
    }

    private void byCity(String... params) {
        String UNITS_VALUE = sharedPreferences.getString(Constants.PREF_TEMPERATURE_UNITS, Constants.METRIC);
        System.out.println(UNITS_VALUE);
        dayUri = Uri.parse(Constants.OPEN_WEATHER_MAP_DAILY_API).buildUpon()
                .appendQueryParameter(Constants.QUERY_PARAM, params[0])
                .appendQueryParameter(Constants.FORMAT_PARAM, Constants.FORMAT_VALUE)
                .appendQueryParameter(Constants.UNITS_PARAM, UNITS_VALUE)
                .appendQueryParameter(Constants.DAYS_PARAM, Integer.toString(10))
                .build();
        forecastUri = Uri.parse(Constants.OPEN_WEATHER_MAP_FORECAST_API).buildUpon()
                .appendQueryParameter(Constants.QUERY_PARAM, params[0])
                .appendQueryParameter(Constants.FORMAT_PARAM, Constants.FORMAT_VALUE)
                .appendQueryParameter(Constants.UNITS_PARAM, UNITS_VALUE)
                .appendQueryParameter(Constants.DAYS_PARAM, Integer.toString(10))
                .build();
    }

    private void byCoord(String... params) {
        String UNITS_VALUE = sharedPreferences.getString(Constants.PREF_TEMPERATURE_UNITS, Constants.METRIC);
        dayUri = Uri.parse(Constants.OPEN_WEATHER_MAP_DAILY_API).buildUpon()
                .appendQueryParameter(Constants.LATITUDE, params[0])
                .appendQueryParameter(Constants.LONGITUDE, params[1])
                .appendQueryParameter(Constants.FORMAT_PARAM, Constants.FORMAT_VALUE)
                .appendQueryParameter(Constants.UNITS_PARAM, UNITS_VALUE)
                .appendQueryParameter(Constants.DAYS_PARAM, Integer.toString(10))
                .build();
        forecastUri = Uri.parse(Constants.OPEN_WEATHER_MAP_FORECAST_API).buildUpon()
                .appendQueryParameter(Constants.LATITUDE, params[0])
                .appendQueryParameter(Constants.LONGITUDE, params[1])
                .appendQueryParameter(Constants.FORMAT_PARAM, Constants.FORMAT_VALUE)
                .appendQueryParameter(Constants.UNITS_PARAM, UNITS_VALUE)
                .appendQueryParameter(Constants.DAYS_PARAM, Integer.toString(10))
                .build();
    }

    @Nullable
    private DailyResponse getWeather() throws IOException {
        URL day = new URL(dayUri.toString());
        HttpURLConnection connection1 = (HttpURLConnection) day.openConnection();
        connection1.addRequestProperty("x-api-key", preferences.getWeatherKey());

        InputStream content = connection1.getInputStream();

        try {
            Reader reader = new InputStreamReader(content);
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setDateFormat("M/d/yy hh:mm a");
            Gson gson = gsonBuilder.create();
            DailyResponse dailyResponse = gson.fromJson(reader, DailyResponse.class);
            System.out.println(gson.toJson(dailyResponse));
            content.close();
            return dailyResponse;
        } catch (Exception ex) {
            Log.e("FetchWeather", "Failed to parse JSON due to: " + ex);
        }
        return null;
    }

    @Nullable
    private ForecastResponse getFort() throws IOException {
        URL url = new URL(forecastUri.toString());
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.addRequestProperty("x-api-key", preferences.getWeatherKey());

        InputStream content = httpURLConnection.getInputStream();

        try {
            Reader reader = new InputStreamReader(content);
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setDateFormat("M/d/yy hh:mm a");
            Gson gson = gsonBuilder.create();
            ForecastResponse forecastResponse = gson.fromJson(reader, ForecastResponse.class);
            System.out.println(gson.toJson(forecastResponse));
            content.close();
            return forecastResponse;
        } catch (Exception ex) {
            Log.e("FetchWeather", "Failed to parse JSON due to: " + ex);
        }
        return null;
    }
}
