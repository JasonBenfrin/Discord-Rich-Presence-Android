package tk.jasonbenfrin.discordrpc

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.io.File

class PresenceFragment : Fragment() {
    private var receiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(p0: Context?, p1: Intent?) {
            val connect = view?.findViewById<ExtendedFloatingActionButton>(R.id.connect)
            connect?.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_connect_off)
            connect?.text = "Disconnect"
        }
    }

    private var connectReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            connectFancy(requireView())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.presence, container,false)
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireContext().registerReceiver(connectReceiver, IntentFilter("LoadConnect"))
        val viewPager2 = view.findViewById<ViewPager2>(R.id.viewpager)
        viewPager2.adapter = ViewPagerAdapter()
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        TabLayoutMediator(tabLayout, viewPager2) { tab , position ->
            when (position) {
                0 -> {
                    tab.text = resources.getText(R.string.status)
                }
                1 -> {
                    tab.text = resources.getText(R.string.rich_presence)
                }
                2 -> {
                    tab.text = resources.getText(R.string.load)
                }
            }
        }.attach()
        val connect = view.findViewById<ExtendedFloatingActionButton>(R.id.connect)
        tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tabLayout.selectedTabPosition == 2) {
                    connect.hide()
                }else{
                    connect.show()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        if(Service.SERVICE_RUNNING) {
            connect?.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_connect_off)
            connect?.text = "Disconnect"
            connect.setOnClickListener { disconnect(it) }
        }
        else connect.setOnClickListener { connectFancy(it) }
    }

    @SuppressLint("SetTextI18n")
    fun connectFancy(view: View) {
        val connect = view.findViewById<ExtendedFloatingActionButton>(R.id.connect)
        connect.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_connecting)
        connect.text = "Connecting..."
        connect.setOnClickListener { disconnect(it) }
        connect()
    }

    private fun connect() {
        try {
            if (MainActivity.getToken(requireContext()) == null) {
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
        MainActivity.CONNECTED = true
    }

    override fun onDestroy() {
        super.onDestroy()
        try { requireContext().unregisterReceiver(connectReceiver) } catch (e:Throwable) {}
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
        MainActivity.CONNECTED = false
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