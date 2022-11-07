package com.frezcirno.weather.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper.getMainLooper
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.frezcirno.weather.R
import com.frezcirno.weather.activity.FirstLaunchActivity
import com.frezcirno.weather.activity.GlobalActivity
import com.frezcirno.weather.activity.WeatherActivity
import com.frezcirno.weather.databinding.FragmentWeatherBinding
import com.frezcirno.weather.fragment.WeatherFragment.HorizontalAdapter.MyViewHolder
import com.frezcirno.weather.internet.isNetworkAvailable
import com.frezcirno.weather.model.DataResult
import com.frezcirno.weather.model.ForecastDay
import com.frezcirno.weather.permissions.GPSTracker
import com.frezcirno.weather.permissions.Permissions
import com.frezcirno.weather.preferences.MyPreference
import com.frezcirno.weather.utils.Constants
import com.frezcirno.weather.utils.FetchWeather
import com.frezcirno.weather.utils.Snack
import com.frezcirno.weather.utils.Utils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.math.abs
import kotlin.math.roundToInt


class WeatherFragment : Fragment() {
    private lateinit var binding: FragmentWeatherBinding
    private lateinit var handler: Handler
    private lateinit var materialDialog: MaterialDialog
    private lateinit var horizontalRecyclerView: RecyclerView
    private lateinit var rootView: View
    private lateinit var fab: FloatingActionButton
    lateinit var weatherFont: Typeface
    lateinit var myPreference: MyPreference

    var json: DataResult? = null
    var city: String? = null
    var gps: GPSTracker? = null
    var permission: Permissions? = null

    fun setCity(city: String): WeatherFragment {
        this.city = city
        return this
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentWeatherBinding.inflate(inflater, container, false)
        rootView = binding.root
        handler = Handler(getMainLooper())

        setHasOptionsMenu(true)

        myPreference = MyPreference(requireContext())
        weatherFont = Typeface.createFromAsset(requireContext().assets, "fonts/weather.ttf")

        val mode = arguments?.getInt(Constants.MODE)
        if (mode == 0) {
            updateWeatherData(myPreference.city, null, null)
        } else {
            updateWeatherData(
                null, myPreference.latitude.toString(), myPreference.longitude.toString()
            )
        }

        binding.cityField.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColor))
        binding.updatedField.setTextColor(
            ContextCompat.getColor(
                requireContext(), R.color.textColor
            )
        )
        binding.humidityView.setTextColor(
            ContextCompat.getColor(
                requireContext(), R.color.textColor
            )
        )
        binding.sunriseIcon.setTextColor(
            ContextCompat.getColor(
                requireContext(), R.color.textColor
            )
        )
        binding.sunriseIcon.typeface = weatherFont
        binding.sunriseIcon.text = requireActivity().getString(R.string.sunrise_icon)
        binding.sunsetIcon.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColor))
        binding.sunsetIcon.typeface = weatherFont
        binding.sunsetIcon.text = requireActivity().getString(R.string.sunset_icon)
        binding.humidityIcon.setTextColor(
            ContextCompat.getColor(
                requireContext(), R.color.textColor
            )
        )
        binding.humidityIcon.typeface = weatherFont
        binding.humidityIcon.text = requireActivity().getString(R.string.humidity_icon)
        binding.windView.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColor))
        binding.swipe.setColorSchemeResources(
            R.color.red, R.color.green, R.color.blue, R.color.yellow, R.color.orange
        )
        binding.swipe.setOnRefreshListener {
            handler.post {
                changeCity(myPreference.city)
                binding.swipe.isRefreshing = false
            }
        }
        horizontalRecyclerView = rootView.findViewById(R.id.horizontal_recycler_view)

        fab = (activity as WeatherActivity).findViewById(R.id.fab)

        val horizontalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        horizontalRecyclerView.layoutManager = horizontalLayoutManager
        horizontalRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (horizontalLayoutManager.findLastVisibleItemPosition() == 9 || city != null)
                    fab.hide()
                else
                    fab.show()
            }
        })
        binding.directionView.typeface = weatherFont
        binding.directionView.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.textColor)
        )

        binding.dailyView.text = getString(R.string.daily)
        binding.dailyView.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColor))

        binding.sunriseView.setTextColor(
            ContextCompat.getColor(
                requireContext(), R.color.textColor
            )
        )
        binding.sunsetView.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColor))

        binding.iconButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColor))

        materialDialog =
            MaterialDialog(requireContext()).title(R.string.please_wait).message(R.string.loading)
                .cancelable(false).cancelOnTouchOutside(false)
