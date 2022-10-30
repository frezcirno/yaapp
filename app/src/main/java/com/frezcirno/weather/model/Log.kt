package com.frezcirno.weather.model

import android.util.Log
import com.frezcirno.weather.BuildConfig

object Log {
    @JvmStatic
    fun i(tag: String?, message: String?) {
        if (BuildConfig.DEBUG) Log.i(tag, message!!)
    }

    @JvmStatic
    fun i(tag: String?, message: String?, r: Throwable?) {
        if (BuildConfig.DEBUG) Log.i(tag, message, r)
    }

    @JvmStatic
    fun d(tag: String?, message: String?) {
        if (BuildConfig.DEBUG) Log.i(tag, message!!)
    }

    @JvmStatic
    fun d(tag: String?, message: String?, r: Throwable?) {
        if (BuildConfig.DEBUG) Log.i(tag, message, r)
    }

    @JvmStatic
    fun e(tag: String?, message: String?) {
        if (BuildConfig.DEBUG) Log.i(tag, message!!)
    }

    @JvmStatic
    fun e(tag: String?, message: String?, r: Throwable?) {
        if (BuildConfig.DEBUG) Log.i(tag, message, r)
    }
}