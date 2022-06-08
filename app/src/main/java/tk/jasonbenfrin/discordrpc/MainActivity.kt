package tk.jasonbenfrin.discordrpc

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.jakewharton.processphoenix.ProcessPhoenix
import okhttp3.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.URL
import java.util.*
import java.util.concurrent.Executors
import kotlin.system.exitProcess

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
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
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

    @SuppressLint("SetTextI18n")
    class LoginFragment : Fragment() {
        private var token : String? = null
        private lateinit var viewT : View

        companion object {
            var webSocket : WebSocket? = null
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
            super.onCreateView(inflater, container, savedInstanceState)
            return inflater.inflate(R.layout.login, container,false)
        }

        @SuppressLint("SetJavaScriptEnabled")
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            viewT = view
            val login = view.findViewById<Button>(R.id.button2)
            val logout = view.findViewById<Button>(R.id.logout)
            val textView = view.findViewById<TextView>(R.id.textView4)
            val image = view.findViewById<ImageView>(R.id.imageView6)
            login.setOnClickListener { login() }
            logout.setOnClickListener { logout() }
            token = context?.let { getToken(it) }
            if (token == null) {
                image.visibility = View.GONE
                textView.text = "You are not logged in, please log in first"
                logout.visibility = View.GONE
            }else{
                login.visibility = View.GONE
                val file = File( context?.filesDir, "user")
                if (file.exists()) {
                    context?.let { updateUser(it, file, textView, image) }
                }else{
                    textView.text = "Testing the extracted token, please wait..."
                    Thread {
                        val request =
                            Request.Builder().url("wss://gateway.discord.gg/?v=9&encoding=json").build()
                        val listener = context?.let { TestDiscordGatewayWebSocket(this, it, view) }
                        val client = OkHttpClient()
                        client.newWebSocket(request, listener)
                        client.dispatcher().executorService().shutdown()
                    }.start()
                }
            }
        }

        override fun onDestroyView() {
            super.onDestroyView()
            if(webSocket != null) webSocket?.close(1000, "Closed by User")
        }

        private fun logout () {
            var successful: Boolean
            try {
                val dir = File( context?.filesDir?.parentFile, "app_webview" )
                val dir2 = File( context?.filesDir?.parentFile, "cache" )
                val dir3 = File( context?.filesDir?.parentFile, "shared_prefs")
                val dir4 = File( context?.filesDir?.parentFile, "app_textures")
                val file6 = File( context?.filesDir, "user" )
                successful = dir.deleteRecursively()
                if(file6.exists() && !file6.delete()) successful = false
                if(!dir2.deleteRecursively()) successful = false
                if(!dir3.deleteRecursively()) successful = false
                if(!dir4.deleteRecursively()) successful = false
                if(successful) {
                    Toast.makeText(context, "Tokens and cache successfully deleted!", Toast.LENGTH_SHORT).show()
                    ProcessPhoenix.triggerRebirth(requireContext().applicationContext)
                }
                else Toast.makeText(context, "Files Partially deleted!\nPlease clear cache and data in settings!", Toast.LENGTH_LONG).show()
            }catch (_ : Throwable) {
                Toast.makeText(context, "Failed to Logout!\nPlease clear cache and data in settings", Toast.LENGTH_LONG).show()
            }
        }

        private fun login () {
            val token = getToken(requireContext())
            if (token != null) {
                val login = viewT.findViewById<Button>(R.id.button2)
                val textView = viewT.findViewById<TextView>(R.id.textView4)
                val image = viewT.findViewById<ImageView>(R.id.imageView6)
                login.visibility = View.GONE
                val file = File( context?.filesDir, "user")
                if (file.exists()) {
                    context?.let { updateUser(it, file, textView, image) }
                }else{
                    textView.text = "Testing the extracted token, please wait..."
                    Thread {
                        val request =
                            Request.Builder().url("wss://gateway.discord.gg/?v=9&encoding=json").build()
                        val listener = context?.let { TestDiscordGatewayWebSocket(this, it, viewT) }
                        val client = OkHttpClient()
                        client.newWebSocket(request, listener)
                        client.dispatcher().executorService().shutdown()
                    }.start()
                }
            }
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                .replace(R.id.fragmentContainerView, LoginWebFragment())
                .commit()
        }

        fun updateView(url : String, username : String, discriminator : String) {
            val file = File(context?.filesDir, "user")
            file.writeText("$username#$discriminator#$url")
            context?.let { userAvatar(it, viewT.findViewById(R.id.imageView6)) }
        }

        private fun userAvatar(context: Context, imageView: ImageView) {
            val executor = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())
            var image: Bitmap?
            executor.execute{
                val file = File(context.filesDir, "user")
                val str = file.readText(Charsets.UTF_8)
                try {
                    val input = URL(str.split("#")[2]+"?size=512").openStream()
                    image = BitmapFactory.decodeStream(input)
                    handler.post {
                        imageView.setImageBitmap(image)
                    }
                }catch (_:Exception) {imageView.visibility = View.GONE}
            }
        }

        fun updateUser(context: Context, file: File, textView : TextView, imageView: ImageView) {
            val string = file.readText(Charsets.UTF_8)
            if (string != "") {
                val user = string.split("#")
                userAvatar(context, imageView)
                textView.text = "Logged in as:\n ${user[0]}#${user[1]}"
            }
        }

        fun updateFailure() {
            viewT.findViewById<TextView>(R.id.textView4)?.text = "Failed to test the token. Please Logout and Login again"
        }
    }

    class PresenceFragment : Fragment() {
        private var receiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                val connect = view?.findViewById<ExtendedFloatingActionButton>(R.id.connect)
                connect?.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_connect_off)
                connect?.text = "Disconnect"
            }
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
            super.onCreateView(inflater, container, savedInstanceState)
            return inflater.inflate(R.layout.presence, container,false)
        }

        @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            // TODO: Check for currently running services and change button listeners accordingly
            super.onViewCreated(view, savedInstanceState)
            val viewPager2 = view.findViewById<ViewPager2>(R.id.viewpager)
            viewPager2.adapter = ViewPagerAdapter()
            val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
            TabLayoutMediator(tabLayout, viewPager2) { tab , position ->
                when (position) {
                    0 -> tab.text = resources.getText(R.string.status)
                    1 -> tab.text = resources.getText(R.string.rich_presence)
                    2 -> tab.text = resources.getText(R.string.load)
                }
            }.attach()
            view.findViewById<ExtendedFloatingActionButton>(R.id.connect).setOnClickListener { connect(it) }
        }

        @SuppressLint("SetTextI18n")
        fun connect(view : View) {
            if(getToken(requireContext()) == null) {
                Toast.makeText(requireContext(), "Please Login first", Toast.LENGTH_SHORT).show()
                return
            }
            val connect = view.findViewById<ExtendedFloatingActionButton>(R.id.connect)
            connect.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_connecting)
            connect.text = "Connecting..."
            val file = File(context?.cacheDir, "payload")
            if(!file.exists()) {
                Toast.makeText(context, "Oops, something went wrong! Please restart this app.", Toast.LENGTH_SHORT).show()
                return
            }
            val intent = Intent(context, Service::class.java)
            Thread {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireContext().startForegroundService(intent)
                } else {
                    requireContext().startService(intent)
                }
            }.start()
            requireContext().registerReceiver(receiver, IntentFilter("ServiceToConnectButton"))
            connect.setOnClickListener { disconnect(it) }
            Log.d("Payload", file.readText(Charsets.UTF_8))
        }

        @SuppressLint("SetTextI18n")
        fun disconnect(view : View) {
            val connect = view.findViewById<ExtendedFloatingActionButton>(R.id.connect)
            connect.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_connect)
            connect.text = "Connect"
            val intent = Intent(context, Service::class.java)
            requireContext().stopService(intent)
            requireContext().unregisterReceiver(receiver)
            connect.setOnClickListener { connect(view) }
        }

        private inner class ViewPagerAdapter : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3

            override fun createFragment(position: Int): Fragment {
                return when(position) {
                    0 -> Status()
                    1 -> RichPresence()
                    else -> Load()
                }
            }
        }
    }

    class Info : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
            super.onCreateView(inflater, container, savedInstanceState)
            return inflater.inflate(R.layout.info, container,false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val githubButton : ImageButton = view.findViewById(R.id.github)
            githubButton.setOnClickListener{ github() }
        }

        private fun github() {
            startActivity(Intent("android.intent.action.VIEW", Uri.parse("https://github.com/JasonBenfrin/Discord-Rich-Presence-Android")))
        }
    }

    class LoginWebFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
            super.onCreateView(inflater, container, savedInstanceState)
            return inflater.inflate(R.layout.login_webview, container,false)
        }

        @SuppressLint("SetJavaScriptEnabled")
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            view.findViewById<ExtendedFloatingActionButton>(R.id.back).setOnClickListener { back() }
            val webView = view.findViewById<WebView>(R.id.webView)
            webView.settings.javaScriptEnabled = true
            webView.settings.setAppCacheEnabled(true)
            webView.settings.databaseEnabled = true
            webView.settings.domStorageEnabled = true
            webView.webViewClient = object : WebViewClient(){
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    webView.stopLoading()
                    if(url != null && url.endsWith("/app")) back()
                    return false
                }
            }
            webView.loadUrl("https://discord.com/login")
        }

        private fun back() {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainerView, LoginFragment())
                .commit()
        }
    }

    class Status : Fragment() {
        companion object{
            var hours: Int = 0
            var minutes: Int = 0
            var day: Long = 0
            var timeShowing = false
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
            super.onCreateView(inflater, container, savedInstanceState)
            return inflater.inflate(R.layout.status, container,false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            statusSimilar(view, requireContext(), requireActivity(), parentFragmentManager)
        }
    }

    class RichPresence : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
            super.onCreateView(inflater, container, savedInstanceState)
            return inflater.inflate(R.layout.rich_presence, container,false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            statusSimilar(view, requireContext(), requireActivity(), parentFragmentManager)
        }
    }

    class Load : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
            super.onCreateView(inflater, container, savedInstanceState)
            return inflater.inflate(R.layout.load, container,false)
        }
    }

    private class TestDiscordGatewayWebSocket(private val fragment : LoginFragment, private val context : Context, private val view : View) : WebSocketListener() {
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
            properties.addProperty("\$os","linux")
            properties.addProperty("\$browser","unknown")
            properties.addProperty("\$device","unknown")
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

        private fun statusSimilar(view: View, context: Context, activity: Activity, parentFragmentManager : FragmentManager) {
            val since : Button = view.findViewById(R.id.since)
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
    }
}