//            .progress()
        materialDialog.show()

        horizontalRecyclerView.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        binding.weatherIcon11.typeface = weatherFont
        binding.weatherIcon11.setTextColor(
            ContextCompat.getColor(
                requireContext(), R.color.textColor
            )
        )

        if (city == null)
            (requireActivity() as WeatherActivity).showFab()
        else
            (requireActivity() as WeatherActivity).hideFab()

        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (city == null) requireActivity().menuInflater.inflate(R.menu.menu_weather, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.location -> {
                permission = Permissions(requireContext())
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
                materialDialog.show()
                updateWeatherData(myPreference.city, null, null)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gps?.stopUsingGPS()
    }

    private fun updateWeatherData(city: String?, lat: String?, lon: String?) {
        val fetchWeather = FetchWeather(requireContext())
        Thread {
            try {
                if (lat == null && lon == null) {
                    json = fetchWeather.execute(if (this.city != null) this.city else city).get()
                } else if (city == null) {
                    json = fetchWeather.execute(lat, lon).get()
                }
            } catch (iex: InterruptedException) {
                Log.e("InterruptedException", "iex")
            } catch (eex: ExecutionException) {
                Log.e("ExecutionException", "eex")
            }
            if (materialDialog.isShowing) materialDialog.dismiss()
            if (json == null) {
                myPreference.city = myPreference.lastCity
                handler.post {
                    GlobalActivity.i = 1
                    if (!myPreference.launched) {
                        firstStart()
                    } else {
                        if (!isNetworkAvailable(requireContext())) {
                            showNoInternet()
                        } else {
                            if (materialDialog.isShowing) materialDialog.dismiss()
                            showInputDialog()
                        }
                    }
                }
                return@Thread
            }

            handler.post {
                myPreference.setLaunched()
                renderWeather(json!!)
                if (!myPreference.getv3TargetShown()) showTargets()
                if (materialDialog.isShowing) materialDialog.dismiss()
                if (this.city == null) {
                    myPreference.lastCity = json!!.daily.name + "," + json!!.daily.sys.country
                } else {
                    myPreference.lastCity = myPreference.lastCity
                }
            }
        }.start()
    }

    private fun firstStart() {
        if (materialDialog.isShowing) materialDialog.dismiss()
        val intent = Intent(requireActivity(), FirstLaunchActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    val dailyJson: List<ForecastDay>
        get() = json!!.fort.list

    fun changeCity(city: String?) {
        updateWeatherData(city, null, null)
        myPreference.city = city
    }

    private fun changeCity(lat: String?, lon: String?) {
        (requireActivity() as WeatherActivity).showFab()
        updateWeatherData(null, lat, lon)
    }

    @SuppressLint("CheckResult")
    private fun showInputDialog() {
        MaterialDialog(requireContext()).show {
            title(R.string.change_city)
            message(R.string.could_not_find)
            negativeButton(android.R.string.cancel) {
                it.dismiss()
            }
            input { _, text ->
                changeCity(text.toString())
            }
            cancelable(false)
        }
    }

    private fun showTargets() {
        handler.postDelayed({
            MaterialTapTargetPrompt.Builder(requireActivity()).setTarget(R.id.fab)
                .setBackgroundColour(
                    ContextCompat.getColor(
                        requireContext(), R.color.md_light_blue_400
                    )
                )
                .setFocalColour(ContextCompat.getColor(requireContext(), R.color.colorAccent))
                .setPrimaryText(getString(R.string.target_search_title))
                .setSecondaryText(getString(R.string.target_search_content))
                .setIconDrawableColourFilter(
                    ContextCompat.getColor(
                        requireContext(), R.color.md_black_1000
                    )
                )
                .setPromptStateChangeListener { prompt, state -> if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_DISMISSING) showRefresh() }
                .show()
        }, 4500)
    }

    private fun showLocTarget() {
        MaterialTapTargetPrompt.Builder(requireActivity())
            .setTarget(R.id.location)
            .setBackgroundColour(
                ContextCompat.getColor(requireContext(), R.color.md_light_blue_400)
            )
            .setPrimaryText(getString(R.string.location))
            .setFocalColour(ContextCompat.getColor(requireContext(), R.color.colorAccent))
            .setSecondaryText(getString(R.string.target_location_content))
            .setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_DISMISSING)
                    myPreference.setv3TargetShown(true)
            }.show()
    }

    private fun showRefresh() {
        MaterialTapTargetPrompt.Builder(requireActivity()).setTarget(R.id.toolbar)
            .setBackgroundColour(
                ContextCompat.getColor(
                    requireContext(), R.color.md_light_blue_400
                )
            ).setPrimaryText(getString(R.string.target_refresh_title))
            .setFocalColour(ContextCompat.getColor(requireContext(), R.color.colorAccent))
            .setSecondaryText(getString(R.string.target_refresh_content))
            .setPromptStateChangeListener { _, state -> if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_DISMISSING) showLocTarget() }
            .show()
    }

    private fun showNoInternet() {
        MaterialDialog(requireContext()).show {
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
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            Constants.READ_COARSE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    showCity()
                } else {
                    permission!!.permissionDenied()
                }
            }
        }
    }

    private fun showCity() {
        gps = GPSTracker(requireContext())
        if (!gps!!.canGetLocation) gps!!.showSettingsAlert()
        else {
            val lat = gps!!.getLatitude()
            val lon = gps!!.getLongitude()
            changeCity(lat, lon)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun renderWeather(jsonObj: DataResult) {
        try {
            val daily = jsonObj.daily
            val fort = jsonObj.fort
            myPreference.latitude = fort.city.coord.latitude.toFloat()
            myPreference.longitude = fort.city.coord.longitude.toFloat()
            if (city == null) myPreference.city = fort.city.name + "," + daily.sys.country

            val city = (fort.city.name.uppercase() + ", " + fort.city.country)
            binding.cityField.text = city
            binding.cityField.setOnClickListener { v -> Snack.make(v, city, Snackbar.LENGTH_SHORT) }

            val details: List<ForecastDay> = fort.list
            val horizontalAdapter = HorizontalAdapter(details)
            horizontalRecyclerView.adapter = horizontalAdapter

            val timeFormat24Hours = myPreference.isTimeFormat24Hours
            binding.sunriseView.text =
                SimpleDateFormat(if (timeFormat24Hours) "kk:mm" else "hh:mm a", Locale.CHINA).format(
                    Date(daily.sys.sunrise * 1000)
                )
            binding.sunriseView.setOnClickListener {
                Snack.make(
                    rootView, "日出时间：" + binding.sunriseView.text, Snackbar.LENGTH_SHORT
                )
            }
            binding.sunriseIcon.setOnClickListener {
                Snack.make(
                    rootView, "日出时间：" + binding.sunriseView.text, Snackbar.LENGTH_SHORT
                )
            }
            binding.sunsetView.text =
                SimpleDateFormat(if (timeFormat24Hours) "kk:mm" else "hh:mm a", Locale.CHINA).format(
                    Date(daily.sys.sunset * 1000)
                )
            binding.sunsetView.setOnClickListener {
                Snack.make(
                    rootView, "日落时间：" + binding.sunriseView.text, Snackbar.LENGTH_SHORT
                )
            }
            binding.sunsetIcon.setOnClickListener {
                Snack.make(
                    rootView, "日落时间：" + binding.sunriseView.text, Snackbar.LENGTH_SHORT
                )
            }

            val df = DateFormat.getDateTimeInstance()
            val updatedOn = "上次更新: " + df.format(Date(daily.dt * 1000L))
            binding.updatedField.text = updatedOn


            val humidity1 = getString(R.string.humidity, daily.main.getHumidityInt())
            binding.humidityView.text = getString(R.string.humidity_, daily.main.getHumidityInt())
            binding.humidityIcon.setOnClickListener {
                Snack.make(
                    rootView, humidity1, Snackbar.LENGTH_SHORT
                )
            }
            binding.humidityView.setOnClickListener {
                Snack.make(
                    rootView, humidity1, Snackbar.LENGTH_SHORT
                )
            }

            val wind = getString(
                R.string.wind,
                daily.wind.speed,
                if ((PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(
                        Constants.PREF_TEMPERATURE_UNITS, Constants.METRIC
                    ) == Constants.METRIC)
                ) getString(R.string.mps) else getString(R.string.mph)
            )
            val wind1 = getString(
                R.string.wind_,
                daily.wind.speed,
                if ((PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(
                        Constants.PREF_TEMPERATURE_UNITS, Constants.METRIC
                    ) == Constants.METRIC)
                ) getString(R.string.mps) else getString(R.string.mph)
            )
            binding.windView.text = wind
            binding.windView.setOnClickListener {
                Snack.make(
                    rootView, wind1, Snackbar.LENGTH_SHORT
                )
            }
            binding.directionView.setOnClickListener {
                Snack.make(
                    rootView, wind1, Snackbar.LENGTH_SHORT
                )
            }

            binding.weatherIcon11.text = Utils.setWeatherIcon(requireContext(), daily.weather.get(0).id)
            binding.weatherIcon11.setOnClickListener { v ->
                try {
                    val rs = daily.weather[0].description
                    val strArray = rs.split(" ").toTypedArray()
                    val builder = StringBuilder()
                    for (s: String in strArray) {
                        val cap = s.substring(0, 1).uppercase(Locale.getDefault()) + s.substring(1)
                        builder.append("$cap ")
                    }
                    Snack.make(
                        v, getString(
                            R.string.hey_there_condition,
                            builder.toString().substring(0, builder.length - 1)
                        ), Snackbar.LENGTH_SHORT
                    )
                } catch (e: Exception) {
                    Log.e("Error", "Main Weather Icon OnClick Details could not be loaded")
                }
            }

            val a = json!!.daily.main.temp.roundToInt()
            binding.iconButton.text =
                "$a${if (myPreference.units == Constants.METRIC) "°C" else "°F"}"

            setDeg(daily.wind.getDirectionInt())
        } catch (e: Exception) {
            Log.e(
                WeatherFragment::class.java.simpleName,
                "One or more fields not found in the JSON data"
            )
        }
    }

    private fun setDeg(deg: Int) {
        val index = abs(Math.round((deg % 360).toFloat()) / 45)
        when (index) {
            0 -> {
                binding.directionView.text = requireActivity().getString(R.string.top)
                setDirection(getString(R.string.north))
            }
            1 -> {
                binding.directionView.text = requireActivity().getString(R.string.top_right)
                setDirection(getString(R.string.north_east))
            }
            2 -> {
                binding.directionView.text = requireActivity().getString(R.string.right)
                setDirection(getString(R.string.east))
            }
            3 -> {
                binding.directionView.text = requireActivity().getString(R.string.bottom_right)
                setDirection(getString(R.string.south_east))
            }
            4 -> {
                binding.directionView.text = requireActivity().getString(R.string.down)
                setDirection(getString(R.string.south))
            }
            5 -> {
                binding.directionView.text = requireActivity().getString(R.string.bottom_left)
                setDirection(getString(R.string.south_west))
            }
            6 -> {
                binding.directionView.text = requireActivity().getString(R.string.left)
                setDirection(getString(R.string.west))
            }
            7 -> {
                binding.directionView.text = requireActivity().getString(R.string.top_left)
                setDirection(getString(R.string.north_west))
            }
        }
    }

    private fun setDirection(string: String) {
        binding.directionView.setOnClickListener { view ->
            Snack.make(
                view, getString(R.string.wind_blowing_in, string), Snackbar.LENGTH_SHORT
            )
        }
    }

    inner class HorizontalAdapter internal constructor(private val horizontalList: List<ForecastDay>) :
        RecyclerView.Adapter<MyViewHolder>() {
        inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var weatherIcon: TextView
            var detailsView: TextView

            init {
                weatherIcon = view.findViewById(R.id.weather_icon)
                detailsView = view.findViewById(R.id.details_view)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.weather_daily_list_item, parent, false)
            return MyViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.weatherIcon.text = Utils.setWeatherIcon(
                requireContext(), horizontalList[position].weather[0].id
            )
            val date1 = horizontalList[position].dt
            val expiry = Date(date1 * 1000L)
            val date = SimpleDateFormat("EE, dd", Locale(myPreference.language)).format(expiry)
            val maxTemp = String.format(
                "%.1f", horizontalList[position].temp.max
            ) + (if (myPreference.units == Constants.METRIC) "°C" else "°F") + "    "
            val minTemp = String.format(
                "%.1f", horizontalList[position].temp.min
            ) + (if (myPreference.units == Constants.METRIC) "°C" else "°F")
            val fs = date + "\n" + maxTemp + minTemp + "\n"
            val ss1 = SpannableString(fs)
            ss1.setSpan(
                RelativeSizeSpan(1.1f),
                fs.indexOf(date),
                date.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            ss1.setSpan(
                RelativeSizeSpan(1.4f), fs.indexOf(maxTemp), date.length + maxTemp.length, 0
            )
            holder.detailsView.text = ss1
            holder.detailsView.setOnClickListener {
                val bottomSheetDialogFragment = newInstance(horizontalList[position])
                bottomSheetDialogFragment.show(
                    requireActivity().supportFragmentManager, bottomSheetDialogFragment.tag
                )
            }
            holder.weatherIcon.setOnClickListener {
                val bottomSheetDialogFragment = newInstance(horizontalList[position])
                bottomSheetDialogFragment.show(
                    requireActivity().supportFragmentManager, bottomSheetDialogFragment.tag
                )
            }
            holder.weatherIcon.setTextColor(
                ContextCompat.getColor(
                    requireContext(), R.color.textColor
                )
            )
            holder.weatherIcon.typeface = weatherFont
            holder.detailsView.setTextColor(
                ContextCompat.getColor(
                    requireContext(), R.color.textColor
                )
            )
        }

        override fun getItemCount(): Int {
            return horizontalList.size
        }
    }

    override fun onResume() {
        changeCity(myPreference.city)
        super.onResume()
    }

    companion object {
        fun newInstance(describable: ForecastDay?): CustomBottomSheetDialogFragment {
            val fragment = CustomBottomSheetDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(Constants.DESCRIBABLE_KEY, describable)
            fragment.arguments = bundle
            return fragment
        }
    }
}