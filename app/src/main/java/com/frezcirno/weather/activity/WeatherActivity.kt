package com.frezcirno.weather.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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
import com.frezcirno.weather.internet.FetchWeather
import com.frezcirno.weather.model.Info
import com.frezcirno.weather.model.Log.i
import com.frezcirno.weather.model.WeatherFort.WeatherList
import com.frezcirno.weather.preferences.DBHelper
import com.frezcirno.weather.preferences.Prefs
import com.frezcirno.weather.service.NotificationService
import com.frezcirno.weather.utils.Constants
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IProfile
import com.mikepenz.weather_icons_typeface_library.WeatherIcons
import shortbread.Shortbread
import shortbread.Shortcut
import java.util.concurrent.ExecutionException

class WeatherActivity : AppCompatActivity() {
    lateinit var preferences: Prefs
    var wf = WeatherFragment()
    var gf = GraphsFragment()

    lateinit var binding: ActivityWeatherBinding

    //MapsFragment mf;
    var toolbar: Toolbar? = null
    var drawer: Drawer? = null
    var mManager: NotificationManagerCompat? = null
    var handler: Handler = Handler()
    var dbHelper: DBHelper? = null
    var f: Fragment? = null
    var mode = 0

    @Shortcut(id = "home", icon = R.drawable.shortcut_home, shortLabel = "Home", rank = 2)
    fun addWeather() {
    }

