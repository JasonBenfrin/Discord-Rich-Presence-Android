package tk.jasonbenfrin.discordrpc

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigationrail.NavigationRailView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                    // TODO: Change to set Presence tab
                .add(R.id.fragmentContainerView, LoginFragment())
                .commit()
        }

        findViewById<BottomNavigationView>(R.id.bottomBar).setOnItemSelectedListener { item -> itemSelectedListener(item) }
        findViewById<NavigationRailView>(R.id.navRail).setOnItemSelectedListener{ item -> itemSelectedListener(item) }
    }

    // UI: Set NavigationBars' selected item on orientation and refresh
    @SuppressLint("CutPasteId")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val bottomBar : BottomNavigationView = findViewById(R.id.bottomBar)
        val navRail : NavigationRailView = findViewById(R.id.navRail)
        val previousFragment : Fragment? = supportFragmentManager.findFragmentById(R.id.fragmentContainerView)
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
    }

    private fun itemSelectedListener( item : MenuItem ) : Boolean {
        val fragmentManager = supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
        when (item.itemId) {
            R.id.login -> fragmentManager.replace(R.id.fragmentContainerView, LoginFragment())
            R.id.connect -> fragmentManager.replace(R.id.fragmentContainerView, ConnectFragment())
            else -> return false
        }
        fragmentManager.commit()
        return true
    }

    class LoginFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
            super.onCreateView(inflater, container, savedInstanceState)
            return inflater.inflate(R.layout.login, container,false)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            //TODO: set WebView visibility to VISIBLE when token is not found
        }
    }

    class ConnectFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
            super.onCreateView(inflater, container, savedInstanceState)
            return inflater.inflate(R.layout.connect, container,false)
        }
    }
}