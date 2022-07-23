package tk.jasonbenfrin.discordrpc

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.FragmentManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*

class MainActivity : AppCompatActivity() {
    private var previousFragment : Int? = null
    private var previousOrientation : Int = Configuration.ORIENTATION_UNDEFINED

    enum class THEME(val theme : Int, val drawable : Int) {
        DARK(AppCompatDelegate.MODE_NIGHT_YES, R.drawable.ic_theme_dark),
        LIGHT(AppCompatDelegate.MODE_NIGHT_NO, R.drawable.ic_theme_light),
        SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.drawable.ic_theme_system)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_main)

        val topAppBar = findViewById<MaterialToolbar>(R.id.materialToolbar)
        val text = topAppBar.getChildAt(0) as TextView
        text.typeface = Typeface.createFromAsset(assets, "fonts/GintoNord-Medium.ttf")
        text.textSize = 25f
        val themeButton = topAppBar.menu[0]
        themeButton.setOnMenuItemClickListener { menuItem -> themeChangeListener(menuItem) }
        val file = File(applicationContext.filesDir, "theme")
        if(file.exists()) {
            val theme = file.readText(Charsets.UTF_8).toInt()
            AppCompatDelegate.setDefaultNightMode(theme)
            when(theme) {
                THEME.DARK.theme -> themeButton.icon = ContextCompat.getDrawable(baseContext, THEME.DARK.drawable)
                THEME.LIGHT.theme -> themeButton.icon = ContextCompat.getDrawable(baseContext, THEME.LIGHT.drawable)
                THEME.SYSTEM.theme -> themeButton.icon = ContextCompat.getDrawable(baseContext, THEME.SYSTEM.drawable)
            }
        }

        previousOrientation = resources.configuration.orientation
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragmentContainerView, PresenceFragment())
                .commit()
        }

        val bottomBar : BottomNavigationView = findViewById(R.id.bottomBar)
        bottomBar.selectedItemId = R.id.set
        bottomBar.setOnItemSelectedListener { item -> itemSelectedListener(item) }
        findViewById<NavigationRailView>(R.id.navRail).setOnItemSelectedListener{ item -> itemSelectedListener(item) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(getString(R.string.notification_service_id), getString(R.string.notification_service_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = getString(R.string.notification_service_description)
            }
            val channel2 = NotificationChannel(getString(R.string.notification_error_id), getString(R.string.notification_error_name), NotificationManager.IMPORTANCE_HIGH).apply {
                description = getString(R.string.notification_error_description)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(channel2)
        }
    }

    @SuppressLint("CutPasteId")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val bottomBar : BottomNavigationView = findViewById(R.id.bottomBar)
        val navRail : NavigationRailView = findViewById(R.id.navRail)
        setContentView(R.layout.activity_main)
        findViewById<NavigationRailView>(R.id.navRail).setOnItemSelectedListener{ item -> itemSelectedListener(item) }
        val topAppBar = findViewById<MaterialToolbar>(R.id.materialToolbar)
        val text = topAppBar.getChildAt(0) as TextView
        text.typeface = Typeface.createFromAsset(assets, "fonts/GintoNord-Medium.ttf")
        text.textSize = 25f
        val themeButton = topAppBar.menu[0]
        val file = File(applicationContext.filesDir, "theme")
        if(file.exists()) {
            val theme = file.readText(Charsets.UTF_8).toInt()
            AppCompatDelegate.setDefaultNightMode(theme)
            when(theme) {
                THEME.DARK.theme -> themeButton.icon = ContextCompat.getDrawable(baseContext, THEME.DARK.drawable)
                THEME.LIGHT.theme -> themeButton.icon = ContextCompat.getDrawable(baseContext, THEME.LIGHT.drawable)
                THEME.SYSTEM.theme -> themeButton.icon = ContextCompat.getDrawable(baseContext, THEME.SYSTEM.drawable)
            }
        }
        themeButton.setOnMenuItemClickListener { menuItem -> themeChangeListener(menuItem) }
        val newBottomBar : BottomNavigationView = findViewById(R.id.bottomBar)
        val newNavRail : NavigationRailView = findViewById(R.id.navRail)
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                newNavRail.setOnItemSelectedListener{ item -> itemSelectedListener(item) }
                newNavRail.selectedItemId = bottomBar.selectedItemId
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                newBottomBar.setOnItemSelectedListener{ item -> itemSelectedListener(item) }
                newBottomBar.selectedItemId = navRail.selectedItemId
            }
            Configuration.ORIENTATION_SQUARE -> {
                newBottomBar.visibility = View.VISIBLE
                newNavRail.visibility = View.INVISIBLE
                newBottomBar.setOnItemSelectedListener{ item -> itemSelectedListener(item) }
                newBottomBar.selectedItemId = bottomBar.selectedItemId
            }
            Configuration.ORIENTATION_UNDEFINED -> {
                newBottomBar.visibility = View.VISIBLE
                newNavRail.visibility = View.INVISIBLE
                newBottomBar.setOnItemSelectedListener{ item -> itemSelectedListener(item) }
                newBottomBar.selectedItemId = bottomBar.selectedItemId
            }
        }
        previousOrientation = newConfig.orientation
    }

    private fun themeChangeListener( menuItem : MenuItem ) : Boolean {
        if(menuItem.itemId != R.id.more) return false
        val file = File(applicationContext.filesDir, "theme")
        return if(!file.exists()) {
            file.writeText(THEME.DARK.theme.toString())
            AppCompatDelegate.setDefaultNightMode(THEME.DARK.theme)
            menuItem.icon = ContextCompat.getDrawable(applicationContext, THEME.DARK.drawable)
            true
        } else {
            when(file.readText(Charsets.UTF_8).toInt()) {
                THEME.DARK.theme -> {
                    file.writeText(THEME.LIGHT.theme.toString())
                    AppCompatDelegate.setDefaultNightMode(THEME.LIGHT.theme)
                    menuItem.icon = ContextCompat.getDrawable(applicationContext, THEME.LIGHT.drawable)
                }
                THEME.LIGHT.theme -> {
                    file.writeText(THEME.SYSTEM.theme.toString())
                    AppCompatDelegate.setDefaultNightMode(THEME.SYSTEM.theme)
                    menuItem.icon = ContextCompat.getDrawable(applicationContext, THEME.SYSTEM.drawable)
                }
                THEME.SYSTEM.theme -> {
                    file.writeText(THEME.DARK.theme.toString())
                    AppCompatDelegate.setDefaultNightMode(THEME.DARK.theme)
                    menuItem.icon = ContextCompat.getDrawable(applicationContext, THEME.DARK.drawable)
                }
            }
            true
        }
    }

    private fun itemSelectedListener( id : MenuItem ) : Boolean {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
        if( previousFragment != null && previousFragment == id.itemId && previousOrientation == resources.configuration.orientation) return false
        when (id.itemId) {
            R.id.login -> fragmentTransaction.replace(R.id.fragmentContainerView, LoginFragment())
            R.id.set -> fragmentTransaction.replace(R.id.fragmentContainerView, PresenceFragment())
            R.id.about -> fragmentTransaction.replace(R.id.fragmentContainerView, Info())
            else -> return false
        }
        previousFragment = id.itemId
        fragmentTransaction.commit()
        return true
    }

    class TestDiscordGatewayWebSocket(private val fragment : LoginFragment, private val context : Context, private val view : View) : WebSocketListener() {
        private var seq : Int? = null

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            LoginFragment.webSocket = webSocket
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            val json = JsonParser.parseString(text).asJsonObject
            when(json.get("op").asInt) {
                10 -> {
                    identify(webSocket, context)
                }
                0 -> {
                    seq = json.get("s").asInt
                    if(json.get("t").asString == "READY") {
                        val d = json.get("d").asJsonObject
                        val user = d.get("user").asJsonObject
                        val avatar: String = if(user.get("avatar").isJsonNull) {
                            "https://cdn.discordapp.com/embed/avatars/${user.get("discriminator").asInt%5}.png"
                        }else{
                            "https://cdn.discordapp.com/avatars/${user.get("id").asString}/${user.get("avatar").asString}.png"
                        }
                        fragment.updateView(avatar, user.get("username").asString, user.get("discriminator").asString)
                        val file = File(context.filesDir, "user")
                        fragment.updateUser(context, file, view.findViewById(R.id.textView4), view.findViewById(R.id.imageView6))
                        webSocket.close(1000, "Job done")
                    }
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            fragment.updateFailure()
        }
    }

    companion object {
        fun identify(webSocket: WebSocket, context: Context) {
            val properties = JsonObject()
            properties.addProperty("os","linux")
            properties.addProperty("browser","unknown")
            properties.addProperty("device","unknown")
            val d = JsonObject()
            d.addProperty("token", getToken(context))
            d.addProperty("intents", 0)
            d.add("properties", properties)
            val payload = JsonObject()
            payload.addProperty("op",2)
            payload.add("d", d)
            webSocket.send(payload.toString())
        }

        fun getToken(context : Context) : String? {
            try {
                val listFiles : Array<File> = File( context.filesDir.parentFile, "app_webview/Default/Local Storage/leveldb" ).listFiles { _, str -> str.endsWith(".log") } as Array<File>
                if (listFiles.isEmpty()) return null
                var bufferLine : String
                val bufferedReader = BufferedReader(FileReader(listFiles[0]))
                do {
                    bufferLine = bufferedReader.readLine()
                } while (!bufferLine.contains("token") && bufferLine != null)
                val sub1 = bufferLine.substring(bufferLine.indexOf("token") + 5)
                val sub2 = sub1.substring(sub1.indexOf("\"")+1)
                return sub2.substring(0, sub2.indexOf("\""))
            }catch (_ : Throwable) {
                return null
            }
        }

        fun heartbeatSend( webSocket: WebSocket, seq: Int? ) {
            val json = JsonObject()
            json.addProperty("op",1)
            json.addProperty("d", seq)
            webSocket.send(json.toString())
        }

        fun statusSimilar(view: View, context: Context, activity: Activity, parentFragmentManager : FragmentManager) {
            val since : Button = view.findViewById(R.id.activityCreatedAt)
            val afk : CheckBox = view.findViewById(R.id.checkBox)
            val spinner : Spinner = view.findViewById(R.id.spinner3)
            val showAll : SwitchMaterial = view.findViewById(R.id.switch1)
            val adapter = ArrayAdapter.createFromResource(context, R.array.status_types, android.R.layout.simple_spinner_item)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
            since.visibility = View.GONE
            afk.visibility = View.GONE
            var json = JsonObject()
            json.addProperty("since", System.currentTimeMillis())
            json.add("activities", JsonArray())
            json.addProperty("status", "online")
            json.addProperty("afk", false)
            val file = File(context.cacheDir, "payload")
            file.writeText(json.toString())
            showAll.setOnCheckedChangeListener { _, isChecked ->
                if(isChecked) {
                    since.visibility = View.VISIBLE
                    afk.visibility = View.VISIBLE
                }else{
                    since.visibility = View.GONE
                    afk.visibility = View.GONE
                }
            }
            since.setOnClickListener {
                val calendar = Calendar.getInstance()
                if (Status.timeShowing) return@setOnClickListener
                val timePicker = MaterialTimePicker.Builder()
                    .setTimeFormat(if(DateFormat.is24HourFormat(activity)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
                    .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                    .setMinute(calendar.get(Calendar.MINUTE))
                    .setTitleText("\"Since\" property")
                    .setPositiveButtonText("Set")
                    .setNegativeButtonText("Reset")
                    .build()
                timePicker.show(parentFragmentManager, "TimePicker")
                timePicker.addOnPositiveButtonClickListener {
                    Status.hours = timePicker.hour
                    Status.minutes = timePicker.minute
                    Status.day = MaterialDatePicker.todayInUtcMilliseconds() - TimeZone.getDefault().getOffset(Date().time)
                    json = setTime(json, context)
                    val constraintsBuilderRange = CalendarConstraints.Builder()
                    val listOfValidators = ArrayList<CalendarConstraints.DateValidator>()
                    listOfValidators.add(DateValidatorPointBackward.now())
                    val validators = CompositeDateValidator.allOf(listOfValidators)
                    constraintsBuilderRange.setValidator(validators)
                    val datePicker = MaterialDatePicker.Builder.datePicker()
                        .setCalendarConstraints(constraintsBuilderRange.build())
                        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                        .setTitleText("\"Since\" property")
                        .setPositiveButtonText("Set")
                        .setNegativeButtonText("Reset")
                        .build()
                    datePicker.show(parentFragmentManager, "DatePicker")
                    datePicker.addOnPositiveButtonClickListener {
                        Status.day = it - TimeZone.getDefault().getOffset(Date().time)
                        json = setTime(json, context)
                    }
                    datePicker.addOnNegativeButtonClickListener { json = resetTime(json, context) }
                    datePicker.addOnCancelListener {
                        Status.day = MaterialDatePicker.todayInUtcMilliseconds() - TimeZone.getDefault().getOffset(Date().time)
                        json = setTime(json, context)
                    }
                }
                timePicker.addOnNegativeButtonClickListener { json = resetTime(json, context) }
            }
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, int : Int, p3: Long) {
                    when(int) {
                        0 -> json.addProperty("status","online")
                        1 -> json.addProperty("status","idle")
                        2 -> json.addProperty("status","dnd")
                        3 -> json.addProperty("status","invisible")
                        4 -> json.addProperty("status","offline")
                    }
                    file.writeText(json.toString())
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
            afk.setOnCheckedChangeListener { _, bool ->
                if(bool) json.addProperty("afk", true)
                else json.addProperty("afk", false)
                file.writeText(json.toString())
            }
        }

        private fun resetTime(json : JsonObject, context: Context) : JsonObject {
            json.addProperty("since",System.currentTimeMillis())
            File(context.cacheDir, "payload").writeText(json.toString())
            return json
        }

        private fun setTime(json : JsonObject, context: Context) : JsonObject {
            val final = Status.day + ( Status.hours * 60 + Status.minutes) * 60000
            json.addProperty("since", final)
            File(context.cacheDir, "payload").writeText(json.toString())
            return json
        }

        //For API 17, just use View.generateViewId()
        fun generateViewId() : Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                View.generateViewId()
            } else {
                resourceID += 1
                resourceID
            }
        }

        var ACTIVITY_ENABLED = false

        var CONNECTED = false

        private var resourceID = 0
    }
}