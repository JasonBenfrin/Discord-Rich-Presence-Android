package tk.jasonbenfrin.discordrpc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

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
        MainActivity.statusSimilar(view, requireContext(), requireActivity(), parentFragmentManager)
    }
}