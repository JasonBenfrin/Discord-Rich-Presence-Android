package tk.jasonbenfrin.discordrpc

import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.*
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.util.*

class RichPresence : Fragment() {
    private val timestampJsonObject = JsonObject()

    override fun onResume() {
        super.onResume()
        updateLayout(requireView())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.rich_presence, container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val file = File(requireContext().cacheDir, "activity")
        lateinit var json : JsonObject
        if(!file.exists()) {
            json = JsonObject().apply {
                addProperty("name", "")
                addProperty("type", 0)
                addProperty("created_at", System.currentTimeMillis())
            }
            file.writeText(json.toString())
        }else json = JsonParser.parseString(file.readText(Charsets.UTF_8)).asJsonObject
        super.onViewCreated(view, savedInstanceState)
        val activity = view.findViewById<SwitchMaterial>(R.id.activity)
        activitySwitch(View.GONE, view)
        view.findViewById<SwitchMaterial>(R.id.showAll).setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) {
                showAllSwitch(View.VISIBLE, view)
            }else{
                showAllSwitch(View.GONE, view)
            }
        }
        activity.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) {
                MainActivity.ACTIVITY_ENABLED = true
                activitySwitch(View.VISIBLE, view)
            }else{
                MainActivity.ACTIVITY_ENABLED = false
                activitySwitch(View.GONE, view)
            }
        }
        val activityURL = view.findViewById<EditText>(R.id.activityURL)
        val emojiId = view.findViewById<EditText>(R.id.activityEmojiId)
        val emojiName = view.findViewById<EditText>(R.id.activityEmojiName)
        val emojiAnimated = view.findViewById<CheckBox>(R.id.activityEmojiAnimated)
        val emojiJsonObject = JsonObject().apply {
            addProperty("name", "question")
        }
        val partyJsonObject = JsonObject()
        val assetsJsonObject = JsonObject()
        val secretsJsonObject = JsonObject()
        var partySizeMin : Int? = null
        var partySizeMax : Int? = null
        var button1Text = ""
        var button1URL = ""
        var button2Text = ""
        var button2URL = ""
        view.findViewById<EditText>(R.id.activityName).apply {
            addTextChangedListener { jsonUpdate("name", it.toString(), "", json, file) }
        }
        view.findViewById<EditText>(R.id.activityDetails).apply {
            addTextChangedListener { jsonUpdate("details", it.toString(), null, json, file) }
        }
        view.findViewById<Spinner>(R.id.activityType).apply {
            adapter = ArrayAdapter.createFromResource(requireContext(), R.array.activity_types, android.R.layout.simple_spinner_item).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, int: Int, p3: Long) {
                    when (int) {
                        1 -> {
                            activityURL.visibility = View.VISIBLE
                            emojiId.visibility = View.GONE
                            emojiName.visibility = View.GONE
                            emojiAnimated.visibility = View.GONE
                        }
                        4 -> {
                            activityURL.visibility = View.GONE
                            emojiId.visibility = View.VISIBLE
                            emojiName.visibility = View.VISIBLE
                            emojiAnimated.visibility = View.VISIBLE
                        }
                        else -> {
                            activityURL.visibility = View.GONE
                            emojiId.visibility = View.GONE
                            emojiName.visibility = View.GONE
                            emojiAnimated.visibility = View.GONE
                            emojiId.setText("")
                            emojiName.setText("")
                            jsonRemove("emoji", json, file)
                        }
                    }
                    jsonUpdate("type", int, json, file)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
        }
        activityURL.addTextChangedListener { jsonUpdate("url", it.toString(), null, json, file) }
        emojiId.addTextChangedListener { jsonUpdate("emoji", emojiJsonObject.apply {
            val string: String? = if (it.toString() == "") null else it.toString()
            addProperty("id", string)
        }, json, file) }
        emojiName.addTextChangedListener { jsonUpdate("emoji", emojiJsonObject.apply {
            addProperty("name", if(it.toString() == "") "question" else it.toString())
        }, json, file) }
        emojiAnimated.setOnCheckedChangeListener { _, b ->
            emojiJsonObject.addProperty("animated", b)
        }
        view.findViewById<Button>(R.id.activityTimestampStart).apply {
            setOnClickListener {
                timePicker(
                    "Timestamp Start",
                    null,
                    0
                    , json, file)
            }
        }
        view.findViewById<Button>(R.id.activityTimestampEnd).apply {
            setOnClickListener {
                timePicker(
                    "Timestamp End",
                    null,
                    1
                    , json, file)
            }
        }
        view.findViewById<EditText>(R.id.activityApplicationId).apply {
            addTextChangedListener {
                jsonUpdate(
                    "application_id",
                    it.toString(),
                    null
                    , json, file)
            }
        }
        view.findViewById<EditText>(R.id.activityPartyState).apply {
            addTextChangedListener {
                jsonUpdate(
                    "state",
                    it.toString(),
                    null
                    , json, file)
            }
        }
        view.findViewById<EditText>(R.id.activityPartyId).apply {
            addTextChangedListener {
                jsonUpdate(
                    "party",
                    partyJsonObject.apply { addProperty("id", if(it.toString() == "") null else it.toString()) }, json, file)
            }
        }
        view.findViewById<EditText>(R.id.activityPartySizeMin).apply {
            addTextChangedListener {
                partySizeMin = if (it.toString() == "") null else it.toString().toInt()
                jsonUpdate("party", partyJsonObject.apply {
                    if (partySizeMin != null && partySizeMax != null) {
                        add("size", JsonArray().apply {
                            add(partySizeMin)
                            add(partySizeMax)
                        })
                    }else{
                        remove("size")
                    }
                }, json, file)
            }
        }
        view.findViewById<EditText>(R.id.activityPartySizeMax).apply {
            addTextChangedListener {
                partySizeMax = if (it.toString() == "") null else it.toString().toInt()
                jsonUpdate("party", partyJsonObject.apply {
                    if (partySizeMin != null && partySizeMax != null) {
                        add("size", JsonArray().apply {
                            add(partySizeMin)
                            add(partySizeMax)
                        })
                    }else{
                        remove("size")
                    }
                }, json, file)
            }
        }
        view.findViewById<EditText>(R.id.activityLargeImage).apply {
            addTextChangedListener {
                jsonUpdate("assets", assetsJsonObject.apply {
                    addProperty("large_image", if (it.toString() == "") null else urlResolver(it.toString()))
                }, json, file)
            }
        }
        view.findViewById<EditText>(R.id.activityLargeText).apply {
            addTextChangedListener {
                jsonUpdate("assets", assetsJsonObject.apply {
                    addProperty("large_text", if (it.toString() == "") null else it.toString())
                }, json, file)
            }
        }
        view.findViewById<EditText>(R.id.activitySmallImage).apply {
            addTextChangedListener {
                jsonUpdate("assets", assetsJsonObject.apply {
                    addProperty("small_image", if (it.toString() == "") null else urlResolver(it.toString()))
                }, json, file)
            }
        }
        view.findViewById<EditText>(R.id.activitySmallText).apply {
            addTextChangedListener {
                jsonUpdate("assets", assetsJsonObject.apply {
                    addProperty("small_text", if (it.toString() == "") null else it.toString())
                }, json, file)
            }
        }
        view.findViewById<CheckBox>(R.id.activityInstanced).apply {
            setOnCheckedChangeListener { _, b ->
                jsonUpdate("instance", b, json, file)
            }
        }
        view.findViewById<EditText>(R.id.activitySecretJoin).apply {
            addTextChangedListener {
                jsonUpdate("secrets", secretsJsonObject.apply {
                    addProperty("join", if (it.toString() == "") null else it.toString())
                }, json, file)
                secretUpdate(secretsJsonObject, file)
            }
        }
        view.findViewById<EditText>(R.id.activitySecretSpectate).apply {
            addTextChangedListener {
                jsonUpdate("secrets", secretsJsonObject.apply {
                    addProperty("spectate", if (it.toString() == "") null else it.toString())
                }, json, file)
                secretUpdate(secretsJsonObject, file)
            }
        }
        view.findViewById<EditText>(R.id.activitySecretMatch).apply {
            addTextChangedListener {
                jsonUpdate("secrets", secretsJsonObject.apply {
                    addProperty("match", if (it.toString() == "") null else it.toString())
                }, json, file)
                secretUpdate(secretsJsonObject, file)
            }
        }
        view.findViewById<Button>(R.id.activityCreatedAt).apply {
            setOnClickListener {
                timePicker("Created At", false, 2, json, file)
            }
        }
        view.findViewById<EditText>(R.id.activityButton1Label).apply {
            addTextChangedListener {
                button1Text = it.toString()
                buttonUpdate(button1Text, button2Text, json, file)
            }
        }
        view.findViewById<EditText>(R.id.activityButton1URL).apply{
            addTextChangedListener {
                button1URL = it.toString()
                buttonURLUpdate(button1URL, button2URL, json, file)
            }
        }
        view.findViewById<EditText>(R.id.activityButton2Label).apply {
            addTextChangedListener {
                button2Text = it.toString()
                buttonUpdate(button1Text, button2Text, json, file)
            }
        }
        view.findViewById<EditText>(R.id.activityButton2URL).apply{
            addTextChangedListener {
                button2URL = it.toString()
                buttonURLUpdate(button1URL, button2URL, json, file)
            }
        }
        view.findViewById<EditText>(R.id.activityFlags).apply {
            addTextChangedListener {
                if(it.toString() != "") jsonUpdate("flags", it.toString().toInt(), json, file)
                else jsonRemove("flags", json, file)
            }
        }
        updateLayout(view)
    }

    private fun updateLayout(view: View) {
        val file = File(requireContext().cacheDir, "activity")
        if (file.exists()) {
            val json = JsonParser.parseString(file.readText(Charsets.UTF_8)).asJsonObject
            if (json.has("name") && !json.get("name").isJsonNull) view.findViewById<EditText>(R.id.activityName).setText(json.get("name").asString, TextView.BufferType.EDITABLE)
            if (json.has("type") && !json.get("type").isJsonNull) view.findViewById<Spinner>(R.id.activityType).setSelection(json.get("type").asInt)
            if (json.has("url") && !json.get("url").isJsonNull) view.findViewById<EditText>(R.id.activityURL).setText(json.get("url").asString, TextView.BufferType.EDITABLE)
            if (json.has("application_id") && !json.get("application_id").isJsonNull) view.findViewById<EditText>(R.id.activityApplicationId).setText(json.get("application_id").asString,
                TextView.BufferType.EDITABLE)
            if (json.has("details") && !json.get("details").isJsonNull) view.findViewById<EditText>(R.id.activityDetails).setText(json.get("details").asString,
                TextView.BufferType.EDITABLE)
            if (json.has("state") && !json.get("state").isJsonNull) view.findViewById<EditText>(R.id.activityPartyState).setText(json.get("state").asString,
                TextView.BufferType.EDITABLE)
            if (json.has("emoji") && !json.get("emoji").isJsonNull) {
                val emoji = json.get("emoji").asJsonObject
                if (emoji.has("name") && !emoji.get("name").isJsonNull) view.findViewById<EditText>(R.id.activityEmojiName).setText(emoji.get("name").asString,
                    TextView.BufferType.EDITABLE)
                if (emoji.has("id") && !emoji.get("id").isJsonNull) view.findViewById<EditText>(R.id.activityEmojiId).setText(emoji.get("id").asString, TextView.BufferType.EDITABLE)
                if (emoji.has("animated") && !emoji.get("animated").isJsonNull) view.findViewById<CheckBox>(R.id.activityEmojiAnimated).isChecked = emoji.get("animated").asBoolean
            }
            if (json.has("party") && !json.get("party").isJsonNull) {
                val party = json.get("party").asJsonObject
                if (party.has("id") && !party.get("id").isJsonNull) view.findViewById<EditText>(R.id.activityPartyId).setText(party.get("id").asString,
                    TextView.BufferType.EDITABLE)
                if (party.has("size") && !party.get("size").isJsonNull) {
                    val partySize = party.get("size").asJsonArray
                    view.findViewById<EditText>(R.id.activityPartySizeMin).setText(partySize[0].asString,
                        TextView.BufferType.EDITABLE)
                    view.findViewById<EditText>(R.id.activityPartySizeMax).setText(partySize[1].asString,
                        TextView.BufferType.EDITABLE)
                }
            }
            if (json.has("assets") && !json.get("assets").isJsonNull) {
                val assets = json.get("assets").asJsonObject
                if (assets.has("large_image") && !assets.get("large_image").isJsonNull) view.findViewById<EditText>(R.id.activityLargeImage).setText(assets.get("large_image").asString,
                    TextView.BufferType.EDITABLE)
                if (assets.has("large_text") && !assets.get("large_text").isJsonNull) view.findViewById<EditText>(R.id.activityLargeText).setText(assets.get("large_text").asString,
                    TextView.BufferType.EDITABLE)
                if (assets.has("small_image") && !assets.get("small_image").isJsonNull) view.findViewById<EditText>(R.id.activitySmallImage).setText(assets.get("small_image").asString,
                    TextView.BufferType.EDITABLE)
                if (assets.has("small_text") && !assets.get("small_text").isJsonNull) view.findViewById<EditText>(R.id.activitySmallText).setText(assets.get("small_text").asString,
                    TextView.BufferType.EDITABLE)
            }
            if (json.has("secrets") && !json.get("secrets").isJsonNull) {
                val secrets = json.get("secrets").asJsonObject
                if (secrets.has("join") && !secrets.get("join").isJsonNull) view.findViewById<EditText>(R.id.activitySecretJoin).setText(secrets.get("join").asString,
                    TextView.BufferType.EDITABLE)
                if (secrets.has("spectate") && !secrets.get("spectate").isJsonNull) view.findViewById<EditText>(R.id.activitySecretSpectate).setText(secrets.get("spectate").asString,
                    TextView.BufferType.EDITABLE)
                if (secrets.has("match") && !secrets.get("match").isJsonNull) view.findViewById<EditText>(R.id.activitySecretMatch).setText(secrets.get("match").asString,
                    TextView.BufferType.EDITABLE)
            }
            if (json.has("instance") && !json.get("instance").isJsonNull) view.findViewById<CheckBox>(R.id.activityInstanced).isChecked = json.get("instance").asBoolean
            if (json.has("buttons") && !json.get("buttons").isJsonNull) {
                val buttons = json.get("buttons").asJsonArray
                if (buttons.size() == 1) {
                    view.findViewById<EditText>(R.id.activityButton1Label).setText(buttons[0].asString,
                        TextView.BufferType.EDITABLE)
                } else {
                    view.findViewById<EditText>(R.id.activityButton1Label).setText(buttons[0].asString,
                        TextView.BufferType.EDITABLE)
                    view.findViewById<EditText>(R.id.activityButton2Label).setText(buttons[1].asString,
                        TextView.BufferType.EDITABLE)
                }
            }
            if (json.has("metadata") && !json.get("metadata").isJsonNull) {
                val metadata = json.get("metadata").asJsonObject
                if (metadata.has("button_urls") && !metadata.get("button_urls").isJsonNull) {
                    val urls = json.get("metadata").asJsonObject.get("button_urls").asJsonArray
                    if (urls.size() == 1) {
                        view.findViewById<EditText>(R.id.activityButton1URL).setText(urls[0].asString,
                            TextView.BufferType.EDITABLE)
                    } else {
                        view.findViewById<EditText>(R.id.activityButton1URL).setText(urls[0].asString,
                            TextView.BufferType.EDITABLE)
                        view.findViewById<EditText>(R.id.activityButton2URL).setText(urls[1].asString,
                            TextView.BufferType.EDITABLE)
                    }
                }
            }
            if (json.has("flags") && !json.get("flags").isJsonNull) view.findViewById<EditText>(R.id.activityFlags).setText(json.get("flags").asString,
                TextView.BufferType.EDITABLE)
            view.findViewById<SwitchMaterial>(R.id.activity).isChecked =
                MainActivity.ACTIVITY_ENABLED
        }
    }

    private fun urlResolver(url:String) : String {
        val formattedURL : String = url.removePrefix("https://").removePrefix("http://")
        if(url.contains("mp")) return url
        if(url.contains("cdn.discordapp.com")) return "mp:" + formattedURL.removePrefix("cdn.discordapp.com/")
        if(url.contains("media.discordapp.net")) return "mp:" + formattedURL.removePrefix("media.discordapp.net/")
        return "mp:$url"
    }

    private fun buttonUpdate(text1: String, text2: String, json: JsonObject, file: File) {
        jsonUpdate("buttons", JsonArray().apply {
            if (text1 != "") add(text1)
            if (text2 != "") add(text2)
        }, json, file)
        if(text1 == "" && text2 == "" && json.has("buttons")) jsonRemove("buttons", json, file)
    }

    private fun buttonURLUpdate(url1: String, url2: String, json: JsonObject, file: File) {
        jsonUpdate("metadata", JsonObject().apply {
            if(url1 != "" || url2 != "") {
                if (!json.has("application_id")) jsonUpdate("application_id", "978135236372234282", "", json, file)
                add("button_urls", JsonArray().apply {
                    if (url1 != "") add(url1)
                    if (url2 != "") add(url2)
                })
            }
        }, json, file)
        if(url1 == "" && url2 == "" && json.has("metadata") && json.get("metadata").asJsonObject.has("button_urls")) {
            jsonRemove("metadata", json, file)
            if (json.get("application_id").asString == "978135236372234282") jsonRemove("application_id", json, file)
        }
    }

    private fun secretUpdate(json: JsonObject, file: File) {
        if(json.has("join") && json.has("spectate") && json.has("match")) {
            if(json.get("join").isJsonNull && json.get("spectate").isJsonNull && json.get("match").isJsonNull) jsonRemove("secrets", json, file)
        }else{
            jsonRemove("secrets", json, file)
        }
    }

    private fun timePicker(property: String, backwards : Boolean?, type: Int, json: JsonObject, file: File) {
        var hours : Int? = null
        var minutes : Int? = null
        var day : Long? = null
        val calendar = Calendar.getInstance()
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(if(DateFormat.is24HourFormat(activity)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setTitleText("\"$property\" property")
            .setPositiveButtonText("Set")
            .setNegativeButtonText("Reset")
            .build()
        timePicker.show(parentFragmentManager, "TimePicker")
        timePicker.addOnPositiveButtonClickListener {
            hours = timePicker.hour
            minutes = timePicker.minute
            day = MaterialDatePicker.todayInUtcMilliseconds() - TimeZone.getDefault().getOffset(Date().time)
            val constraintsBuilderRange = CalendarConstraints.Builder()
            val listOfValidators = ArrayList<CalendarConstraints.DateValidator>()
            if(backwards == true) listOfValidators.add(DateValidatorPointBackward.now())
            else if(backwards == false) listOfValidators.add(DateValidatorPointForward.now())
            val validators = CompositeDateValidator.allOf(listOfValidators)
            constraintsBuilderRange.setValidator(validators)
            val datePickerBuilder = MaterialDatePicker.Builder.datePicker()
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setTitleText("\"$property\" property")
                .setPositiveButtonText("Set")
                .setNegativeButtonText("Reset")
            if (backwards != null) datePickerBuilder.setCalendarConstraints(constraintsBuilderRange.build())
            val datePicker = datePickerBuilder.build()
            datePicker.show(parentFragmentManager, "DatePicker")
            datePicker.addOnPositiveButtonClickListener {
                day = it - TimeZone.getDefault().getOffset(Date().time)
                dateTimePickerJsonResolver(type, calculateMilliSeconds(day, hours, minutes), json, file)
            }
            datePicker.addOnNegativeButtonClickListener {
                dateTimePickerJsonResolver(type, calculateMilliSeconds(day, hours, minutes), json, file)
            }
        }
        timePicker.addOnNegativeButtonClickListener {
            dateTimePickerJsonResolver(type, calculateMilliSeconds(day, hours, minutes), json, file)
        }
    }

    private fun calculateMilliSeconds(day: Long?, hours: Int?, minutes: Int?): Long? {
        return if(day == null && hours == null && minutes == null) null
        else day!! + (hours!! * 60 + minutes!!) * 60000
    }

    private fun dateTimePickerJsonResolver(type: Int, time: Long?, json: JsonObject, file: File) {
        when (type) {
            0 -> jsonUpdate("timestamps", timestampJsonObject.apply { addProperty("start", time) }, json, file)
            1 -> jsonUpdate("timestamps", timestampJsonObject.apply { addProperty("end", time) }, json, file)
            2 -> jsonUpdate("created_at", time ?: System.currentTimeMillis(), json, file)
        }
    }

    private fun jsonUpdate(property: String, key: String, fallback: String?, json: JsonObject, file: File) {
        if(key != "") json.addProperty(property, key) else {
            if(fallback == null) {
                jsonRemove(property, json, file)
            }else{
                json.addProperty(property, fallback)
            }
        }
        file.writeText(json.toString())
    }

    private fun jsonUpdate(property: String, key: Int, json: JsonObject, file: File) {
        json.addProperty(property, key)
        file.writeText(json.toString())
    }

    private fun jsonUpdate(property: String, key: Boolean, json: JsonObject, file: File) {
        json.addProperty(property, key)
        file.writeText(json.toString())
    }

    private fun jsonUpdate(property: String, key: Long?, json: JsonObject, file: File) {
        json.addProperty(property, key)
        file.writeText(json.toString())
    }

    private fun jsonUpdate(property: String, key: JsonElement, json: JsonObject, file: File) {
        json.add(property, key)
        file.writeText(json.toString())
    }

    private fun jsonRemove(property: String, json: JsonObject, file: File) {
        json.remove(property)
        file.writeText(json.toString())
    }

    private fun showAllSwitch(visibility: Int, view: View) {
        view.findViewById<EditText>(R.id.activityApplicationId).visibility = visibility
        view.findViewById<EditText>(R.id.activityPartyId).visibility = visibility
        view.findViewById<EditText>(R.id.activitySecretJoin).visibility = visibility
        view.findViewById<EditText>(R.id.activitySecretSpectate).visibility = visibility
        view.findViewById<EditText>(R.id.activitySecretMatch).visibility = visibility
        view.findViewById<CheckBox>(R.id.activityInstanced).visibility = visibility
        view.findViewById<Button>(R.id.activityCreatedAt).visibility = visibility
        view.findViewById<EditText>(R.id.activityFlags).visibility = visibility
    }

    private fun activitySwitch(visibility : Int, view: View) {
        // I have no idea how to make this more efficient, if you do, please create an issue at https://github.com/JasonBenfrin/Discord-Rich-Presence-Android please
        val spinner = view.findViewById<Spinner>(R.id.activityType)
        val url = view.findViewById<EditText>(R.id.activityURL)
        val emojiId = view.findViewById<EditText>(R.id.activityEmojiId)
        val emojiName = view.findViewById<EditText>(R.id.activityEmojiName)
        val emojiAnimated = view.findViewById<CheckBox>(R.id.activityEmojiAnimated)
        view.findViewById<SwitchMaterial>(R.id.showAll).visibility = visibility
        view.findViewById<EditText>(R.id.activityName).visibility = visibility
        view.findViewById<EditText>(R.id.activityDetails).visibility = visibility
        spinner.visibility = visibility
        url.visibility = visibility
        emojiId.visibility = visibility
        emojiName.visibility = visibility
        emojiAnimated.visibility = visibility
        view.findViewById<Button>(R.id.activityTimestampStart).visibility = visibility
        view.findViewById<Button>(R.id.activityTimestampEnd).visibility = visibility
        view.findViewById<EditText>(R.id.activityPartyState).visibility = visibility
        view.findViewById<EditText>(R.id.activityPartySizeMin).visibility = visibility
        view.findViewById<EditText>(R.id.activityPartySizeMax).visibility = visibility
        view.findViewById<EditText>(R.id.activityLargeImage).visibility = visibility
        view.findViewById<EditText>(R.id.activityLargeText).visibility = visibility
        view.findViewById<EditText>(R.id.activitySmallImage).visibility = visibility
        view.findViewById<EditText>(R.id.activitySmallText).visibility = visibility
        view.findViewById<EditText>(R.id.activityButton1Label).visibility = visibility
        view.findViewById<EditText>(R.id.activityButton2Label).visibility = visibility
        if(view.findViewById<SwitchMaterial>(R.id.showAll).isChecked && visibility == View.VISIBLE) showAllSwitch(
            View.VISIBLE, view) else showAllSwitch(View.GONE, view)
        when (spinner.selectedItemPosition) {
            1 -> {
                url.visibility = View.VISIBLE
                emojiId.visibility = View.GONE
                emojiName.visibility = View.GONE
                emojiAnimated.visibility = View.GONE
            }
            4 -> {
                url.visibility = View.GONE
                emojiId.visibility = View.VISIBLE
                emojiName.visibility = View.VISIBLE
                emojiAnimated.visibility = View.VISIBLE
            }
            else -> {
                url.visibility = View.GONE
                emojiId.visibility = View.GONE
                emojiName.visibility = View.GONE
                emojiAnimated.visibility = View.GONE
            }
        }
        view.findViewById<EditText>(R.id.activityButton1URL).visibility = visibility
        view.findViewById<EditText>(R.id.activityButton2URL).visibility = visibility
    }
}