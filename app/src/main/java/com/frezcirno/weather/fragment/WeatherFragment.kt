package com.frezcirno.weather.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
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
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.math.abs
import kotlin.math.roundToInt

class WeatherFragment : Fragment() {
    lateinit var weatherFont: Typeface

    private lateinit var binding: FragmentWeatherBinding

    var horizontalLayoutManager: LinearLayoutManager? = null
    private var tc = 0.0
    var handler: Handler = Handler()

    var json: Info? = null
    var citys: String? = null
    lateinit var pd: MaterialDialog
    lateinit var wt: FetchWeather
    lateinit var prefs: Prefs
    var gps: GPSTracker? = null
    lateinit var rootView: View
    var permission: Permissions? = null
    lateinit var horizontalRecyclerView: RecyclerView

    fun setCity(city: String?): WeatherFragment {
        this.citys = city
        return this
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentWeatherBinding.inflate(inflater, container, false)
        rootView = binding.root
        pd = MaterialDialog(requireContext())
            .title(R.string.please_wait)
            .message(R.string.loading)
            .cancelable(false)
            .cancelOnTouchOutside(false)
//            .progress()
        setHasOptionsMenu(true)
        prefs = Prefs(requireContext())
        weatherFont = Typeface.createFromAsset(requireContext().assets, "fonts/weather.ttf")
        val bundle = arguments
        val mode = bundle?.getInt(Constants.MODE)
        if (mode == 0)
            updateWeatherData(prefs.city, null, null)
        else
            updateWeatherData(null, prefs.latitude.toString(), prefs.longitude.toString())
        binding.cityField.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColor))
        binding.updatedField.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.textColor
            )
        )
        binding.humidityView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.textColor
            )
        )
        binding.sunriseIcon.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.textColor
            )
        )
        binding.sunriseIcon.typeface = weatherFont
        binding.sunriseIcon.text = requireActivity().getString(R.string.sunrise_icon)
        binding.sunsetIcon.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColor))
        binding.sunsetIcon.typeface = weatherFont
        binding.sunsetIcon.text = requireActivity().getString(R.string.sunset_icon)
        binding.humidityIcon.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.textColor
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
                changeCity(prefs.city)
                binding.swipe.isRefreshing = false
            }
        }
        horizontalRecyclerView = rootView.findViewById(R.id.horizontal_recycler_view)
        horizontalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        horizontalRecyclerView.layoutManager = horizontalLayoutManager
        horizontalRecyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                if (horizontalLayoutManager!!.findLastVisibleItemPosition() == 9 || citys != null) fab.hide() else fab.show()
            }
        })
        binding.directionView.typeface = weatherFont
        binding.directionView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.textColor
            )
        )
        binding.dailyView.text = getString(R.string.daily)
        binding.dailyView.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColor))
        binding.sunriseView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.textColor
            )
        )
        binding.sunsetView.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColor))
        binding.button1.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColor))
        pd.show()
        horizontalRecyclerView.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        binding.weatherIcon11.typeface = weatherFont
        binding.weatherIcon11.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.textColor
            )
        )
        //        if (citys == null)
