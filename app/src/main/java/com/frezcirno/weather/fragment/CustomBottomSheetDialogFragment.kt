package com.frezcirno.weather.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.preference.PreferenceManager
import com.frezcirno.weather.R
import com.frezcirno.weather.model.ForecastDay
import com.frezcirno.weather.preferences.MyPreference
import com.frezcirno.weather.utils.Constants
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.*

class CustomBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private val mBottomSheetBehaviorCallback: BottomSheetCallback = object : BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }
    private lateinit var windText: TextView
    private lateinit var rainText: TextView
    private lateinit var snowText: TextView
    private lateinit var humidityText: TextView
    private lateinit var pressureText: TextView
    private lateinit var nightValue: TextView
    private lateinit var mornValue: TextView
    private lateinit var dayValue: TextView
    private lateinit var eveValue: TextView
    private lateinit var condition: TextView
    private lateinit var rootView: View
    private lateinit var weatherFont: Typeface
    private lateinit var forecastDay: ForecastDay
    private lateinit var myPreference: MyPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        forecastDay = requireArguments().getSerializable(DESCRIBABLE_KEY) as ForecastDay
        weatherFont = Typeface.createFromAsset(requireActivity().assets, "fonts/weather.ttf")
        myPreference = MyPreference(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.dialog_modal, container, false)
        condition = rootView.findViewById(R.id.description)
        nightValue = rootView.findViewById(R.id.night_temperature)
        mornValue = rootView.findViewById(R.id.morning_temperature)
        dayValue = rootView.findViewById(R.id.day_temperature)
        eveValue = rootView.findViewById(R.id.evening_temperature)
        val windIcon = rootView.findViewById(R.id.wind_icon) as TextView
        windIcon.typeface = weatherFont
        windIcon.text = getString(R.string.speed_icon)
        val rainIcon = rootView.findViewById(R.id.rain_icon) as TextView
        rainIcon.typeface = weatherFont
        rainIcon.text = getString(R.string.rain)
        val snowIcon = rootView.findViewById(R.id.snow_icon) as TextView
        snowIcon.typeface = weatherFont
        snowIcon.text = getString(R.string.snow)
        val humidityIcon = rootView.findViewById(R.id.humidity_icon) as TextView
        humidityIcon.typeface = weatherFont
        humidityIcon.text = getString(R.string.humidity_icon)
        val pressureIcon = rootView.findViewById(R.id.pressure_icon) as TextView
        pressureIcon.typeface = weatherFont
        pressureIcon.text = getString(R.string.pressure_icon)
        windText = rootView.findViewById(R.id.wind)
        rainText = rootView.findViewById(R.id.rain)
        snowText = rootView.findViewById(R.id.snow)
        humidityText = rootView.findViewById(R.id.humidity)
        pressureText = rootView.findViewById(R.id.pressure)
        updateElements()
        return rootView
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        //super.setupDialog(dialog, style);
        val contentView = View.inflate(context, R.layout.dialog_modal, null)
        dialog.setContentView(contentView)
        val layoutParams =
            (contentView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
        val behavior = layoutParams.behavior
        if (behavior is BottomSheetBehavior<*>) {
            behavior.setBottomSheetCallback(mBottomSheetBehaviorCallback)
        }
    }

    private fun updateElements() {
        setCondition()
        setOthers()
        setTemperatures()
    }

    private fun setCondition() {
        val cond = forecastDay.weather[0].description
        val strArray = cond.split(" ").toTypedArray()
        val builder = StringBuilder()
        for (s in strArray) {
            val cap = s.substring(0, 1).uppercase(Locale.getDefault()) + s.substring(1)
            builder.append("$cap ")
        }
        condition.text = builder.toString()
    }

    private fun setOthers() {
        try {
            val wind = getString(
                R.string.wind_,
                forecastDay.speed,
                if (PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(
                        Constants.PREF_TEMPERATURE_UNITS, Constants.METRIC
                    ) == Constants.IMPERIAL
                ) getString(R.string.mph) else getString(R.string.mps)
            )
            windText.text = wind
            try {
                rainText.text =
                    getString(R.string.rain_, getString(R.string.bottom_rain), forecastDay.rain)
            } catch (ex: Exception) {
                rainText.text = getString(R.string.rain_, getString(R.string.bottom_rain), 0)
            }
            try {
                snowText.text = getString(
                    R.string.snow_,
                    forecastDay.snow,
                    if (myPreference.units == "metric") requireContext().getString(R.string.mps) else requireContext().getString(
                        R.string.mph
                    )
                )
            } catch (ex: Exception) {
                snowText.text = getString(
                    R.string.snow_,
                    0,
                    if (myPreference.units == "metric") requireContext().getString(R.string.mps) else requireContext().getString(
                        R.string.mph
                    )
                )
            }
            humidityText.text = getString(R.string.humidity, forecastDay.humidity)
            pressureText.text = getString(R.string.pressure, forecastDay.pressure)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun setTemperatures() {
        dayValue.text = String.format(
            "%.1f" + (if (myPreference.units == Constants.METRIC) "°C" else "°F"),
            forecastDay.temp.day
        )
        mornValue.text = String.format(
            "%.1f" + (if (myPreference.units == Constants.METRIC) "°C" else "°F"),
            forecastDay.temp.morn
        )
        eveValue.text = String.format(
            "%.1f" + (if (myPreference.units == Constants.METRIC) "°C" else "°F"),
            forecastDay.temp.eve
        )
        nightValue.text = String.format(
            "%.1f" + (if (myPreference.units == Constants.METRIC) "°C" else "°F"),
            forecastDay.temp.night
        )
    }

    companion object {
        private const val DESCRIBABLE_KEY = Constants.DESCRIBABLE_KEY
    }
}