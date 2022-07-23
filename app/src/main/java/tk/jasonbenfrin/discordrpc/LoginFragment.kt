package tk.jasonbenfrin.discordrpc

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.jakewharton.processphoenix.ProcessPhoenix
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import java.io.File
import java.net.URL
import java.util.concurrent.Executors

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
        token = context?.let { MainActivity.getToken(it) }
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
                        Request.Builder().url("wss://gateway.discord.gg/?v=10&encoding=json").build()
                    val listener = context?.let {
                        MainActivity.TestDiscordGatewayWebSocket(
                            this,
                            it,
                            view
                        )
                    }
                    val client = OkHttpClient()
                    if (listener != null) {
                        client.newWebSocket(request, listener)
                    }
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

    @SuppressLint("SetTextI18n")
    private fun login () {
        val token = MainActivity.getToken(requireContext())
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
                        Request.Builder().url("wss://gateway.discord.gg/?v=10&encoding=json").build()
                    val listener = context?.let {
                        MainActivity.TestDiscordGatewayWebSocket(
                            this,
                            it,
                            viewT
                        )
                    }
                    val client = OkHttpClient()
                    if (listener != null) {
                        client.newWebSocket(request, listener)
                    }
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

    @SuppressLint("SetTextI18n")
    fun updateUser(context: Context, file: File, textView : TextView, imageView: ImageView) {
        val string = file.readText(Charsets.UTF_8)
        if (string != "") {
            val user = string.split("#")
            userAvatar(context, imageView)
            textView.text = "Logged in as:\n ${user[0]}#${user[1]}"
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateFailure() {
        requireActivity().runOnUiThread{
            viewT.findViewById<TextView>(R.id.textView4)?.text = "Failed to test the token. Please Logout and Login again"
        }
    }
}