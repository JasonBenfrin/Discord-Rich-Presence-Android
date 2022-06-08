package tk.jasonbenfrin.discordrpc

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.*
import java.io.File

class Service : Service() {
    private var heartbeat : Int = 0
    private var sequence : Int? = null
    private var sessionId : String = ""
    private lateinit var webSocket: WebSocket
    private lateinit var heartbeatThread : Thread
    private lateinit var client :OkHttpClient

    override fun onCreate() {
        super.onCreate()
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(this, getString(R.string.notification_service_id))
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Discord Presence")
            .setContentText("Running in the background")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        startForeground(1, builder.build())
        client = OkHttpClient()
        client.newWebSocket(
            Request.Builder().url("wss://gateway.discord.gg/?v=9&encoding=json").build(),
            DiscordWebSocketListener()
        )
        client.dispatcher().executorService().shutdown()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
        // TODO: May not need this
    }

    override fun onDestroy() {
        super.onDestroy()
        if(this::webSocket.isInitialized) webSocket.close(1000, "Closed by user")
    }

    override fun onBind(p0: Intent?): IBinder? = null

    inner class DiscordWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            this@Service.webSocket = webSocket
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            val json = JsonParser.parseString(text).asJsonObject
            when (json.get("op").asInt) {
                0 -> {
                    if(json.has("s")) sequence = json.get("s").asInt
                    if (json.get("t").asString != "READY") return
                    sessionId = json.get("d").asJsonObject.get("session_id").asString
                    val file = File(baseContext.cacheDir, "payload")
                    if(!file.exists()) {}// TODO: Broadcast Error here
                    val d = JsonParser.parseString(file.readText(Charsets.UTF_8)).asJsonObject
                    val payload = JsonObject()
                    payload.addProperty("op", 3)
                    payload.add("d", d)
                    webSocket.send(payload.toString())
                    sendBroadcast(Intent("ServiceToConnectButton"))
                }
                1 -> {
                    heartbeatThread.interrupt()
                    MainActivity.heartbeatSend(webSocket, sequence)
                    heartbeatThread = Thread(HeartbeatRunnable())
                    heartbeatThread.start()
                }
                7 -> {
                    webSocket.close(1012, "Requested to Restart by the server")
                    client.newWebSocket(
                        Request.Builder().url("wss://gateway.discord.gg/?v=9&encoding=json").build(),
                        DiscordWebSocketListener()
                    )
                    client.dispatcher().executorService().shutdown()
                }
                9 -> {
                    Thread.sleep(5000)
                    MainActivity.identify(webSocket, baseContext)
                }
                10 -> {
                    heartbeat = json.get("d").asJsonObject.get("heartbeat_interval").asInt
                    heartbeatThread = Thread(HeartbeatRunnable())
                    heartbeatThread.start()
                    MainActivity.identify(webSocket, baseContext)
                }
                11 -> {
                    heartbeatThread = Thread(HeartbeatRunnable())
                    heartbeatThread.start()
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            t.message?.let { Log.d("WebSocket", "onFailure() $it") }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosing(webSocket, code, reason)
            Log.d("WebSocket", "onClosing() $code $reason")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            Log.d("WebSocket", "onClosed() $code $reason")
            if(code >= 4000) return
            reconnect()
        }
    }

    fun reconnect() {
        client.newWebSocket(
            Request.Builder().url("wss://gateway.discord.gg/?v=9&encoding=json").build(),
            DiscordWebSocketListener()
        )
        client.dispatcher().executorService().shutdown()
        val d = JsonObject()
        d.addProperty("token", MainActivity.getToken(baseContext))
        d.addProperty("session_id", sessionId)
        d.addProperty("seq", sequence)
        val json = JsonObject()
        json.addProperty("op", 6)
        json.add("d", d)
        webSocket.send(json.toString())
    }

    inner class HeartbeatRunnable : Runnable {
        override fun run() {
            Thread.sleep(heartbeat.toLong())
            MainActivity.heartbeatSend(webSocket, sequence)
        }
    }
}