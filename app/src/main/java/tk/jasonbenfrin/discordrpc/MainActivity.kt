package tk.jasonbenfrin.discordrpc

import android.annotation.SuppressLint
import android.app.*
import android.content.*
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
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.datepicker.*
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.*
import com.jakewharton.processphoenix.ProcessPhoenix
import okhttp3.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.lang.IllegalArgumentException
import java.net.URL
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

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
            .addToBackStack(null)
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
            @SuppressLint("SetTextI18n")
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
            val connect = view.findViewById<ExtendedFloatingActionButton>(R.id.connect)
            if(Service.SERVICE_RUNNING) {
                connect?.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_connect_off)
                connect?.text = "Disconnect"
                connect.setOnClickListener { disconnect(it) }
            }
            else connect.setOnClickListener { connectFancy(it) }
        }

        @SuppressLint("SetTextI18n")
        private fun connectFancy(view: View) {
            val connect = view.findViewById<ExtendedFloatingActionButton>(R.id.connect)
            connect.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_connecting)
            connect.text = "Connecting..."
            connect.setOnClickListener { disconnect(it) }
            connect()
        }

        private fun connect() {
            try {
                if (getToken(requireContext()) == null) {
                    Toast.makeText(requireContext(), "Please Login first", Toast.LENGTH_SHORT)
                        .show()
                    return
                }
                val file = File(context?.cacheDir, "payload")
                if (!file.exists()) {
                    Toast.makeText(
                        context,
                        "Oops, something went wrong! Please restart this app.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
            } catch (e:Throwable) {}
            val intent = Intent(context, Service::class.java)
            Thread {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireContext().startForegroundService(intent)
                } else {
                    requireContext().startService(intent)
                }
            }.start()
            requireContext().registerReceiver(receiver, IntentFilter("ServiceToConnectButton"))
            CONNECTED = true
        }

        @SuppressLint("SetTextI18n")
        fun disconnect(view : View) {
            val connect = view.findViewById<ExtendedFloatingActionButton>(R.id.connect)
            connect.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_connect)
            connect.text = "Connect"
            val intent = Intent(context, Service::class.java)
            requireContext().stopService(intent)
            try { requireContext().unregisterReceiver(receiver) } catch (e : IllegalArgumentException) {}
            connect.setOnClickListener { connectFancy(view) }
            CONNECTED = false
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
            view.findViewById<Button>(R.id.viewLog).setOnClickListener { viewLog() }
        }

        private fun github() {
            startActivity(Intent("android.intent.action.VIEW", Uri.parse("https://github.com/JasonBenfrin/Discord-Rich-Presence-Android")))
        }

        private fun viewLog() {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                .addToBackStack(null)
                .replace(R.id.fragmentContainerView, Log())
                .commit()
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
        private lateinit var json : JsonObject
        private lateinit var file : File
        private val timestampJsonObject = JsonObject()

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
            super.onCreateView(inflater, container, savedInstanceState)
            return inflater.inflate(R.layout.rich_presence, container,false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val activity = view.findViewById<SwitchMaterial>(R.id.activity)
            file = File(requireContext().cacheDir, "activity")
            activitySwitch(View.GONE, view)
            view.findViewById<SwitchMaterial>(R.id.showAll).setOnCheckedChangeListener { _, isChecked ->
                if(isChecked) {
                    showAllSwitch(View.VISIBLE, view)
                }else{
                    showAllSwitch(View.GONE, view)
                }
            }
            activity.setOnCheckedChangeListener { _, isChecked ->
                if(isChecked) {
                    ACTIVITY_ENABLED = true
                    if(!file.exists()) {
                        json = JsonObject().apply {
                            addProperty("name", "\u200b")
                            addProperty("type", 0)
                            addProperty("created_at", System.currentTimeMillis())
                        }
                        file.writeText(json.toString())
                    }else json = JsonParser.parseString(file.readText(Charsets.UTF_8)).asJsonObject
                    activitySwitch(View.VISIBLE, view)
                }else{
                    ACTIVITY_ENABLED = false
                    activitySwitch(View.GONE, view)
                }
            }
            val activityURL = view.findViewById<EditText>(R.id.activityURL)
            val emojiId = view.findViewById<EditText>(R.id.activityEmojiId)
            val emojiName = view.findViewById<EditText>(R.id.activityEmojiName)
            val emojiAnimated = view.findViewById<CheckBox>(R.id.activityEmojiAnimated)
            val emojiJsonObject = JsonObject().apply {
                addProperty("name", "question")
            }
            val partyJsonObject = JsonObject()
            val assetsJsonObject = JsonObject()
            val secretsJsonObject = JsonObject()
            var partySizeMin : Int? = null
            var partySizeMax : Int? = null
            var button1Text = ""
            var button2Text = ""
            val activityName = view.findViewById<EditText>(R.id.activityName).apply {
                addTextChangedListener { jsonUpdate("name", it.toString(), "\u200b") }
            }
            val activityDetails = view.findViewById<EditText>(R.id.activityDetails).apply {
                addTextChangedListener { jsonUpdate("details", it.toString(), null) }
            }
            val activityType = view.findViewById<Spinner>(R.id.activityType).apply {
                adapter = ArrayAdapter.createFromResource(requireContext(), R.array.activity_types, android.R.layout.simple_spinner_item).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, int: Int, p3: Long) {
                        when (int) {
                            1 -> {
                                activityURL.visibility = View.VISIBLE
                                emojiId.visibility = View.GONE
                                emojiName.visibility = View.GONE
                                emojiAnimated.visibility = View.GONE
                            }
                            4 -> {
                                activityURL.visibility = View.GONE
                                emojiId.visibility = View.VISIBLE
                                emojiName.visibility = View.VISIBLE
                                emojiAnimated.visibility = View.VISIBLE
                            }
                            else -> {
                                activityURL.visibility = View.GONE
                                emojiId.visibility = View.GONE
                                emojiName.visibility = View.GONE
                                emojiAnimated.visibility = View.GONE
                            }
                        }
                        jsonUpdate("type", int)
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {}
                }
            }
            activityURL.addTextChangedListener { jsonUpdate("url", it.toString(), null) }
            emojiId.addTextChangedListener { jsonUpdate("emoji", emojiJsonObject.apply {
                var string : String? = ""
                string = if (it.toString() == "") null else it.toString()
                addProperty("id", string)
            }) }
            emojiName.addTextChangedListener { jsonUpdate("emoji", emojiJsonObject.apply {
                addProperty("id", it.toString())
            }) }
            emojiAnimated.setOnCheckedChangeListener { _, b ->
                emojiJsonObject.addProperty("animated", b)
            }
            view.findViewById<Button>(R.id.activityTimestampStart).apply {
                setOnClickListener {
                    timePicker(
                        "Timestamp Start",
                        null,
                        0
                    )
                }
            }
            view.findViewById<Button>(R.id.activityTimestampEnd).apply {
                setOnClickListener {
                    timePicker(
                        "Timestamp End",
                        null,
                        1
                    )
                }
            }
            val activityApplicationId = view.findViewById<EditText>(R.id.activityApplicationId).apply {
                addTextChangedListener {
                    jsonUpdate(
                        "application_id",
                        it.toString(),
                        null
                    )
                }
            }
            val activityPartyState = view.findViewById<EditText>(R.id.activityPartyState).apply {
                addTextChangedListener {
                    jsonUpdate(
                        "state",
                        it.toString(),
                        null
                    )
                }
            }
            val activityPartyId = view.findViewById<EditText>(R.id.activityPartyId).apply {
                addTextChangedListener {
                    jsonUpdate(
                        "party",
                        partyJsonObject.apply { addProperty("id", it.toString()) })
                }
            }
            val activityPartySizeMin = view.findViewById<EditText>(R.id.activityPartySizeMin).apply {
               addTextChangedListener {
                   partySizeMin = if (it.toString() == "") null else it.toString().toInt()
                   if (partySizeMin != null && partySizeMax != null) {
                       jsonUpdate("party", partyJsonObject.apply {
                           add("size", JsonArray().apply {
                               add(partySizeMin)
                               add(partySizeMax)
                           })
                       })
                   } else {
                       json.remove("size")
                   }
               }
           }
            val activityPartySizeMax = view.findViewById<EditText>(R.id.activityPartySizeMax).apply {
                addTextChangedListener {
                    partySizeMax = if (it.toString() == "") null else it.toString().toInt()
                    if (partySizeMin != null && partySizeMax != null) {
                        jsonUpdate("party", partyJsonObject.apply {
                            add("size", JsonArray().apply {
                                add(partySizeMin)
                                add(partySizeMax)
                            })
                        })
                    } else {
                        json.remove("size")
                    }
                }
            }
            val activityLargeImage = view.findViewById<EditText>(R.id.activityLargeImage).apply {
                addTextChangedListener {
                    jsonUpdate("assets", assetsJsonObject.apply {
                        addProperty("large_image", if (it.toString() == "") null else urlResolver(it.toString()))
                    })
                }
            }
            val activityLargeText = view.findViewById<EditText>(R.id.activityLargeText).apply {
                addTextChangedListener {
                    jsonUpdate("assets", assetsJsonObject.apply {
                        addProperty("large_text", if (it.toString() == "") null else it.toString())
                    })
                }
            }
            val activitySmallImage = view.findViewById<EditText>(R.id.activitySmallImage).apply {
                addTextChangedListener {
                    jsonUpdate("assets", assetsJsonObject.apply {
                        addProperty("small_image", if (it.toString() == "") null else urlResolver(it.toString()))
                    })
                }
            }
            val activitySmallText = view.findViewById<EditText>(R.id.activitySmallText).apply {
                addTextChangedListener {
                    jsonUpdate("assets", assetsJsonObject.apply {
                        addProperty("small_text", if (it.toString() == "") null else it.toString())
                    })
                }
            }
            val activityInstanced = view.findViewById<CheckBox>(R.id.activityInstanced).apply {
                setOnCheckedChangeListener { _, b ->
                    jsonUpdate("instance", b)
                }
            }
            val activitySecretJoin = view.findViewById<EditText>(R.id.activitySecretJoin).apply {
                addTextChangedListener {
                    jsonUpdate("secrets", secretsJsonObject.apply {
                        addProperty("join", if (it.toString() == "") null else it.toString())
                    })
                }
            }
            val activitySecretSpectate = view.findViewById<EditText>(R.id.activitySecretSpectate).apply {
                addTextChangedListener {
                    jsonUpdate("secrets", secretsJsonObject.apply {
                        addProperty("spectate", if (it.toString() == "") null else it.toString())
                    })
                }
            }
            val activitySecretMatch = view.findViewById<EditText>(R.id.activitySecretMatch).apply {
                addTextChangedListener {
                    jsonUpdate("secrets", secretsJsonObject.apply {
                        addProperty("match", if (it.toString() == "") null else it.toString())
                    })
                }
            }
            view.findViewById<Button>(R.id.activityCreatedAt).apply {
                setOnClickListener {
                    timePicker("Created At", false, 2)
                }
            }
            val activityButton1Label = view.findViewById<EditText>(R.id.activityButton1Label).apply {
                addTextChangedListener {
                    button1Text = it.toString()
                    buttonUpdate(button1Text, button2Text)
                }
            }
            val activityButton2Label = view.findViewById<EditText>(R.id.activityButton2Label).apply {
                addTextChangedListener {
                    button2Text = it.toString()
                    buttonUpdate(button1Text, button2Text)
                }
            }
            if(file.exists()) {
                json = JsonParser.parseString(file.readText(Charsets.UTF_8)).asJsonObject
                if(json.has("name") && !json.get("name").isJsonNull) activityName.setText(json.get("name").asString, TextView.BufferType.EDITABLE)
                if(json.has("type") && !json.get("type").isJsonNull) activityType.setSelection(json.get("type").asInt)
                if(json.has("url") && !json.get("url").isJsonNull) activityURL.setText(json.get("url").asString, TextView.BufferType.EDITABLE)
                if(json.has("application_id") && !json.get("application_id").isJsonNull) activityApplicationId.setText(json.get("application_id").asString, TextView.BufferType.EDITABLE)
                if(json.has("details") && !json.get("details").isJsonNull) activityDetails.setText(json.get("details").asString, TextView.BufferType.EDITABLE)
                if(json.has("state") && !json.get("state").isJsonNull) activityPartyState.setText(json.get("state").asString, TextView.BufferType.EDITABLE)
                if(json.has("emoji") && !json.get("emoji").isJsonNull) {
                    val emoji = json.get("emoji").asJsonObject
                    if(emoji.has("name") && !emoji.get("name").isJsonNull) emojiName.setText(emoji.get("name").asString, TextView.BufferType.EDITABLE)
                    if(emoji.has("id") && !emoji.get("id").isJsonNull) emojiId.setText(emoji.get("id").asString, TextView.BufferType.EDITABLE)
                    if(emoji.has("animated") && !emoji.get("animated").isJsonNull) emojiAnimated.isChecked = emoji.get("animated").asBoolean
                }
                if(json.has("party") && !json.get("party").isJsonNull) {
                    val party = json.get("party").asJsonObject
                    if(party.has("id") && !party.get("id").isJsonNull) activityPartyId.setText(party.get("id").asString, TextView.BufferType.EDITABLE)
                    if(party.has("size") && !party.get("size").isJsonNull) {
                        val partySize = party.get("size").asJsonArray
                        activityPartySizeMin.setText(partySize[0].asString, TextView.BufferType.EDITABLE)
                        activityPartySizeMax.setText(partySize[1].asString, TextView.BufferType.EDITABLE)
                    }
                }
                if(json.has("assets") && !json.get("assets").isJsonNull) {
                    val assets = json.get("assets").asJsonObject
                    if(assets.has("large_image") && !assets.get("large_image").isJsonNull) activityLargeImage.setText(assets.get("large_image").asString, TextView.BufferType.EDITABLE)
                    if(assets.has("large_text") && !assets.get("large_text").isJsonNull) activityLargeText.setText(assets.get("large_text").asString, TextView.BufferType.EDITABLE)
                    if(assets.has("small_image") && !assets.get("small_image").isJsonNull) activitySmallImage.setText(assets.get("small_image").asString, TextView.BufferType.EDITABLE)
                    if(assets.has("small_text") && !assets.get("small_text").isJsonNull) activitySmallText.setText(assets.get("small_text").asString, TextView.BufferType.EDITABLE)
                }
                if(json.has("secrets") && !json.get("secrets").isJsonNull) {
                    val secrets = json.get("secrets").asJsonObject
                    if(secrets.has("join") && !secrets.get("join").isJsonNull) activitySecretJoin.setText(secrets.get("join").asString, TextView.BufferType.EDITABLE)
                    if(secrets.has("spectate") && !secrets.get("spectate").isJsonNull) activitySecretSpectate.setText(secrets.get("spectate").asString, TextView.BufferType.EDITABLE)
                    if(secrets.has("match") && !secrets.get("match").isJsonNull) activitySecretMatch.setText(secrets.get("match").asString, TextView.BufferType.EDITABLE)
                }
                if(json.has("instance") && !json.get("instance").isJsonNull) activityInstanced.isChecked = json.get("instance").asBoolean
                if(json.has("buttons") && !json.get("buttons").isJsonNull) {
                    val buttons = json.get("buttons").asJsonArray
                    if(buttons.size() == 1) {
                        activityButton1Label.setText(buttons[0].asString, TextView.BufferType.EDITABLE)
                    }else{
                        activityButton1Label.setText(buttons[0].asString, TextView.BufferType.EDITABLE)
                        activityButton2Label.setText(buttons[1].asString, TextView.BufferType.EDITABLE)
                    }
                }
            }
            activity.isChecked = ACTIVITY_ENABLED
        }

        private fun urlResolver(url:String) : String {
            val formattedURL : String = url.removePrefix("https://").removePrefix("http://")
            if(url.contains("mp")) return url
            if(url.contains("cdn.discordapp.com")) return "mp:" + formattedURL.removePrefix("cdn.discordapp.com/")
            if(url.contains("media.discordapp.net")) return "mp:" + formattedURL.removePrefix("media.discordapp.net/")
            return "mp:$url"
        }

        private fun buttonUpdate(text1: String, text2: String) {
            jsonUpdate("buttons", JsonArray().apply {
                if (text1 != "") add(text1)
                if (text2 != "") add(text2)
            })
            if(text1 == "" && text2 == "" && json.has("buttons")) jsonRemove("buttons")
        }

        private fun timePicker(property: String, backwards : Boolean?, type: Int) {
            var hours : Int? = null
            var minutes : Int? = null
            var day : Long? = null
            val calendar = Calendar.getInstance()
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(if(DateFormat.is24HourFormat(activity)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .setTitleText("\"$property\" property")
                .setPositiveButtonText("Set")
                .setNegativeButtonText("Reset")
                .build()
            timePicker.show(parentFragmentManager, "TimePicker")
            timePicker.addOnPositiveButtonClickListener {
                hours = timePicker.hour
                minutes = timePicker.minute
                day = MaterialDatePicker.todayInUtcMilliseconds() - TimeZone.getDefault().getOffset(Date().time)
                val constraintsBuilderRange = CalendarConstraints.Builder()
                val listOfValidators = ArrayList<CalendarConstraints.DateValidator>()
                if(backwards == true) listOfValidators.add(DateValidatorPointBackward.now())
                else if(backwards == false) listOfValidators.add(DateValidatorPointForward.now())
                val validators = CompositeDateValidator.allOf(listOfValidators)
                constraintsBuilderRange.setValidator(validators)
                val datePickerBuilder = MaterialDatePicker.Builder.datePicker()
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .setTitleText("\"$property\" property")
                    .setPositiveButtonText("Set")
                    .setNegativeButtonText("Reset")
                if (backwards != null) datePickerBuilder.setCalendarConstraints(constraintsBuilderRange.build())
                val datePicker = datePickerBuilder.build()
                datePicker.show(parentFragmentManager, "DatePicker")
                datePicker.addOnPositiveButtonClickListener {
                    day = it - TimeZone.getDefault().getOffset(Date().time)
                    dateTimePickerJsonResolver(type, calculateMilliSeconds(day, hours, minutes))
                }
                datePicker.addOnNegativeButtonClickListener {
                    dateTimePickerJsonResolver(type, calculateMilliSeconds(day, hours, minutes))
                }
            }
            timePicker.addOnNegativeButtonClickListener {
                dateTimePickerJsonResolver(type, calculateMilliSeconds(day, hours, minutes))
            }
        }

        private fun calculateMilliSeconds(day: Long?, hours: Int?, minutes: Int?): Long? {
            return if(day == null && hours == null && minutes == null) null
            else day!! + (hours!! * 60 + minutes!!) * 60000
        }

        private fun dateTimePickerJsonResolver(type: Int, time: Long?) {
            when (type) {
                0 -> jsonUpdate("timestamps", timestampJsonObject.apply { addProperty("start", time) })
                1 -> jsonUpdate("timestamps", timestampJsonObject.apply { addProperty("end", time) })
                2 -> jsonUpdate("created_at", time ?: System.currentTimeMillis())
            }
        }

        private fun jsonUpdate(property: String, key: String, fallback: String?) {
            if(key != "") json.addProperty(property, key) else json.addProperty(property, fallback)
            file.writeText(json.toString())
        }

        private fun jsonUpdate(property: String, key: Int) {
            json.addProperty(property, key)
            file.writeText(json.toString())
        }

        private fun jsonUpdate(property: String, key: Boolean) {
            json.addProperty(property, key)
            file.writeText(json.toString())
        }

        private fun jsonUpdate(property: String, key: Long?) {
            json.addProperty(property, key)
            file.writeText(json.toString())
        }

        private fun jsonUpdate(property: String, key: JsonElement) {
            json.add(property, key)
            file.writeText(json.toString())
        }

        private fun jsonRemove(property: String) {
            json.remove(property)
            file.writeText(json.toString())
        }

        private fun showAllSwitch(visibility: Int, view: View) {
            view.findViewById<EditText>(R.id.activityApplicationId).visibility = visibility
            view.findViewById<EditText>(R.id.activityPartyId).visibility = visibility
            view.findViewById<EditText>(R.id.activitySecretJoin).visibility = visibility
            view.findViewById<EditText>(R.id.activitySecretSpectate).visibility = visibility
            view.findViewById<EditText>(R.id.activitySecretMatch).visibility = visibility
            view.findViewById<CheckBox>(R.id.activityInstanced).visibility = visibility
            view.findViewById<Button>(R.id.activityCreatedAt).visibility = visibility
        }

        private fun activitySwitch(visibility : Int, view: View) {
            // I have no idea how to make this more efficient, if you do, please create an issue at https://github.com/JasonBenfrin/Discord-Rich-Presence-Android please
            val spinner = view.findViewById<Spinner>(R.id.activityType)
            val url = view.findViewById<EditText>(R.id.activityURL)
            val emojiId = view.findViewById<EditText>(R.id.activityEmojiId)
            val emojiName = view.findViewById<EditText>(R.id.activityEmojiName)
            val emojiAnimated = view.findViewById<CheckBox>(R.id.activityEmojiAnimated)
            view.findViewById<SwitchMaterial>(R.id.showAll).visibility = visibility
            view.findViewById<EditText>(R.id.activityName).visibility = visibility
            view.findViewById<EditText>(R.id.activityDetails).visibility = visibility
            spinner.visibility = visibility
            url.visibility = visibility
            emojiId.visibility = visibility
            emojiName.visibility = visibility
            emojiAnimated.visibility = visibility
            view.findViewById<Button>(R.id.activityTimestampStart).visibility = visibility
            view.findViewById<Button>(R.id.activityTimestampEnd).visibility = visibility
            view.findViewById<EditText>(R.id.activityPartyState).visibility = visibility
            view.findViewById<EditText>(R.id.activityPartySizeMin).visibility = visibility
            view.findViewById<EditText>(R.id.activityPartySizeMax).visibility = visibility
            view.findViewById<EditText>(R.id.activityLargeImage).visibility = visibility
            view.findViewById<EditText>(R.id.activityLargeText).visibility = visibility
            view.findViewById<EditText>(R.id.activitySmallImage).visibility = visibility
            view.findViewById<EditText>(R.id.activitySmallText).visibility = visibility
            view.findViewById<EditText>(R.id.activityButton1Label).visibility = visibility
            view.findViewById<EditText>(R.id.activityButton2Label).visibility = visibility
            if(view.findViewById<SwitchMaterial>(R.id.showAll).isChecked && visibility == View.VISIBLE) showAllSwitch(View.VISIBLE, view) else showAllSwitch(View.GONE, view)
            when (spinner.selectedItemPosition) {
                1 -> {
                    url.visibility = View.VISIBLE
                    emojiId.visibility = View.GONE
                    emojiName.visibility = View.GONE
                    emojiAnimated.visibility = View.GONE
                }
                4 -> {
                    url.visibility = View.GONE
                    emojiId.visibility = View.VISIBLE
                    emojiName.visibility = View.VISIBLE
                    emojiAnimated.visibility = View.VISIBLE
                }
                else -> {
                    url.visibility = View.GONE
                    emojiId.visibility = View.GONE
                    emojiName.visibility = View.GONE
                    emojiAnimated.visibility = View.GONE
                }
            }
        }
    }

    class Load : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
            super.onCreateView(inflater, container, savedInstanceState)
            return inflater.inflate(R.layout.load, container,false)
        }
    }

    class Log : Fragment() {
        private lateinit var textView: TextView
        private var logReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, intent: Intent?) {
                if(textView.text == getString(R.string.no_log_here)) textView.text = ""
                textView.append(intent?.extras?.getString("new"))
            }
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
            super.onCreateView(inflater, container, savedInstanceState)
            return inflater.inflate(R.layout.view_log, container,false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            textView = view.findViewById(R.id.logTextView)
            val file = File(requireContext().filesDir, "WebSocket.log")
            if(file.exists()) { textView.text = file.readText(Charsets.UTF_8) }
            requireContext().registerReceiver(logReceiver, IntentFilter("ServiceLog"))
        }

        override fun onDestroy() {
            super.onDestroy()
            requireContext().unregisterReceiver(logReceiver)
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

        var ACTIVITY_ENABLED = false

        var CONNECTED = false
    }
}