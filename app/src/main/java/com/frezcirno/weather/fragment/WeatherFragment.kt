package com.frezcirno.weather.fragment

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.view.*
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.frezcirno.weather.GlobalActivity
import com.frezcirno.weather.R
import com.frezcirno.weather.activity.FirstLaunch
import com.frezcirno.weather.activity.WeatherActivity
import com.frezcirno.weather.databinding.FragmentWeatherBinding
import com.frezcirno.weather.fragment.WeatherFragment.HorizontalAdapter.MyViewHolder
import com.frezcirno.weather.internet.FetchWeather
import com.frezcirno.weather.internet.isNetworkAvailable
import com.frezcirno.weather.model.Info
import com.frezcirno.weather.model.Log.e
import com.frezcirno.weather.model.Snack
import com.frezcirno.weather.model.WeatherFort.WeatherList
import com.frezcirno.weather.permissions.GPSTracker
import com.frezcirno.weather.permissions.Permissions
import com.frezcirno.weather.preferences.Prefs
import com.frezcirno.weather.service.NotificationService
import com.frezcirno.weather.utils.Constants
import com.frezcirno.weather.utils.Utils
import com.google.android.material.snackbar.Snackbar
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt.PromptStateChangeListener
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.math.roundToInt

class WeatherFragment : Fragment() {
    lateinit var weatherFont: Typeface

    private lateinit var binding: FragmentWeatherBinding

    var horizontalLayoutManager: LinearLayoutManager? = null
    var tc = 0.0
    var handler: Handler

    var json: Info? = null
    var citys: String? = null
    lateinit var pd: MaterialDialog
    lateinit var wt: FetchWeather
    var preferences: Prefs? = null
    var gps: GPSTracker? = null
    lateinit var rootView: View
    var permission: Permissions? = null

    init {
        handler = Handler()
    }

    fun setCity(city: String?): WeatherFragment {
        this.citys = city
        return this
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWeatherBinding.inflate(inflater, container, false)
        rootView = binding.root
        pd = MaterialDialog(context()!!)
            .title(R.string.please_wait)
            .message(R.string.loading)
            .cancelable(false)
            .cancelOnTouchOutside(false)
//            .progress()
        setHasOptionsMenu(true)
        preferences = Prefs(context())
        weatherFont = Typeface.createFromAsset(requireContext().assets, "fonts/weather.ttf")
        val bundle = arguments
        val mode: Int
        if (bundle != null) {
            mode = bundle.getInt(Constants.MODE, 0)
        } else {
            mode = 0
        }
        if (mode == 0) updateWeatherData(preferences!!.city, null, null) else updateWeatherData(
            null, preferences!!.latitude.toString(), preferences!!.longitude.toString()
        )
        gps = GPSTracker(context()!!)
        binding.cityField.setTextColor(ContextCompat.getColor((context())!!, R.color.textColor))
        binding.updatedField.setTextColor(ContextCompat.getColor((context())!!, R.color.textColor))
        binding.humidityView.setTextColor(ContextCompat.getColor((context())!!, R.color.textColor))
        binding.sunriseIcon.setTextColor(ContextCompat.getColor((context())!!, R.color.textColor))
        binding.sunriseIcon.setTypeface(weatherFont)
        binding.sunriseIcon.text = activity()!!.getString(R.string.sunrise_icon)
        binding.sunsetIcon.setTextColor(ContextCompat.getColor((context())!!, R.color.textColor))
        binding.sunsetIcon.setTypeface(weatherFont)
        binding.sunsetIcon.text = activity()!!.getString(R.string.sunset_icon)
        binding.humidityIcon.setTextColor(ContextCompat.getColor((context())!!, R.color.textColor))
        binding.humidityIcon.setTypeface(weatherFont)
        binding.humidityIcon.text = activity()!!.getString(R.string.humidity_icon)
        binding.windView.setTextColor(ContextCompat.getColor((context())!!, R.color.textColor))
        binding.swipe.setColorSchemeResources(
            R.color.red,
            R.color.green,
            R.color.blue,
            R.color.yellow,
            R.color.orange
        )
        binding.swipe.setOnRefreshListener {
            handler.post {
                changeCity(preferences!!.city)
                binding.swipe.isRefreshing = false
            }
        }
        horizontalLayoutManager =
            LinearLayoutManager(context(), LinearLayoutManager.HORIZONTAL, false)
        binding.horizontalRecyclerView.layoutManager = horizontalLayoutManager
        binding.horizontalRecyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                if (horizontalLayoutManager!!.findLastVisibleItemPosition() == 9 || citys != null) fab.hide() else fab.show()
            }
        })
        binding.directionView.typeface = weatherFont
        binding.directionView.setTextColor(ContextCompat.getColor((context())!!, R.color.textColor))
        binding.dailyView.text = getString(R.string.daily)
        binding.dailyView.setTextColor(ContextCompat.getColor((context())!!, R.color.textColor))
        binding.sunriseView.setTextColor(ContextCompat.getColor((context())!!, R.color.textColor))
        binding.sunsetView.setTextColor(ContextCompat.getColor((context())!!, R.color.textColor))
        binding.button1.setTextColor(ContextCompat.getColor((context())!!, R.color.textColor))
        pd.show()
        binding.horizontalRecyclerView.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        binding.weatherIcon11.typeface = weatherFont
        binding.weatherIcon11.setTextColor(ContextCompat.getColor((context())!!, R.color.textColor))
        //        if (citys == null)