    @Shortcut(
        id = "graphs",
        icon = R.drawable.shortcut_graph,
        shortLabel = "Weather Graphs",
        rank = 1
    )
    fun addGraphs() {
        handler.postDelayed({
            drawer!!.setSelectionAtPosition(2)
            val graphsFragment = newGraphInstance(
                ArrayList(
                    wf.dailyJson
                )
            )
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment, graphsFragment)
                .commit()
        }, 750)
    }

    /*@Shortcut(id = "maps", icon = R.drawable.shortcut_map, shortLabel = "Weather Maps")
    public void addMaps() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                drawer.setSelectionAtPosition(3);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment, mf)
                        .commit();
            }
        }, 750);
    }*/
    override fun attachBaseContext(newBase: Context) {
        val context: Context = MyContextWrapper.wrap(newBase, Prefs(newBase).language)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeatherBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_weather)
        preferences = Prefs(this)
        i("Activity", WeatherActivity::class.java.simpleName)
        mManager = NotificationManagerCompat.from(this)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        binding.fab.setOnClickListener { showInputDialog() }
        binding.fab.show()
        val intent = intent
        val bundle = Bundle()
        bundle.putInt("mode", intent.getIntExtra(Constants.MODE, 0))
        //mf = new MapsFragment();
        dbHelper = DBHelper(this)
        wf.arguments = bundle
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment, wf)
            .commit()
        initDrawer()
        NotificationService.enqueueWork(this, Intent(this, WeatherActivity::class.java))
    }

    @SuppressLint("CheckResult")
    private fun showInputDialog() {
        MaterialDialog(this).show {
            title(R.string.change_city)
            message(R.string.enter_zip_code)
            positiveButton { binding.fab.show() }
            onDismiss { binding.fab.show() }
            input { dialog, text ->
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
            positiveButton { binding.fab.show() }
            onDismiss { binding.fab.show() }
            input { dialog, text ->
                checkForCity(text.toString())
            }
            negativeButton(android.R.string.cancel) { dialog ->
                dialog.dismiss()
            }
        }
    }

    var json: Info? = null
    var i = 5
    private fun checkForCity(city: String) {
        val wt = FetchWeather(this)
        val context: Context = this
        object : Thread() {
            override fun run() {
                try {
                    json = wt.execute(city).get()
                } catch (ex: InterruptedException) {
                    ex.printStackTrace()
                } catch (ex: ExecutionException) {
                    ex.printStackTrace()
                }
                if (json == null) {
                    handler.post {
                        MaterialDialog(context).show {
                            title(R.string.city_not_found)
                            message(R.string.city_not_found)
                            positiveButton { dialog ->
                                dialog.dismiss()
                            }
                            negativeButton(android.R.string.ok) { dialog ->
                                dialog.dismiss()
                            }
                        }
                    }
                } else {
                    if (dbHelper!!.cityExists(json!!.day.name + "," + json!!.day.sys.country)) {
                        handler.post {
                            MaterialDialog(context).show {
                                title(R.string.city_already_exists)
                                message(R.string.need_not_add)
                                negativeButton(android.R.string.ok) { dialog ->
                                    dialog.dismiss()
                                }
                            }
                        }
                    } else {
                        dbHelper!!.addCity(json!!.day.name + "," + json!!.day.sys.country)
                        handler.post {
                            val itemx = SecondaryDrawerItem().withName(
                                json!!.day.name + "," + json!!.day.sys.country
                            )
                                .withIcon(IconicsDrawable(context).icon(GoogleMaterial.Icon.gmd_place))
                                .withOnDrawerItemClickListener(object :
                                    Drawer.OnDrawerItemClickListener {
                                    override fun onItemClick(
                                        view: View,
                                        position: Int,
                                        drawerItem: IDrawerItem<*, *>?
                                    ): Boolean {
                                        if (f !is WeatherFragment) {
                                            wf =
                                                WeatherFragment().setCity(json!!.day.name + "," + json!!.day.sys.country)
                                            supportFragmentManager.beginTransaction()
                                                .replace(R.id.fragment, wf)
                                                .commit()
                                        }
                                        return true
                                    }
                                })
                            drawer!!.addItemAtPosition(itemx, ++i)
                        }
                    }
                }
            }
        }.start()
    }

    fun createShortcuts() {
        if (mode == 0) {
            Shortbread.create(this)
            mode = -1
        }
    }

    private fun changeCity(city: String?) {
        val wf = supportFragmentManager
            .findFragmentById(R.id.fragment) as WeatherFragment?
        wf!!.changeCity(city)
        Prefs(this).city = city
    }

    private fun initDrawer() {
        val profile: IProfile<*> = ProfileDrawerItem().withName(getString(R.string.app_name))
            .withEmail("Version : " + BuildConfig.VERSION_NAME)
            .withIcon(R.mipmap.ic_launcher_x)
        val headerResult = AccountHeaderBuilder()
            .withActivity(this)
            .withHeaderBackground(R.drawable.header)
            .withTextColor(ContextCompat.getColor(this, R.color.md_amber_400))
            .addProfiles(
                profile
            )
            .withSelectionListEnabled(false)
            .withProfileImagesClickable(false)
            .build()
        val item1 = SecondaryDrawerItem().withName(R.string.drawer_item_home)
            .withIcon(
                IconicsDrawable(this)
                    .icon(WeatherIcons.Icon.wic_day_sunny)
            )
            .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
                override fun onItemClick(
                    view: View,
                    position: Int,
                    drawerItem: IDrawerItem<*, *>?
                ): Boolean {
                    wf = WeatherFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment, wf)
                        .commit()
                    return true
                }
            })
        val item2 = SecondaryDrawerItem().withName(R.string.drawer_item_graph)
            .withIcon(
                IconicsDrawable(this)
                    .icon(GoogleMaterial.Icon.gmd_trending_up)
            )
            .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
                override fun onItemClick(
                    view: View,
                    position: Int,
                    drawerItem: IDrawerItem<*, *>?
                ): Boolean {
                    if (f !is GraphsFragment) {
                        val graphsFragment = newGraphInstance(
                            ArrayList(
                                wf.dailyJson
                            )
                        )
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment, graphsFragment)
                            .commit()
                    }
                    return true
                }
            })
        val item3 = SecondaryDrawerItem().withName(R.string.drawer_item_map)
            .withIcon(
                IconicsDrawable(this)
                    .icon(GoogleMaterial.Icon.gmd_map)
            )
            .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
                override fun onItemClick(
                    view: View,
                    position: Int,
                    drawerItem: IDrawerItem<*, *>?
                ): Boolean {
                    if (f !is MapsFragment) {
                        val mapsFragment = MapsFragment()
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment, mapsFragment)
                            .commit()
                    }
                    return true
                }
            })
        val item4 = SecondaryDrawerItem().withName(R.string.drawer_item_add_city)
            .withIcon(
                IconicsDrawable(this)
                    .icon(GoogleMaterial.Icon.gmd_add_location)
            )
            .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
                override fun onItemClick(
                    view: View,
                    position: Int,
                    drawerItem: IDrawerItem<*, *>?
                ): Boolean {
                    showCityDialog()
                    return true
                }
            })
            .withSelectable(false)
        val item9 = SecondaryDrawerItem().withName(R.string.settings)
            .withIcon(
                IconicsDrawable(this)
                    .icon(GoogleMaterial.Icon.gmd_settings)
            )
            .withSelectable(false)
            .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
                override fun onItemClick(
                    view: View,
                    position: Int,
                    drawerItem: IDrawerItem<*, *>?
                ): Boolean {
                    startActivity(Intent(this@WeatherActivity, SettingsActivity::class.java))
                    return true
                }
            })
        val drawerBuilder = DrawerBuilder()
        drawerBuilder
            .withActivity(this)
            .withToolbar(toolbar!!)
            .withTranslucentStatusBar(true)
            .withAccountHeader(headerResult)
            .withActionBarDrawerToggleAnimated(true)
            .addDrawerItems(
                item1,
                item2,
                item3,
                DividerDrawerItem(),
                item4
            )
            .addStickyDrawerItems(
                item9
            )
        val cities = dbHelper!!.cities
        val listIterator: ListIterator<String> = cities.listIterator(cities.size)
        while (listIterator.hasPrevious()) {
            val city = listIterator.previous()
            drawerBuilder.addDrawerItems(
                SecondaryDrawerItem().withName(city)
                    .withIcon(
                        IconicsDrawable(this)
                            .icon(GoogleMaterial.Icon.gmd_place)
                    )
                    .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
                        override fun onItemClick(
                            view: View,
                            position: Int,
                            drawerItem: IDrawerItem<*, *>?
                        ): Boolean {
                            wf = WeatherFragment().setCity(city)
                            supportFragmentManager.beginTransaction()
                                .replace(R.id.fragment, wf)
                                .commit()
                            return true
                        }
                    })
            )
        }
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
        private val DESCRIBABLE_KEY = "describable_key"
        fun newGraphInstance(describable: ArrayList<WeatherList?>?): GraphsFragment {
            val fragment = GraphsFragment()
            val bundle = Bundle()
            bundle.putSerializable(DESCRIBABLE_KEY, describable)
            fragment.arguments = bundle
            return fragment
        }
    }
}