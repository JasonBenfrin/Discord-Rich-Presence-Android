package tk.jasonbenfrin.discordrpc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.io.File


class LogViewer : Fragment() {
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