//            ((WeatherActivity) activity()).showFab();
//        else
//            ((WeatherActivity) activity()).hideFab();
        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (citys == null) activity()!!.menuInflater.inflate(R.menu.menu_weather, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.location -> {
                permission = Permissions(context())
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    Constants.READ_COARSE_LOCATION
                )
            }
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 0) {
            if (!isNetworkAvailable(requireContext())) {
                showNoInternet()
            } else {
                pd.show()
                updateWeatherData(preferences!!.city, null, null)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gps!!.stopUsingGPS()
    }

    private fun updateWeatherData(city: String?, lat: String?, lon: String?) {
        wt = FetchWeather(context())
        if (this.citys == null) handler.postDelayed({
//            fabProgressCircle.show();
        }, 50)
        object : Thread() {
            override fun run() {
                try {
                    if (lat == null && lon == null) {
                        json = wt.execute(if (citys != null) citys else city).get()
                    } else if (city == null) {
                        json = wt.execute(lat, lon).get()
                    }
                } catch (iex: InterruptedException) {
                    e("InterruptedException", "iex")
                } catch (eex: ExecutionException) {
                    e("ExecutionException", "eex")
                }
                if (pd.isShowing) pd.dismiss()
                if (json == null) {
                    preferences!!.city = preferences!!.lastCity
                    handler.post {
                        GlobalActivity.i = 1
                        if (!preferences!!.launched) {
                            FirstStart()
                        } else {
                            //                                if (citys == null)
                            //                                    fabProgressCircle.hide();
                            if (!isNetworkAvailable(requireContext())) {
                                showNoInternet()
                            } else {
                                if (pd.isShowing) pd.dismiss()
                                showInputDialog()
                            }
                        }
                    }
                } else {
                    handler.post {
                        preferences!!.setLaunched()
                        renderWeather(json!!)
                        if (!preferences!!.getv3TargetShown()) showTargets()
                        if (pd.isShowing) pd.dismiss()
                        if (citys == null) {
                            preferences!!.lastCity =
                                json!!.day.name + "," + json!!.day.sys.country
                            (activity() as WeatherActivity?)!!.createShortcuts()
                            progress()
                        } else preferences!!.lastCity = preferences!!.lastCity
                        NotificationService.enqueueWork(
                            context(),
                            Intent(context(), WeatherActivity::class.java)
                        )
                    }
                }
            }
        }.start()
    }

    private fun progress() {
//        fabProgressCircle.onArcAnimationComplete();
        handler.postDelayed({
            //                fabProgressCircle.hide();
        }, 500)
    }

    fun FirstStart() {
        if (pd.isShowing) pd.dismiss()
        val intent = Intent(activity(), FirstLaunch::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    val dailyJson: List<WeatherList>
        get() = json!!.fort.list

    fun changeCity(city: String?) {
        updateWeatherData(city, null, null)
        preferences!!.city = city
    }

    fun changeCity(lat: String?, lon: String?) {
//        ((WeatherActivity) activity()).showFab();
        updateWeatherData(null, lat, lon)
    }

    private fun context(): Context? {
        return context
    }

    private fun activity(): FragmentActivity? {
        return activity
    }

    private fun showInputDialog() {
        MaterialDialog(context()!!).show {
            title(R.string.change_city)
            message(R.string.could_not_find)
            negativeButton(android.R.string.cancel) {
                it.dismiss()
            }
            input(hintRes = R.string.city) { _, text ->
                changeCity(text.toString())
            }
            cancelable(false)
        }
    }

    private fun showTargets() {
        Handler().postDelayed(object : Runnable {
            override fun run() {
                MaterialTapTargetPrompt.Builder((activity())!!)
                    .setTarget(R.id.fab)
                    .setBackgroundColour(
                        ContextCompat.getColor(
                            (context())!!,
                            R.color.md_light_blue_400
                        )
                    )
                    .setFocalColour(ContextCompat.getColor((context())!!, R.color.colorAccent))
                    .setPrimaryText(getString(R.string.target_search_title))
                    .setSecondaryText(getString(R.string.target_search_content))
                    .setIconDrawableColourFilter(
                        ContextCompat.getColor(
                            (context())!!,
                            R.color.md_black_1000
                        )
                    )
                    .setPromptStateChangeListener { prompt, state -> if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_DISMISSING) showRefresh() }
                    .show()
            }
        }, 4500)
    }

    private fun showLocTarget() {
        MaterialTapTargetPrompt.Builder((activity())!!)
            .setTarget(R.id.location)
            .setBackgroundColour(ContextCompat.getColor((context())!!, R.color.md_light_blue_400))
            .setPrimaryText(getString(R.string.location))
            .setFocalColour(ContextCompat.getColor((context())!!, R.color.colorAccent))
            .setSecondaryText(getString(R.string.target_location_content))
            .setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_DISMISSING) preferences!!.setv3TargetShown(
                    true
                )
            }
            .show()
    }

    private fun showRefresh() {
        MaterialTapTargetPrompt.Builder((activity())!!)
            .setTarget(R.id.toolbar)
            .setBackgroundColour(ContextCompat.getColor((context())!!, R.color.md_light_blue_400))
            .setPrimaryText(getString(R.string.target_refresh_title))
            .setFocalColour(ContextCompat.getColor((context())!!, R.color.colorAccent))
            .setSecondaryText(getString(R.string.target_refresh_content))
            .setPromptStateChangeListener { _, state -> if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_DISMISSING) showLocTarget() }
            .show()
    }

    fun showNoInternet() {
        MaterialDialog(context()!!).show {
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
                startActivityForResult(Intent(Settings.ACTION_WIFI_SETTINGS), 0)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            Constants.READ_COARSE_LOCATION -> {

                // If request is cancelled, the result arrays are empty.
                if ((grantResults.size > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    showCity()
                } else {
                    permission!!.permissionDenied()
                }
            }
        }
    }

    private fun showCity() {
        gps = GPSTracker(context()!!)
        if (!gps!!.canGetLocation()) gps!!.showSettingsAlert() else {
            val lat = gps!!.getLatitude()
            val lon = gps!!.getLongitude()
            changeCity(lat, lon)
        }
    }

    private fun renderWeather(jsonObj: Info) {
        try {
            val json0 = jsonObj.day
            val json1 = jsonObj.fort
            tc = json0.main.temp
            preferences!!.latitude = json1.getCity().coord.latitude.toFloat()
            preferences!!.longitude = json1.getCity().coord.longitude.toFloat()
            if (citys == null) preferences!!.city =
                json1.city.name + "," + json0!!.getSys().country
            val a = json0!!.main.temp.roundToInt()
            val city = (json1.city.name.uppercase() + ", " + json1.getCity().country)
            binding.cityField.text = city
            binding.cityField.setOnClickListener { v -> Snack.make(v, city, Snackbar.LENGTH_SHORT) }
            val details: MutableList<WeatherList> = json1.list
            for (i in 0..9) {
                details[i] = json1.list[i]
            }
            val horizontalAdapter = HorizontalAdapter(details)
            binding.horizontalRecyclerView.adapter = horizontalAdapter
            val timeFormat24Hours = preferences!!.isTimeFormat24Hours
            val d1 =
                SimpleDateFormat(if (timeFormat24Hours) "kk:mm" else "hh:mm a", Locale.US).format(
                    Date(json0.sys.sunrise * 1000)
                )
            val d2 =
                SimpleDateFormat(if (timeFormat24Hours) "kk:mm" else "hh:mm a", Locale.US).format(
                    Date(json0.sys.sunset * 1000)
                )
            binding.sunriseView.text = d1
            binding.sunsetView.text = d2
            val df = DateFormat.getDateTimeInstance()
            val updatedOn = "Last update: " + df.format(Date(json0.getDt() * 1000))
            binding.updatedField.text = updatedOn
            val humidity = getString(R.string.humidity_, json0.getMain().humidity)
            val humidity1 = getString(R.string.humidity, json0.getMain().humidity)
            binding.humidityView.text = humidity
            binding.humidityIcon.setOnClickListener {
                Snack.make(
                    rootView,
                    humidity1,
                    Snackbar.LENGTH_SHORT
                )
            }
            binding.humidityView.setOnClickListener {
                Snack.make(
                    rootView,
                    humidity1,
                    Snackbar.LENGTH_SHORT
                )
            }
            val wind = getString(
                R.string.wind,
                json0.getWind().speed,
                if ((PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(
                        Constants.PREF_TEMPERATURE_UNITS,
                        Constants.METRIC
                    ) == Constants.METRIC)
                ) getString(R.string.mps) else getString(R.string.mph)
            )
            val wind1 = getString(
                R.string.wind_,
                json0.getWind().speed,
                if ((PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(
                        Constants.PREF_TEMPERATURE_UNITS,
                        Constants.METRIC
                    ) == Constants.METRIC)
                ) getString(R.string.mps) else getString(R.string.mph)
            )
            binding.windView.text = wind
            binding.directionView.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View) {
                    Snack.make(rootView, wind1, Snackbar.LENGTH_SHORT)
                }
            })
            binding.windView.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View) {
                    Snack.make(rootView, wind1, Snackbar.LENGTH_SHORT)
                }
            })
            binding.weatherIcon11.text =
                Utils.setWeatherIcon(context(), json0.getWeather().get(0).id)
            binding.weatherIcon11.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View) {
                    try {
                        val rs = json0.getWeather()[0].description
                        val strArray = rs.split(" ").toTypedArray()
                        val builder = StringBuilder()
                        for (s: String in strArray) {
                            val cap =
                                s.substring(0, 1).uppercase(Locale.getDefault()) + s.substring(1)
                            builder.append("$cap ")
                        }
                        Snack.make(
                            v,
                            getString(
                                R.string.hey_there_condition,
                                builder.toString().substring(0, builder.length - 1)
                            ),
                            Snackbar.LENGTH_SHORT
                        )
                    } catch (e: Exception) {
                        e("Error", "Main Weather Icon OnClick Details could not be loaded")
                    }
                }
            })
            val r1 = Integer.toString(a) + "°"
            binding.button1.text = r1
            val deg = json0.getWind().direction
            setDeg(deg)
        } catch (e: Exception) {
            e(
                WeatherFragment::class.java.simpleName,
                "One or more fields not found in the JSON data"
            )
        }
    }

    private fun setDeg(deg: Int) {
        val index = Math.abs(Math.round((deg % 360).toFloat()) / 45)
        when (index) {
            0 -> {
                binding.directionView.text = activity()!!.getString(R.string.top)
                setDirection(getString(R.string.north))
            }
            1 -> {
                binding.directionView.text = activity()!!.getString(R.string.top_right)
                setDirection(getString(R.string.north_east))
            }
            2 -> {
                binding.directionView.text = activity()!!.getString(R.string.right)
                setDirection(getString(R.string.east))
            }
            3 -> {
                binding.directionView.text = activity()!!.getString(R.string.bottom_right)
                setDirection(getString(R.string.south_east))
            }
            4 -> {
                binding.directionView.text = activity()!!.getString(R.string.down)
                setDirection(getString(R.string.south))
            }
            5 -> {
                binding.directionView.text = activity()!!.getString(R.string.bottom_left)
                setDirection(getString(R.string.south_west))
            }
            6 -> {
                binding.directionView.text = activity()!!.getString(R.string.left)
                setDirection(getString(R.string.west))
            }
            7 -> {
                binding.directionView.text = activity()!!.getString(R.string.top_left)
                setDirection(getString(R.string.north_west))
            }
        }
    }

    private fun setDirection(string: String) {
        binding.directionView.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                Snack.make(view, getString(R.string.wind_blowing_in, string), Snackbar.LENGTH_SHORT)
            }
        })
    }

    inner class HorizontalAdapter internal constructor(private val horizontalList: List<WeatherList>) :
        RecyclerView.Adapter<MyViewHolder>() {
        inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var weather_icon: TextView
            var details_view: TextView

            init {
                weather_icon = view.findViewById(R.id.weather_icon)
                details_view = view.findViewById(R.id.details_view)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.weather_daily_list_item, parent, false)
            return MyViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.weather_icon.text =
                Utils.setWeatherIcon(context(), horizontalList.get(position).weather.get(0).id)
            val date1 = horizontalList[position].dt
            val expiry = Date(date1 * 1000)
            val date = SimpleDateFormat("EE, dd", Locale(Prefs(context()).language)).format(expiry)
            val line2 = horizontalList[position].temp.max.toString() + "°" + "      "
            val line3 = horizontalList[position].temp.min.toString() + "°"
            val fs = date + "\n" + line2 + line3 + "\n"
            val ss1 = SpannableString(fs)
            ss1.setSpan(
                RelativeSizeSpan(1.1f),
                fs.indexOf(date),
                date.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            ss1.setSpan(RelativeSizeSpan(1.4f), fs.indexOf(line2), date.length + line2.length, 0)
            holder.details_view.text = ss1
            holder.details_view.setOnClickListener {
                val bottomSheetDialogFragment = newInstance(horizontalList[position])
                bottomSheetDialogFragment.show(
                    activity()!!.supportFragmentManager,
                    bottomSheetDialogFragment.getTag()
                )
            }
            holder.weather_icon.setOnClickListener {
                val bottomSheetDialogFragment = newInstance(horizontalList[position])
                bottomSheetDialogFragment.show(
                    activity()!!.supportFragmentManager,
                    bottomSheetDialogFragment.getTag()
                )
            }
            holder.weather_icon.setTextColor(
                ContextCompat.getColor(
                    (context())!!,
                    R.color.textColor
                )
            )
            holder.weather_icon.setTypeface(weatherFont)
            holder.details_view.setTextColor(
                ContextCompat.getColor(
                    (context())!!,
                    R.color.textColor
                )
            )
        }

        override fun getItemCount(): Int {
            return horizontalList.size
        }
    }

    override fun onResume() {
        changeCity(preferences!!.city)
        super.onResume()
    }

    companion object {
        fun newInstance(describable: WeatherList?): CustomBottomSheetDialogFragment {
            val fragment = CustomBottomSheetDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(Constants.DESCRIBABLE_KEY, describable)
            fragment.arguments = bundle
            return fragment
        }
    }
}