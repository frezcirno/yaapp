package com.frezcirno.weather.activity.settings

import com.frezcirno.weather.app.MyContextWrapper
import com.frezcirno.weather.preferences.Prefs
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.Preference.OnPreferenceChangeListener
import android.preference.Preference
import com.frezcirno.weather.R
import com.frezcirno.weather.service.NotificationService
import android.content.Intent
import com.frezcirno.weather.activity.WeatherActivity
import android.preference.Preference.OnPreferenceClickListener
import com.afollestad.materialdialogs.MaterialDialog
import com.frezcirno.weather.model.Snack
import com.frezcirno.weather.internet.FetchWeather
import android.content.ComponentName
import android.content.Context
import com.frezcirno.weather.preferences.DBHelper
import android.preference.PreferenceManager
import android.preference.ListPreference
import android.provider.Settings
import android.view.MenuItem
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.frezcirno.weather.internet.isNetworkAvailable
import com.frezcirno.weather.utils.Constants
import java.lang.Exception

class SettingsActivity : AppCompatPreferenceActivity() {
    override fun attachBaseContext(newBase: Context) {
        val context: Context = MyContextWrapper.wrap(newBase, Prefs(newBase).language)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar.setDisplayHomeAsUpEnabled(true)

        // load settings fragment
        fragmentManager.beginTransaction().replace(android.R.id.content, MainPreferenceFragment())
            .commit()
    }

    private val isStateChanged: Boolean
        private get() = changed == 1

    class MainPreferenceFragment : PreferenceFragment(), OnPreferenceChangeListener {
        override fun onPreferenceChange(preference: Preference, o: Any): Boolean {
            when (preference.key) {
                Constants.PREF_DISPLAY_LANGUAGE -> {
                    MaterialDialog(activity).show {
                        title(R.string.restart_app)
                        message(R.string.restart_app_content)
                        positiveButton(android.R.string.ok) {}
                    }
                }
            }
            return true
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_main)

            // gallery EditText change listener
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_refresh_interval)))

            // notification preference change listener
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_units)))
            findPreference(Constants.PREF_ENABLE_NOTIFS).onPreferenceChangeListener =
                OnPreferenceChangeListener { preference, o ->
                    NotificationService.enqueueWork(
                        activity,
                        Intent(activity, WeatherActivity::class.java)
                    )
                    true
                }
            findPreference(Constants.PREF_DISPLAY_LANGUAGE).onPreferenceChangeListener =
                OnPreferenceChangeListener { preference, o ->
                    MaterialDialog(activity).show {
                        title(R.string.restart_app)
                        message(R.string.restart_app_content)
                        positiveButton(android.R.string.ok) {}
                    }
                    true
                }
            findPreference(Constants.PREF_OWM_KEY).onPreferenceClickListener =
                OnPreferenceClickListener {
                    MaterialDialog(activity).show {
                        title(R.string.pref_owm_key_title)
                        message(R.string.pref_owm_key_summary)
                        positiveButton(R.string.reset) {
                            Prefs(activity).weatherKey = Constants.OWM_APP_ID
                        }
                        negativeButton(android.R.string.cancel) {
                            dismiss()
                        }
                        val input1 = input(
                            prefill = Prefs(activity).weatherKey,
                            waitForPositiveButton = false
                        ) { dialog, text ->
                            val input = text.toString()
                            if (input.isEmpty()) {
                                Snack.make(
                                    activity.findViewById(android.R.id.content),
                                    "Please enter a valid key", Snack.LENGTH_SHORT
                                )
                                dialog.getActionButton(WhichButton.POSITIVE).isEnabled = false
                            } else {
                                try {
                                    if (isNetworkAvailable(activity) && Prefs(activity).weatherKey != input
                                    ) {
                                        Prefs(activity).weatherKey = input
                                        val info =
                                            FetchWeather(activity).execute(Prefs(activity).city)
                                                .get()
                                        if (info.day.cod != 200L) {
                                            MaterialDialog(activity).show {
                                                title(R.string.unable_fetch)
                                                message(R.string.unable_fetch_content)
                                                positiveButton(android.R.string.ok) {
                                                    dismiss()
                                                }
                                            }
                                            Prefs(activity).weatherKey = Constants.OWM_APP_ID
                                        }
                                    } else {
                                        MaterialDialog(activity).show {
                                            title(R.string.no_internet_title)
                                            message(R.string.no_internet_content)
                                            cancelable(false)
                                            positiveButton(R.string.no_internet_mobile_data) {
                                                val intent = Intent()
                                                intent.component = ComponentName(
                                                    "com.android.settings",
                                                    "com.android.settings.Settings\$DataUsageSummaryActivity"
                                                )
                                                startActivityForResult(intent, 0)
                                            }
                                            negativeButton(R.string.no_internet_wifi) {
                                                startActivityForResult(
                                                    Intent(Settings.ACTION_WIFI_SETTINGS),
                                                    0
                                                )
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                dialog.getActionButton(WhichButton.POSITIVE).isEnabled = true
                            }
                        }
                    }
                    true
                }
            findPreference(Constants.PREF_DELETE_CITIES).onPreferenceClickListener =
                OnPreferenceClickListener {
                    MaterialDialog(activity).show {
                        title(R.string.pref_delete_cities_title)
                        message(R.string.pref_delete_cities_summary)
                        listItemsMultiChoice(items = DBHelper(activity).cities) { dialog, index, text ->
                            for (i in index) {
                                DBHelper(activity).deleteCity(text[i].toString())
                            }
                        }
                        positiveButton(android.R.string.ok)
                    }
                    true
                }
            findPreference(Constants.PREF_DISPLAY_LANGUAGE).onPreferenceChangeListener = this
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (isStateChanged) startActivity(Intent(this, WeatherActivity::class.java)) else finish()
    }

    companion object {
        private var changed = 0
        private fun bindPreferenceSummaryToValue(preference: Preference) {
            preference.onPreferenceChangeListener =
                sBindPreferenceSummaryToValueListener
            sBindPreferenceSummaryToValueListener.onPreferenceChange(
                preference,
                PreferenceManager
                    .getDefaultSharedPreferences(preference.context)
                    .getString(preference.key, "")
            )
        }

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private val sBindPreferenceSummaryToValueListener =
            OnPreferenceChangeListener { preference, newValue ->
                val stringValue = newValue.toString()
                if (preference is ListPreference) {
                    // For list preferences, look up the correct display value in
                    // the preference's 'entries' list.
                    val listPreference = preference
                    val index = listPreference.findIndexOfValue(stringValue)

                    // Set the summary to reflect the new value.
                    preference.setSummary(
                        if (index >= 0) listPreference.entries[index] else null
                    )
                } else {
                    preference.summary = stringValue
                }
                true
            }
    }
}