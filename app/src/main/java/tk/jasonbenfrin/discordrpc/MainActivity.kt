package tk.jasonbenfrin.discordrpc

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.navigationrail.NavigationRailView
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.lang.Exception
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private var previousFragment : Int? = null
    private var previousOrientation : Int = Configuration.ORIENTATION_UNDEFINED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_main)

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
    }

    // UI: Set NavigationBars' selected item on orientation and refresh
    @SuppressLint("CutPasteId")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val bottomBar : BottomNavigationView = findViewById(R.id.bottomBar)
        val navRail : NavigationRailView = findViewById(R.id.navRail)
        setContentView(R.layout.activity_main)
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

    private fun itemSelectedListener(id : MenuItem ) : Boolean {
        val fragmentManager = supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
        if( previousFragment != null && previousFragment == id.itemId && previousOrientation == resources.configuration.orientation) return false
        when (id.itemId) {
            R.id.login -> fragmentManager.replace(R.id.fragmentContainerView, LoginFragment(supportFragmentManager, this))
            R.id.set -> fragmentManager.replace(R.id.fragmentContainerView, PresenceFragment())
            R.id.store -> fragmentManager.replace(R.id.fragmentContainerView, Save())
            R.id.about -> fragmentManager.replace(R.id.fragmentContainerView, Info())
            else -> return false
        }
        previousFragment = id.itemId
        fragmentManager.commit()
        return true
    }

    class LoginFragment(private val supportFragmentManager : FragmentManager, private val main: MainActivity) : Fragment() {
        private var token : String? = null
        private lateinit var viewT : View

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
            super.onCreateView(inflater, container, savedInstanceState)
            return inflater.inflate(R.layout.login, container,false)
        }

        @SuppressLint("SetJavaScriptEnabled", "SetTextI18n")
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            viewT = view
            val login = view.findViewById<Button>(R.id.button2)
            val logout = view.findViewById<Button>(R.id.logout)
            val textView = view.findViewById<TextView>(R.id.textView4)
            val image = view.findViewById<ImageView>(R.id.imageView6)
            login.setOnClickListener { login() }
            logout.setOnClickListener { logout() }
            token = context?.let { MainActivity().getToken(it) }
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
                        val listener = context?.let { TestDiscordGatewayWebSocket(this, it, main) }
                        val client = OkHttpClient()
                        client.newWebSocket(request, listener)
                        client.dispatcher().executorService().shutdown()
                    }.start()
                }
            }
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
                    val restart = Intent(activity, MainActivity::class.java)
                    restart.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(restart)
                }
                else Toast.makeText(context, "Files Partially deleted!\nPlease clear cache and data in settings!", Toast.LENGTH_LONG).show()
            }catch (_ : Throwable) {
                Toast.makeText(context, "Failed to Logout!\nPlease clear cache and data in settings", Toast.LENGTH_LONG).show()
            }
        }

        private fun login () {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                .replace(R.id.fragmentContainerView, LoginWebFragment(supportFragmentManager, main))
                .commit()
        }

        @SuppressLint("SetTextI18n")
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
                val bufferedReader = BufferedReader(FileReader(file))
                var str = ""
                while (true) {
                    val bufferedLine = bufferedReader.readLine() ?: break
                    str += bufferedLine
                }
                try {
                    val input = URL(str.split("#")[2]+"?size=512").openStream()
                    image = BitmapFactory.decodeStream(input)
                    handler.post {
                        imageView.setImageBitmap(image)
                    }
                }catch (_:Exception) {imageView.visibility = View.GONE}
            }
        }

        @SuppressLint("SetTextI18n")
        fun updateUser(context: Context, file: File, textView : TextView, imageView: ImageView) {
            var string = ""
            val bufferedReader = BufferedReader(FileReader(file))
            while (true) {
                val bufferLine = bufferedReader.readLine() ?: break
                string += bufferLine
            }
            if (string != "") {
                val user = string.split("#")
                userAvatar(context, imageView)
                textView.text = "Logged in as:\n ${user[0]}#${user[1]}"
            }
        }

        @SuppressLint("SetTextI18n")
        fun updateFailure() {
            viewT.findViewById<TextView>(R.id.textView4)?.text = "Failed to test the token. Please Logout and Login again"
        }
    }

    class PresenceFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
            super.onCreateView(inflater, container, savedInstanceState)
            return inflater.inflate(R.layout.presence, container,false)
        }
    }

    class Save : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
            super.onCreateView(inflater, container, savedInstanceState)
            return inflater.inflate(R.layout.save, container,false)
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

    class LoginWebFragment(private val supportFragmentManager: FragmentManager, private val main : MainActivity) : Fragment() {
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
                    if(url != null && url.endsWith("/app")) {
                        back()
                        return false
                    }
                    return false
                }
            }
            webView.loadUrl("https://discord.com/login")
        }

        private fun back() {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainerView, LoginFragment(supportFragmentManager, main))
                .commit()
        }
    }

    private class TestDiscordGatewayWebSocket(private val fragment : LoginFragment, private val context : Context, private val main : MainActivity) : WebSocketListener() {
        private var seq : Int? = null
        private lateinit var webSocket : WebSocket

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            this.webSocket = webSocket
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            val json = JsonParser.parseString(text).asJsonObject
            when(json.get("op").asInt) {
                10 -> {
                    main.identify(webSocket, context)
                }
                0 -> {
                    seq = json.get("s").asInt
                    if(json.get("t").asString == "READY") {
                        val d = json.get("d").asJsonObject
                        val user = d.get("user").asJsonObject
                        var avatar : String = ""
                        if(user.get("avatar").isJsonNull) {
                            avatar = "https://cdn.discordapp.com/embed/avatars/${user.get("discriminator").asInt%5}.png"
                        }else{
                            avatar = "https://cdn.discordapp.com/avatars/${user.get("id").asString}/${user.get("avatar").asString}.png"
                        }
                        fragment.updateView(avatar, user.get("username").asString, user.get("discriminator").asString)
                        val file = File(context.filesDir, "user")
                        fragment.updateUser(context, file, main.findViewById(R.id.textView4), main.findViewById(R.id.imageView6))
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

    private fun identify(webSocket: WebSocket, context: Context) {
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

    fun hearbeatSend( webSocket: WebSocket, seq: Int? ) {
        val json = JsonObject()
        json.addProperty("op","1")
        json.addProperty("d", seq)
        webSocket.send(json.toString())
    }

    private fun getToken(context : Context) : String? {
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

    // UI: TabLayout and ViewPager2 Synchronise

}