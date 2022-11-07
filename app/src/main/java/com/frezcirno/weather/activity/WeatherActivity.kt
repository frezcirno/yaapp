package com.frezcirno.weather.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.input.input
import com.frezcirno.weather.BuildConfig
import com.frezcirno.weather.R
import com.frezcirno.weather.app.MyContextWrapper
import com.frezcirno.weather.databinding.ActivityWeatherBinding
import com.frezcirno.weather.fragment.GraphsFragment
import com.frezcirno.weather.fragment.MapsFragment
import com.frezcirno.weather.fragment.WeatherFragment
import com.frezcirno.weather.model.DataResult
import com.frezcirno.weather.model.ForecastDay
import com.frezcirno.weather.preferences.DBHelper
import com.frezcirno.weather.preferences.MyPreference
import com.frezcirno.weather.utils.Constants
import com.frezcirno.weather.utils.FetchWeather
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IProfile
import com.mikepenz.weather_icons_typeface_library.WeatherIcons
import java.util.concurrent.ExecutionException

class WeatherActivity : AppCompatActivity() {
    lateinit var preferences: MyPreference
    lateinit var toolbar: Toolbar
    lateinit var fab: FloatingActionButton
    lateinit var handler: Handler
    lateinit var dbHelper: DBHelper

    var wf: WeatherFragment? = null
    var drawer: Drawer? = null
    var jsonInfo: DataResult? = null
    var i = 5

    override fun attachBaseContext(newBase: Context) {
        val context: Context = MyContextWrapper.wrap(newBase, MyPreference(newBase).language)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        dbHelper = DBHelper(this)
        preferences = MyPreference(this)
        handler = Handler(Looper.getMainLooper())

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            showInputDialog()
        }
        fab.show()

        val bundle = Bundle()
        bundle.putInt("mode", intent.getIntExtra(Constants.MODE, 0))

        wf = WeatherFragment()
        wf!!.arguments = bundle
        supportFragmentManager.beginTransaction().replace(R.id.fragment, wf!!).commit()

