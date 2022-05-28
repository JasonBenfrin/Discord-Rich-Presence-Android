package tk.jasonbenfrin.discordrpc

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigationrail.NavigationRailView
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewPager2 : ViewPager2 = findViewById(R.id.viewpager)
        val bottomBar : BottomNavigationView = findViewById(R.id.bottomBar)
        val navRail : NavigationRailView = findViewById(R.id.navRail)

        viewPager2.isUserInputEnabled = false
        viewPager2.offscreenPageLimit = 1
        viewPager2.adapter = ViewPagerAdapter(this)

        bottomBar.setOnItemSelectedListener { item -> itemSelectedListener(item) }
        navRail.setOnItemSelectedListener { item -> itemSelectedListener(item) }

        bottomBar.selectedItemId = R.id.set
    }

    // UI: Set NavigationBars' selected item on orientation
    @SuppressLint("CutPasteId")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val bottomBar : BottomNavigationView = findViewById(R.id.bottomBar)
        val navRail : NavigationRailView = findViewById(R.id.navRail)
        setContentView(R.layout.activity_main)
        // This is not Cut & Paste Error. The View is reloaded and needs to be refreshed
        val newBottomBar : BottomNavigationView = findViewById(R.id.bottomBar)
        val newNavRail : NavigationRailView = findViewById(R.id.navRail)
        val viewPager2 : ViewPager2 = findViewById(R.id.viewpager)

        viewPager2.isUserInputEnabled = false
        viewPager2.offscreenPageLimit = 1
        viewPager2.adapter = ViewPagerAdapter(this)
        viewPager2.setPageTransformer(FadeTransformer())

        bottomBar.setOnItemSelectedListener { item -> itemSelectedListener(item) }
        navRail.setOnItemSelectedListener { item -> itemSelectedListener(item) }

        when(newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                newNavRail.selectedItemId = bottomBar.selectedItemId
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                newBottomBar.selectedItemId = navRail.selectedItemId
            }
            Configuration.ORIENTATION_SQUARE -> {
                newBottomBar.selectedItemId = bottomBar.selectedItemId
                newNavRail.visibility = View.GONE
            }
            Configuration.ORIENTATION_UNDEFINED -> {
                newBottomBar.selectedItemId = bottomBar.selectedItemId
                newNavRail.visibility = View.GONE
            }
        }
    }

    private fun itemSelectedListener(item : MenuItem) : Boolean {
        val view : ViewPager2 = findViewById(R.id.viewpager)
        when(item.itemId) {
            R.id.login -> {
                view.setCurrentItem(0, false)
                return true
            }
            R.id.connect -> {
                view.setCurrentItem(1, false)
                return true
            }
            R.id.set -> {
                view.setCurrentItem(2, false)
                return true
            }
            R.id.store -> {
                view.setCurrentItem(3, false)
                return true
            }
            R.id.about -> {
                view.setCurrentItem(4, false)
                return true
            }
            else -> return false
        }
    }

    class ViewPagerAdapter(fragmentActivity : FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> {
                    /* TODO: Expand WebView when token is not found in Storage
                    *   Set Image as Discord User Image*/
                    LoginFragment()
                }
                else -> LoginFragment()
            }
        }
    }

    // UI: Login View
    class LoginFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
            super.onCreateView(inflater, container, savedInstanceState)
            return inflater.inflate(R.layout.login, container,false)
        }
    }

    class FadeTransformer : ViewPager2.PageTransformer {
        override fun transformPage(view: View, position: Float) {
            Log.d("ViewPagerBug",position.toString())
            if(position <= -1.0F || position >= 1.0F) {
                view.translationX = view.width * position
                view.alpha = 0.0F
            } else if( position == 0.0F ) {
                view.translationX = view.width * position
                view.alpha = 1.0F
            } else {
                // position is between -1.0F & 0.0F OR 0.0F & 1.0F
                view.translationX = view.width * -position
                view.alpha = 1.0F - abs(position)
            }
        }

    }
}