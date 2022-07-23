package tk.jasonbenfrin.discordrpc

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class Load : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.load, container,false)
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.save).setOnClickListener {
            var input = EditText(requireContext()).apply {
                inputType = InputType.TYPE_CLASS_TEXT
            }
            val builder = MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle("Name")
                setCancelable(false)
                setMessage("Provide name to save")
                setView(input)
                setPositiveButton("Save") { dialog, _ ->
                    dialog.cancel()
                    val payload = File(requireContext().cacheDir, "payload")
                    val activity = File(requireContext().cacheDir, "activity")
                    val file = File(requireContext().filesDir, "saved"+ File.separator+input.text.toString())
                    if(file.createNewFile()) {
                        val load = view.findViewById<LinearLayout>(R.id.loadScroll)
                        file.writeText(payload.readText(Charsets.UTF_8) + "\n" + activity.readText(Charsets.UTF_8) + "\n" + MainActivity.ACTIVITY_ENABLED.toString())
                        val loadButton = MaterialButton(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            text = "Load"
                            id = MainActivity.generateViewId()
                            setOnClickListener {
                                val loadFile = file.readText(Charsets.UTF_8)
                                payload.writeText(loadFile.split("\n")[0])
                                activity.writeText(loadFile.split("\n")[1])
                                MainActivity.ACTIVITY_ENABLED = loadFile.split("\n")[2].toBoolean()
                                requireContext().sendBroadcast(Intent("LoadConnect"))
                            }
                        }
                        lateinit var loadLayout : ConstraintLayout
                        val deleteButton = MaterialButton(requireContext(), null, R.attr.hollowButtonStyle).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            text = "Delete"
                            id = MainActivity.generateViewId()
                            setOnClickListener {
                                load.removeView(loadLayout)
                                file.delete()
                            }
                        }
                        val textLoad = TextView(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                                setMargins(0, 0, 24, 0)
                            }
                            text = file.name
                            gravity = Gravity.CENTER_VERTICAL
                            setTextColor(ContextCompat.getColor(requireContext(),R.color.text))
                            textSize = 18F
                            id = MainActivity.generateViewId()
                        }
                        loadLayout = ConstraintLayout(requireContext()).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            id = MainActivity.generateViewId()
                            addView(textLoad)
                            addView(loadButton)
                            addView(deleteButton)
                        }
                        val constraintSet = ConstraintSet()
                        constraintSet.clone(loadLayout)
                        constraintSet.apply {
                            connect(textLoad.id, ConstraintSet.TOP, loadLayout.id, ConstraintSet.TOP)
                            connect(textLoad.id, ConstraintSet.LEFT, loadLayout.id, ConstraintSet.LEFT)
                            connect(textLoad.id, ConstraintSet.RIGHT, loadButton.id, ConstraintSet.LEFT)
                            connect(textLoad.id, ConstraintSet.BOTTOM, loadLayout.id, ConstraintSet.BOTTOM)
                            connect(loadButton.id, ConstraintSet.TOP, loadLayout.id, ConstraintSet.TOP)
                            connect(loadButton.id, ConstraintSet.RIGHT, deleteButton.id, ConstraintSet.LEFT)
                            connect(deleteButton.id, ConstraintSet.TOP, loadLayout.id, ConstraintSet.TOP)
                            connect(deleteButton.id, ConstraintSet.RIGHT, loadLayout.id, ConstraintSet.RIGHT)
                        }
                        constraintSet.applyTo(loadLayout)
                        load.addView(loadLayout)
                    }
                    else {
                        input = EditText(requireContext()).apply {
                            inputType = InputType.TYPE_CLASS_TEXT
                        }
                        this.setMessage("A save with the same name was found\nPlease provide another name").setView(input).show()
                    }
                }
                setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
            }
            builder.show()
        }
        val load = view.findViewById<LinearLayout>(R.id.loadScroll)
        val dir = File(requireContext().filesDir, "saved")
        if(dir.exists()) {
            val listOfFiles = dir.listFiles()
            if(dir.isDirectory && listOfFiles != null) {
                Thread{
                    for (child in listOfFiles) {
                        if (!child.isFile) continue
                        lateinit var loadLayout : ConstraintLayout
                        val loadButton = MaterialButton(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            text = "Load"
                            id = MainActivity.generateViewId()
                            setOnClickListener {
                                val payload = File(requireContext().cacheDir, "payload")
                                val activity = File(requireContext().cacheDir, "activity")
                                val loadFile = child.readText(Charsets.UTF_8)
                                try{
                                    payload.writeText(loadFile.split("\n")[0])
                                    activity.writeText(loadFile.split("\n")[1])
                                    MainActivity.ACTIVITY_ENABLED = loadFile.split("\n")[2].toBoolean()
                                } catch (_:Exception) {}
                                requireContext().sendBroadcast(Intent("LoadConnect"))
                            }
                        }
                        val deleteButton = MaterialButton(requireContext(), null, R.attr.hollowButtonStyle).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            text = "Delete"
                            id = MainActivity.generateViewId()
                            setOnClickListener {
                                try {requireActivity().runOnUiThread{ load.removeView(loadLayout) }} catch (e:Throwable) {}
                                child.delete()
                            }
                        }
                        val textLoad = TextView(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                                setMargins(0, 0, 24, 0)
                            }
                            text = child.name
                            gravity = Gravity.CENTER_VERTICAL
                            setTextColor(ContextCompat.getColor(requireContext(),R.color.text))
                            textSize = 18F
                            id = MainActivity.generateViewId()
                        }
                        loadLayout = ConstraintLayout(requireContext()).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            id = MainActivity.generateViewId()
                            addView(textLoad)
                            addView(loadButton)
                            addView(deleteButton)
                        }
                        val constraintSet = ConstraintSet()
                        constraintSet.clone(loadLayout)
                        constraintSet.apply {
                            connect(textLoad.id, ConstraintSet.TOP, loadLayout.id, ConstraintSet.TOP)
                            connect(textLoad.id, ConstraintSet.LEFT, loadLayout.id, ConstraintSet.LEFT)
                            connect(textLoad.id, ConstraintSet.RIGHT, loadButton.id, ConstraintSet.LEFT)
                            connect(textLoad.id, ConstraintSet.BOTTOM, loadLayout.id, ConstraintSet.BOTTOM)
                            connect(loadButton.id, ConstraintSet.TOP, loadLayout.id, ConstraintSet.TOP)
                            connect(loadButton.id, ConstraintSet.RIGHT, deleteButton.id, ConstraintSet.LEFT)
                            connect(deleteButton.id, ConstraintSet.TOP, loadLayout.id, ConstraintSet.TOP)
                            connect(deleteButton.id, ConstraintSet.RIGHT, loadLayout.id, ConstraintSet.RIGHT)
                        }
                        constraintSet.applyTo(loadLayout)
                        try { requireActivity().runOnUiThread { load.addView(loadLayout) } } catch (e:Throwable) {}
                    }
                }.start()
            }else{
                dir.delete()
                dir.mkdir()
            }
        }else{
            dir.mkdir()
        }
        Thread{
            for (i in 1..20) {
                lateinit var loadLayout : ConstraintLayout
                val loadButton = MaterialButton(requireContext()).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    text = "Load"
                    id = MainActivity.generateViewId()
                }
                val deleteButton = MaterialButton(requireContext(), null, R.attr.hollowButtonStyle).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    text = "Delete"
                    id = MainActivity.generateViewId()
                    setOnClickListener {
                        try {requireActivity().runOnUiThread{ load.removeView(loadLayout) }} catch (e:Throwable) {}
                    }
                }
                val textLoad = TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 24, 0)
                    }
                    text = "child.name"
                    gravity = Gravity.CENTER_VERTICAL
                    setTextColor(ContextCompat.getColor(requireContext(),R.color.text))
                    textSize = 18F
                    id = MainActivity.generateViewId()
                }
                loadLayout = ConstraintLayout(requireContext()).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    id = MainActivity.generateViewId()
                    addView(textLoad)
                    addView(loadButton)
                    addView(deleteButton)
                }
                val constraintSet = ConstraintSet()
                constraintSet.clone(loadLayout)
                constraintSet.apply {
                    connect(textLoad.id, ConstraintSet.TOP, loadLayout.id, ConstraintSet.TOP)
                    connect(textLoad.id, ConstraintSet.LEFT, loadLayout.id, ConstraintSet.LEFT)
                    connect(textLoad.id, ConstraintSet.RIGHT, loadButton.id, ConstraintSet.LEFT)
                    connect(textLoad.id, ConstraintSet.BOTTOM, loadLayout.id, ConstraintSet.BOTTOM)
                    connect(loadButton.id, ConstraintSet.TOP, loadLayout.id, ConstraintSet.TOP)
                    connect(loadButton.id, ConstraintSet.RIGHT, deleteButton.id, ConstraintSet.LEFT)
                    connect(deleteButton.id, ConstraintSet.TOP, loadLayout.id, ConstraintSet.TOP)
                    connect(deleteButton.id, ConstraintSet.RIGHT, loadLayout.id, ConstraintSet.RIGHT)
                }
                constraintSet.applyTo(loadLayout)
                try { requireActivity().runOnUiThread { load.addView(loadLayout) } } catch (e:Throwable) {}
            }
        } //this is just for testing
    }
}