//            ((WeatherActivity) requireActivity()).showFab();
//        else
//            ((WeatherActivity) requireActivity()).hideFab();
        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (citys == null) requireActivity().menuInflater.inflate(R.menu.menu_weather, menu)
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
                pd.show()
                updateWeatherData(prefs.city, null, null)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gps?.stopUsingGPS()
    }

    private fun updateWeatherData(city: String?, lat: String?, lon: String?) {
        wt = FetchWeather(requireContext())
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
                    prefs.city = prefs.lastCity
                    handler.post {
                        GlobalActivity.i = 1
                        if (!prefs.launched) {
                            firstStart()
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
                        prefs.setLaunched()
                        renderWeather(json!!)
                        if (!prefs.getv3TargetShown()) showTargets()
                        if (pd.isShowing) pd.dismiss()
                        if (citys == null) {
                            prefs.lastCity = json!!.day.name + "," + json!!.day.sys.country
                            (requireActivity() as WeatherActivity?)!!.createShortcuts()
                            progress()
                        } else prefs.lastCity = prefs.lastCity
                        NotificationService.enqueueWork(
                            requireContext(), Intent(requireContext(), WeatherActivity::class.java)
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

    fun firstStart() {
        if (pd.isShowing) pd.dismiss()
        val intent = Intent(requireActivity(), FirstLaunch::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    val dailyJson: List<WeatherList>
        get() = json!!.fort.list

    fun changeCity(city: String?) {
        updateWeatherData(city, null, null)
        prefs.city = city
    }

    fun changeCity(lat: String?, lon: String?) {
//        ((WeatherActivity) requireActivity()).showFab();
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
        Handler().postDelayed({
            MaterialTapTargetPrompt.Builder(requireActivity()).setTarget(R.id.fab)
                .setBackgroundColour(ContextCompat.getColor(requireContext(), R.color.md_light_blue_400))
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
        MaterialTapTargetPrompt.Builder(requireActivity()).setTarget(R.id.location)
            .setBackgroundColour(
                ContextCompat.getColor(requireContext(), R.color.md_light_blue_400)
            )
            .setPrimaryText(getString(R.string.location))
            .setFocalColour(ContextCompat.getColor(requireContext(), R.color.colorAccent))
            .setSecondaryText(getString(R.string.target_location_content))
            .setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_DISMISSING) prefs.setv3TargetShown(
                    true
                )
            }.show()
    }

    private fun showRefresh() {
        MaterialTapTargetPrompt.Builder(requireActivity()).setTarget(R.id.toolbar)
            .setBackgroundColour(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.md_light_blue_400
                )
            )
            .setPrimaryText(getString(R.string.target_refresh_title))
            .setFocalColour(ContextCompat.getColor(requireContext(), R.color.colorAccent))
            .setSecondaryText(getString(R.string.target_refresh_content))
            .setPromptStateChangeListener { _, state -> if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_DISMISSING) showLocTarget() }
            .show()
    }

    fun showNoInternet() {
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
                if ((grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    showCity()
                } else {
                    permission!!.permissionDenied()
                }
            }
        }
    }

    private fun showCity() {
        gps = GPSTracker(requireContext())
        if (!gps!!.canGetLocation())
            gps!!.showSettingsAlert()
        else {
            val lat = gps!!.getLatitude()
            val lon = gps!!.getLongitude()
            changeCity(lat, lon)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun renderWeather(jsonObj: Info) {
        try {
            val json0 = jsonObj.day
            val json1 = jsonObj.fort
            tc = json0.main.temp
            prefs.latitude = json1.city.coord.latitude.toFloat()
            prefs.longitude = json1.city.coord.longitude.toFloat()
            if (citys == null) prefs.city = json1.city.name + "," + json0!!.sys.country
            val a = json0!!.main.temp.roundToInt()
            val city = (json1.city.name.uppercase() + ", " + json1.city.country)
            binding.cityField.text = city
            binding.cityField.setOnClickListener { v -> Snack.make(v, city, Snackbar.LENGTH_SHORT) }
            val details: MutableList<WeatherList> = json1.list
            for (i in 0..9) {
                details[i] = json1.list[i]
            }
            val horizontalAdapter = HorizontalAdapter(details)
            horizontalRecyclerView.adapter = horizontalAdapter
            val timeFormat24Hours = prefs.isTimeFormat24Hours
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
            val updatedOn = "Last update: " + df.format(Date(json0.dt * 1000))
            binding.updatedField.text = updatedOn
            val humidity = getString(R.string.humidity_, json0.main.humidity)
            val humidity1 = getString(R.string.humidity, json0.main.humidity)
            binding.humidityView.text = humidity
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
                json0.wind.speed,
                if ((PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(
                        Constants.PREF_TEMPERATURE_UNITS, Constants.METRIC
                    ) == Constants.METRIC)
                ) getString(R.string.mps) else getString(R.string.mph)
            )
            val wind1 = getString(
                R.string.wind_,
                json0.wind.speed,
                if ((PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(
                        Constants.PREF_TEMPERATURE_UNITS, Constants.METRIC
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
            binding.weatherIcon11.text = Utils.setWeatherIcon(requireContext(), json0.weather.get(0).id)
            binding.weatherIcon11.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View) {
                    try {
                        val rs = json0.weather[0].description
                        val strArray = rs.split(" ").toTypedArray()
                        val builder = StringBuilder()
                        for (s: String in strArray) {
                            val cap =
                                s.substring(0, 1).uppercase(Locale.getDefault()) + s.substring(1)
                            builder.append("$cap ")
                        }
                        Snack.make(
                            v, getString(
                                R.string.hey_there_condition,
                                builder.toString().substring(0, builder.length - 1)
                            ), Snackbar.LENGTH_SHORT
                        )
                    } catch (e: Exception) {
                        e("Error", "Main Weather Icon OnClick Details could not be loaded")
                    }
                }
            })
            binding.button1.text = "$a${if (prefs.units == Constants.METRIC) "°C" else "°F"}"
            setDeg(json0.wind.direction)
        } catch (e: Exception) {
            e(
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
                Utils.setWeatherIcon(requireContext(), horizontalList.get(position).weather.get(0).id)
            val date1 = horizontalList[position].dt
            val expiry = Date(date1 * 1000)
            val date = SimpleDateFormat("EE, dd", Locale(Prefs(requireContext()).language)).format(expiry)
            val line2 = horizontalList[position].temp.max.toString() + (if (prefs.units == Constants.METRIC) "°C" else "°F") + "    "
            val line3 = horizontalList[position].temp.min.toString() + (if (prefs.units == Constants.METRIC) "°C" else "°F")
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
                    requireActivity().supportFragmentManager, bottomSheetDialogFragment.tag
                )
            }
            holder.weather_icon.setOnClickListener {
                val bottomSheetDialogFragment = newInstance(horizontalList[position])
                bottomSheetDialogFragment.show(
                    requireActivity().supportFragmentManager, bottomSheetDialogFragment.tag
                )
            }
            holder.weather_icon.setTextColor(
                ContextCompat.getColor(
                    requireContext(), R.color.textColor
                )
            )
            holder.weather_icon.typeface = weatherFont
            holder.details_view.setTextColor(
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
        changeCity(prefs.city)
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