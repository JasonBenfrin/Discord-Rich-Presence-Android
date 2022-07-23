package tk.jasonbenfrin.discordrpc

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class Service : Service() {
    private var heartbeat : Int = 0
    private var sequence : Int? = null
    private var sessionId : String = ""
    private var resume = false
    private lateinit var logFile : File
    private lateinit var webSocket: WebSocket
    private lateinit var heartbeatThread : Thread
    private lateinit var client :OkHttpClient
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate() {
        super.onCreate()
        logFile = File(baseContext.filesDir, "WebSocket.log")
        logFile.writeText("")
        log("Service onCreate()")
        val powerManager = baseContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "discordRPC:backgroundPresence")
        wakeLock.acquire()
        log("WakeLock Acquired")
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val builder = NotificationCompat.Builder(this, getString(R.string.notification_service_id))
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Discord Presence")
            .setContentText("Running in the background")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        startForeground(1, builder.build())
        log("Foreground service started, notification shown")
        client = OkHttpClient()
        client.newWebSocket(
            Request.Builder().url("wss://gateway.discord.gg/?v=10&encoding=json").build(),
            DiscordWebSocketListener()
        )
        client.dispatcher().executorService().shutdown()
        SERVICE_RUNNING = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log("Service onStartCommand()")
        if (this::webSocket.isInitialized) setPresence()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        log("Service Destroyed")
        if(this::webSocket.isInitialized) webSocket.close(1000, "Closed by user")
        SERVICE_RUNNING = false
        if (MainActivity.CONNECTED) {
            log("Accidental Service Destruction, restarting service")
            val intent = Intent(baseContext, Service::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                baseContext.startForegroundService(intent)
            } else {
                baseContext.startService(intent)
            }
        } else wakeLock.release()
    }

    override fun onBind(p0: Intent?): IBinder? = null

    inner class DiscordWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            this@Service.webSocket = webSocket
            log("WebSocket: Opened")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            val json = JsonParser.parseString(text).asJsonObject
            log("WebSocket: Received op code ${json.get("op")}")
            when (json.get("op").asInt) {
                0 -> {
                    if(json.has("s")) {
                        log("WebSocket: Sequence ${json.get("s")} Received")
                        sequence = json.get("s").asInt
                    }
                    if (json.get("t").asString != "READY") return
                    sessionId = json.get("d").asJsonObject.get("session_id").asString
                    log("WebSocket: SessionID ${json.get("d").asJsonObject.get("session_id")} Received")
                    setPresence()
                    sendBroadcast(Intent("ServiceToConnectButton"))
                }
                1 -> {
                    log("WebSocket: Received Heartbeat request, sending heartbeat")
                    heartbeatThread.interrupt()
                    MainActivity.heartbeatSend(webSocket, sequence)
                    heartbeatThread = Thread(HeartbeatRunnable())
                    heartbeatThread.start()
                }
                7 -> {
                    resume = true
                    log("WebSocket: Requested to Restart, restarting")
                    webSocket.close(1000, "Requested to Restart by the server")
                    client = OkHttpClient()
                    client.newWebSocket(
                        Request.Builder().url("wss://gateway.discord.gg/?v=10&encoding=json").build(),
                        DiscordWebSocketListener()
                    )
                    client.dispatcher().executorService().shutdown()
                }
                9 -> {
                    log("WebSocket: Invalid Session, restarting")
                    webSocket.close(1000, "Invalid Session")
                    Thread.sleep(5000)
                    client = OkHttpClient()
                    client.newWebSocket(
                        Request.Builder().url("wss://gateway.discord.gg/?v=10&encoding=json").build(),
                        DiscordWebSocketListener()
                    )
                    client.dispatcher().executorService().shutdown()
                }
                10 -> {
                    heartbeat = json.get("d").asJsonObject.get("heartbeat_interval").asInt
                    heartbeatThread = Thread(HeartbeatRunnable())
                    heartbeatThread.start()
                    if(resume) {
                        log("WebSocket: Resuming because server requested")
                        resume()
                        resume = false
                    } else {
                        MainActivity.identify(webSocket, baseContext)
                        log("WebSocket: Identified")
                    }
                }
                11 -> {
                    log("WebSocket: Heartbeat ACKed")
                    heartbeatThread = Thread(HeartbeatRunnable())
                    heartbeatThread.start()
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            t.message?.let { Log.d("WebSocket", "onFailure() $it") }
            log("WebSocket: Error, onFailure() reason: ${t.message}")
            client = OkHttpClient()
            client.newWebSocket(
                Request.Builder().url("wss://gateway.discord.gg/?v=10&encoding=json").build(),
                DiscordWebSocketListener()
            )
            client.dispatcher().executorService().shutdown()
            if(!heartbeatThread.isInterrupted) { heartbeatThread.interrupt() }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosing(webSocket, code, reason)
            Log.d("WebSocket", "onClosing() $code $reason")
            if(!heartbeatThread.isInterrupted) { heartbeatThread.interrupt() }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            Log.d("WebSocket", "onClosed() $code $reason")
            if(code >= 4000) {
                log("WebSocket: Error, code: $code reason: $reason")
                client = OkHttpClient()
                client.newWebSocket(
                    Request.Builder().url("wss://gateway.discord.gg/?v=10&encoding=json").build(),
                    DiscordWebSocketListener()
                )
                client.dispatcher().executorService().shutdown()
                return
            }
        }
    }

    private fun errorNotification(title : String, text: String) {
        val intent = Intent(this@Service, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val builder = NotificationCompat.Builder(this@Service, getString(R.string.notification_error_id))
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        notificationManager.notify(2, builder.build())
        log("Error Notified")
    }

    fun setPresence() {
        val file = File(baseContext.cacheDir, "payload")
        if(!file.exists()) {
            log("WebSocket: Payload file not found")
            errorNotification("Could not set the presence", "Please reset the presence")
            return
        }
        val d = JsonParser.parseString(file.readText(Charsets.UTF_8)).asJsonObject
        if(MainActivity.ACTIVITY_ENABLED) d.add("activities", JsonArray().apply {
            val activity = File(baseContext.cacheDir, "activity")
            if (!activity.exists()) {
                log("WebSocket: Activity file not found")
                errorNotification("Could not set the presence", "Please reset the presence")
                return
            }
            add(JsonParser.parseString(activity.readText(Charsets.UTF_8)).asJsonObject)
        })
        val payload = JsonObject()
        payload.addProperty("op", 3)
        payload.add("d", d)
        Log.d("WebSocket", payload.toString())
        webSocket.send(payload.toString())
        log("WebSocket: Payload sent $payload")
    }

    fun log(string: String) {
        val logLine = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.LONG).format(Calendar.getInstance().time) + ": " +string+ "\n"
        if(logFile.exists()) logFile.appendText(logLine) else logFile.writeText(logLine)
        sendBroadcast(Intent("ServiceLog").putExtra("new", logLine))
    }

    fun resume() {
        log("Sending Resume payload")
        val d = JsonObject()
        d.addProperty("token", MainActivity.getToken(baseContext))
        d.addProperty("session_id", sessionId)
        d.addProperty("seq", sequence)
        val json = JsonObject()
        json.addProperty("op", 6)
        json.add("d", d)
        log(json.toString())
        webSocket.send(json.toString())
    }

    inner class HeartbeatRunnable : Runnable {
        override fun run() {
            try {
                Thread.sleep(heartbeat.toLong())
                MainActivity.heartbeatSend(webSocket, sequence)
                log("WebSocket: Heartbeat Sent")
            } catch (e:InterruptedException) {}
        }
    }

    companion object {
        var SERVICE_RUNNING = false
    }
}