        initDrawer()
    }

    public fun showFab() {
        fab.show()
    }

    public fun hideFab() {
        fab.hide()
    }

    @SuppressLint("CheckResult")
    private fun showInputDialog() {
        MaterialDialog(this).show {
            title(R.string.change_city)
            message(R.string.enter_zip_code)
            positiveButton { fab.show() }
            onDismiss { fab.show() }
            input { _, text ->
                changeCity(text.toString())
            }
            negativeButton(android.R.string.cancel) { dialog ->
                dialog.dismiss()
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun showCityDialog() {
        MaterialDialog(this).show {
            title(R.string.drawer_item_add_city)
            message(R.string.pref_add_city_content)
            positiveButton { fab.show() }
            onDismiss { fab.show() }
            input { _, text ->
                addNewCity(text.toString())
            }
            negativeButton(android.R.string.cancel) { dialog ->
                dialog.dismiss()
            }
        }
    }

    private fun addNewCity(city: String) {
        val context: Context = this
        val fetchWeather = FetchWeather(context)
        Thread {
            try {
                jsonInfo = fetchWeather.execute(city).get()
            } catch (ex: InterruptedException) {
                ex.printStackTrace()
            } catch (ex: ExecutionException) {
                ex.printStackTrace()
            }

            if (jsonInfo == null) {
                handler.post {
                    MaterialDialog(context).show {
                        title(R.string.city_not_found)
                        message(R.string.city_not_found)
                        positiveButton { dialog ->
                            dialog.dismiss()
                        }
                    }
                }
                return@Thread
            }

            if (dbHelper.cityExists(jsonInfo!!.daily.name + "," + jsonInfo!!.daily.sys.country)) {
                handler.post {
                    MaterialDialog(context).show {
                        title(R.string.city_already_exists)
                        message(R.string.need_not_add)
                        positiveButton(android.R.string.ok) { dialog ->
                            dialog.dismiss()
                        }
                    }
                }
                return@Thread
            }

            dbHelper.addCity(jsonInfo!!.daily.name + "," + jsonInfo!!.daily.sys.country)
            handler.post {
                val itemx = SecondaryDrawerItem()
                    .withName(jsonInfo!!.daily.name + "," + jsonInfo!!.daily.sys.country)
                    .withIcon(IconicsDrawable(context).icon(GoogleMaterial.Icon.gmd_place))
                    .withOnDrawerItemClickListener { _, _, _ ->
                        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment)
                        if (currentFragment !is WeatherFragment ||
                            currentFragment.city != (jsonInfo!!.daily.name + "," + jsonInfo!!.daily.sys.country)
                        ) {
                            wf =
                                WeatherFragment().setCity(jsonInfo!!.daily.name + "," + jsonInfo!!.daily.sys.country)
                            supportFragmentManager.beginTransaction().replace(R.id.fragment, wf!!).commit()
                        }
                        true
                    }
                drawer!!.addItemAtPosition(itemx, ++i)
            }
        }.start()
    }

    private fun changeCity(city: String) {
        val weatherFragment =
            supportFragmentManager.findFragmentById(R.id.fragment) as WeatherFragment
        weatherFragment.changeCity(city)
    }

    private fun initDrawer() {
        val profile: IProfile<*> = ProfileDrawerItem().withName(getString(R.string.app_name))
            .withEmail("Version : " + BuildConfig.VERSION_NAME).withIcon(R.mipmap.ic_launcher_x)

        val headerResult =
            AccountHeaderBuilder().withActivity(this).withHeaderBackground(R.drawable.header)
                .withTextColor(ContextCompat.getColor(this, R.color.md_amber_400)).addProfiles(
                    profile
                ).withSelectionListEnabled(false).withProfileImagesClickable(false).build()

        val home = SecondaryDrawerItem()
            .withName(R.string.drawer_item_home)
            .withIcon(IconicsDrawable(this).icon(WeatherIcons.Icon.wic_day_sunny))
            .withOnDrawerItemClickListener { _, _, _ ->
                wf = WeatherFragment()
                supportFragmentManager.beginTransaction().replace(R.id.fragment, wf!!).commit()
                true
            }

        val weatherGraphs = SecondaryDrawerItem().withName(R.string.drawer_item_graph).withIcon(
            IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_trending_up)
        ).withOnDrawerItemClickListener { _, _, _ ->
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment)
            if (currentFragment !is GraphsFragment) {
                val graphsFragment = newGraphInstance(ArrayList(wf!!.dailyJson))
                supportFragmentManager.beginTransaction().replace(R.id.fragment, graphsFragment)
                    .commit()
            }
            true
        }

        val weatherMap = SecondaryDrawerItem().withName(R.string.drawer_item_map).withIcon(
            IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_map)
        ).withOnDrawerItemClickListener { _, _, _ ->
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment)
            if (currentFragment !is MapsFragment) {
                val mapsFragment = MapsFragment()
                supportFragmentManager.beginTransaction().replace(R.id.fragment, mapsFragment)
                    .commit()
            }
            true
        }

        val addCity = SecondaryDrawerItem()
            .withName(R.string.drawer_item_add_city)
            .withIcon(IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_add_location))
            .withOnDrawerItemClickListener { _, _, _ ->
                showCityDialog()
                true
            }
            .withSelectable(false)

        val settings = SecondaryDrawerItem()
            .withName(R.string.settings)
            .withIcon(IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_settings))
            .withSelectable(false)
            .withOnDrawerItemClickListener { _, _, _ ->
                startActivity(Intent(this@WeatherActivity, SettingsActivity::class.java))
                true
            }

        val drawerBuilder = DrawerBuilder()
        drawerBuilder
            .withActivity(this)
            .withToolbar(toolbar)
            .withTranslucentStatusBar(true)
            .withAccountHeader(headerResult)
            .withActionBarDrawerToggleAnimated(true)
            .addDrawerItems(home, weatherGraphs, weatherMap, DividerDrawerItem())
            .addStickyDrawerItems(settings)

        val cities = dbHelper.cities
        val listIterator: ListIterator<String> = cities.listIterator(cities.size)
        while (listIterator.hasPrevious()) {
            val city = listIterator.previous()
            drawerBuilder.addDrawerItems(SecondaryDrawerItem().withName(city).withIcon(
                IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_place)
            ).withOnDrawerItemClickListener { _, _, _ ->
                val wf = WeatherFragment().setCity(city)
                supportFragmentManager.beginTransaction().replace(R.id.fragment, wf).commit()
                true
            })
        }

        drawerBuilder.addDrawerItems(addCity)

        drawer = drawerBuilder.build()
    }

    override fun onBackPressed() {
        if (drawer!!.isDrawerOpen) {
            drawer!!.closeDrawer()
        } else {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
    }

    companion object {
        private const val DESCRIBABLE_KEY = "describable_key"
        fun newGraphInstance(describable: ArrayList<ForecastDay?>?): GraphsFragment {
            val bundle = Bundle()
            bundle.putSerializable(DESCRIBABLE_KEY, describable)
            val fragment = GraphsFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}