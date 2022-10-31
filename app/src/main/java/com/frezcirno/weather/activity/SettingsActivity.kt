package com.frezcirno.weather.activity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceFragmentCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.frezcirno.weather.R
import com.frezcirno.weather.internet.FetchWeather
import com.frezcirno.weather.internet.isNetworkAvailable
import com.frezcirno.weather.model.Log
import com.frezcirno.weather.model.Snack
import com.frezcirno.weather.preferences.DBHelper
import com.frezcirno.weather.preferences.Prefs
import com.frezcirno.weather.service.NotificationService
import com.frezcirno.weather.utils.Constants

class SettingsActivity : AppCompatActivity() {
    private val isStateChanged: Boolean
        get() = changed == 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, MySettingsFragment()).commit()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.i(
                    "SettingsActivity",
                    "Back button pressed" + if (isStateChanged) " with changes" else ""
                )
                if (isStateChanged) {
                    startActivity(
                        Intent(this@SettingsActivity, WeatherActivity::class.java)
                    )
                } else {
                    finish()
                }
            }
        })
    }

    class MySettingsFragment : PreferenceFragmentCompat() {

        private lateinit var pref: Prefs

        @SuppressLint("CheckResult")
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_main)
            pref = Prefs(requireContext())
            findPreference<Preference>(Constants.PREF_ENABLE_NOTIFS)?.onPreferenceChangeListener =
                OnPreferenceChangeListener { _, _ ->
                    NotificationService.enqueueWork(
                        activity, Intent(activity, WeatherActivity::class.java)
                    )
                    true
                }
            findPreference<Preference>(Constants.PREF_DISPLAY_LANGUAGE)?.onPreferenceChangeListener =
                OnPreferenceChangeListener { _, _ ->
                    MaterialDialog(requireActivity()).show {
                        title(R.string.restart_app)
                        message(R.string.restart_app_content)
                        positiveButton(android.R.string.ok)
                    }
                    true
                }
            findPreference<Preference>(Constants.PREF_DELETE_CITIES)?.onPreferenceClickListener =
                OnPreferenceClickListener {
                    MaterialDialog(requireActivity()).show {
                        title(R.string.pref_delete_cities_title)
                        message(R.string.pref_delete_cities_summary)
                        listItemsMultiChoice(items = DBHelper(activity).cities) { _, index, text ->
                            for (i in index) {
                                changed = 1
                                DBHelper(activity).deleteCity(text[i].toString())
                            }
                        }
                        positiveButton(android.R.string.ok)
                    }
                    true
                }
            findPreference<Preference>(Constants.PREF_OWM_KEY)?.onPreferenceClickListener =
                OnPreferenceClickListener {
                    MaterialDialog(requireActivity()).show {
                        title(R.string.pref_owm_key_title)
                        message(R.string.pref_owm_key_summary)
                        positiveButton(R.string.reset) {
                            pref.weatherKey = Constants.OWM_APP_ID
                        }
                        negativeButton(android.R.string.cancel) {
                            dismiss()
                        }
                        input(
                            prefill = pref.weatherKey, waitForPositiveButton = false
                        ) { dialog, text ->
                            val input = text.toString()
                            if (input.isEmpty()) {
                                Snack.make(
                                    requireActivity().findViewById(android.R.id.content),
                                    "Please enter a valid key",
                                    Snack.LENGTH_SHORT
                                )
                                dialog.getActionButton(WhichButton.POSITIVE).isEnabled = false
                                return@input
                            }

                            try {
                                if (isNetworkAvailable(requireActivity()) && pref.weatherKey != input) {
                                    pref.weatherKey = input
                                    val info = FetchWeather(activity).execute(pref.city).get()
                                    if (info.day.cod != 200L) {
                                        MaterialDialog(requireActivity()).show {
                                            title(R.string.unable_fetch)
                                            message(R.string.unable_fetch_content)
                                            positiveButton(android.R.string.ok) {
                                                dismiss()
                                            }
                                        }
                                        pref.weatherKey = Constants.OWM_APP_ID
                                    }
                                } else {
                                    MaterialDialog(requireActivity()).show {
                                        title(R.string.no_internet_title)
                                        message(R.string.no_internet_content)
                                        cancelable(false)
                                        positiveButton(R.string.no_internet_mobile_data) {
                                            val intent = Intent()
                                            intent.component = ComponentName(
                                                "com.android.settings",
                                                "com.android.settings.Settings\$DataUsageSummaryActivity"
                                            )
                                            val startActivity = registerForActivityResult(
                                                ActivityResultContracts.StartActivityForResult()
                                            ) {}
                                            startActivity.launch(intent)
                                        }
                                        negativeButton(R.string.no_internet_wifi) {
                                            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                                            val startActivity = registerForActivityResult(
                                                ActivityResultContracts.StartActivityForResult()
                                            ) {}
                                            startActivity.launch(intent)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            dialog.getActionButton(WhichButton.POSITIVE).isEnabled = true
                        }
                    }
                    true
                }
            findPreference<Preference>(Constants.PREF_DELETE_CITIES)?.onPreferenceChangeListener =
                OnPreferenceChangeListener { preference, newValue ->
                    onPreferenceChange(preference, newValue)
                }
        }

        private fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
            val stringValue = newValue.toString()
            if (preference is ListPreference) {
                val prefIndex = preference.findIndexOfValue(stringValue)
                if (prefIndex >= 0) {
                    preference.setSummary(preference.entries[prefIndex])
                }
            } else {
                preference.summary = stringValue
            }
            return true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private var changed = 0
    }
}