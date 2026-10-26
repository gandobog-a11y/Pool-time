
package ru.example.pool_time

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity() {

    companion object {
        private const val TOTAL_MINUTES = 75

        private const val DEFAULT_CHANGING_MINUTES = 10
        private const val DEFAULT_RED_MINUTES = 5
        private const val DEFAULT_YELLOW_MINUTES = 5

        private const val PREFS_NAME = "pool_time_settings"
        private const val KEY_ENTRY_HOUR = "entry_hour"
        private const val KEY_ENTRY_MINUTE = "entry_minute"
        private const val KEY_ENTRY_TIMESTAMP = "entry_timestamp"
        private const val KEY_CHANGING = "changing_minutes"
        private const val KEY_RED = "red_minutes"
        private const val KEY_YELLOW = "yellow_minutes"
        private const val KEY_INITIALIZED = "initialized"
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: android.content.SharedPreferences

    private lateinit var entryTimeButton: Button
    private lateinit var poolExitTimeText: TextView
    private lateinit var deadlineText: TextView
    private lateinit var currentTimeText: TextView
    private lateinit var remainingText: TextView
    private lateinit var poolExitPanel: LinearLayout
    private lateinit var deadlinePanel: LinearLayout
    private lateinit var currentTimePanel: LinearLayout
    private lateinit var settingsSummary: TextView

    private var entryHour = 0
    private var entryMinute = 0
    private var entryTimestampMillis = 0L

    private var changingMinutes = DEFAULT_CHANGING_MINUTES
    private var redMinutes = DEFAULT_RED_MINUTES
    private var yellowMinutes = DEFAULT_YELLOW_MINUTES

    private val handlerTask = object : Runnable {
        override fun run() {
            updateScreen()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(18, 25, 38)
        window.navigationBarColor = Color.rgb(18, 25, 38)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        loadSettings()
        buildScreen()
        updateScreen()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(handlerTask)
        handler.post(handlerTask)
    }

    override fun onPause() {
        handler.removeCallbacks(handlerTask)
        super.onPause()
    }

    private fun loadSettings() {
        val now = Calendar.getInstance()
        val initialized = prefs.getBoolean(KEY_INITIALIZED, false)

        entryHour = prefs.getInt(
            KEY_ENTRY_HOUR,
            now.get(Calendar.HOUR_OF_DAY)
        )

        entryMinute = prefs.getInt(
            KEY_ENTRY_MINUTE,
            now.get(Calendar.MINUTE)
        )

        changingMinutes = prefs.getInt(
            KEY_CHANGING,
            DEFAULT_CHANGING_MINUTES
        )

        redMinutes = prefs.getInt(
            KEY_RED,
            DEFAULT_RED_MINUTES
        )

        yellowMinutes = prefs.getInt(
            KEY_YELLOW,
            DEFAULT_YELLOW_MINUTES
        )

        entryTimestampMillis = prefs.getLong(
            KEY_ENTRY_TIMESTAMP,
            0L
        )

        if (!initialized || entryTimestampMillis == 0L) {
            entryTimestampMillis =
                calendarForToday(entryHour, entryMinute).timeInMillis
            saveSettings()
        }
    }

    private fun saveSettings() {
        prefs.edit()
            .putInt(KEY_ENTRY_HOUR, entryHour)
            .putInt(KEY_ENTRY_MINUTE, entryMinute)
            .putLong(KEY_ENTRY_TIMESTAMP, entryTimestampMillis)
            .putInt(KEY_CHANGING, changingMinutes)
            .putInt(KEY_RED, redMinutes)
            .putInt(KEY_YELLOW, yellowMinutes)
            .putBoolean(KEY_INITIALIZED, true)
            .apply()
    }

    private fun calendarForToday(
        hour: Int,
        minute: Int
    ): Calendar =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    private fun buildScreen() {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(18, 25, 38))
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(48), dp(18), dp(24))
        }

        scrollView.addView(
            root,
            android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "МОКРОЕ ДЕЛО"
            textSize = 25f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
        }

        val subtitle = TextView(this).apply {
            text = "75 минут до капитуляции"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER_VERTICAL
        }

        titleBlock.addView(title)
        titleBlock.addView(subtitle)

        header.addView(
            titleBlock,
            LinearLayout.LayoutParams(0, dp(64), 1f)
        )

        val settingsButton = Button(this).apply {
            text = "⚙"
            textSize = 24f
            setTextColor(Color.WHITE)
            background = roundedBackground(Color.rgb(45, 57, 76), 14)
            setOnClickListener { showSettingsDialog() }
        }

        header.addView(
            settingsButton,
            LinearLayout.LayoutParams(dp(58), dp(52))
        )

        root.addView(header, matchWrap())

        addLabel(root, "Вход через турникет")

        entryTimeButton = Button(this).apply {
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = roundedBackground(Color.rgb(45, 57, 76), 18)
            setOnClickListener { showEntryTimePicker() }
        }

        root.addView(
            entryTimeButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(76)
            ).apply {
                topMargin = dp(4)
                bottomMargin = dp(18)
            }
        )

        poolExitPanel = createPanel(Color.rgb(35, 105, 72))

        addLabel(poolExitPanel, "ОСТАЛОСЬ ВРЕМЕНИ")

        poolExitTimeText = TextView(this).apply {
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        poolExitPanel.addView(
            poolExitTimeText,
            matchWrap().apply { topMargin = dp(8) }
        )

        remainingText = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        poolExitPanel.addView(
            remainingText,
            matchWrap().apply { topMargin = dp(8) }
        )

        root.addView(
            poolExitPanel,
            matchWrap().apply { bottomMargin = dp(18) }
        )

        deadlinePanel = createPanel(Color.rgb(125, 35, 42))

        val deadlineTitle = TextView(this).apply {
            text = "ВРЕМЯ ПИЗДЕЦА"
            textSize = 29f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        deadlinePanel.addView(deadlineTitle, matchWrap())

        deadlineText = TextView(this).apply {
            textSize = 52f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        deadlinePanel.addView(
            deadlineText,
            matchWrap().apply { topMargin = dp(8) }
        )

        val deadlineHint = TextView(this).apply {
            text = "Последний срок выхода через турникет"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        deadlinePanel.addView(
            deadlineHint,
            matchWrap().apply { topMargin = dp(8) }
        )

        root.addView(
            deadlinePanel,
            matchWrap().apply { bottomMargin = dp(8) }
        )

        currentTimePanel = createPanel(Color.BLACK)

        currentTimeText = TextView(this).apply {
            textSize = 52f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        currentTimePanel.addView(currentTimeText, matchWrap())

        root.addView(
            currentTimePanel,
            matchWrap().apply { bottomMargin = dp(16) }
        )

        settingsSummary = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        root.addView(settingsSummary, matchWrap())

        setContentView(scrollView)
    }

    private fun matchWrap(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

    private fun createPanel(color: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(22), dp(16), dp(22))
            background = roundedBackground(color, 22)
        }

    private fun addLabel(
        parent: LinearLayout,
        value: String
    ) {
        val label = TextView(this).apply {
            text = value
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        parent.addView(
            label,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
                bottomMargin = dp(4)
            }
        )
    }

    private fun showEntryTimePicker() {
        TimePickerDialog(
            this,
            { _, hour, minute ->
                entryHour = hour
                entryMinute = minute
                entryTimestampMillis =
                    calendarForToday(hour, minute).timeInMillis
                saveSettings()
                updateScreen()
            },
            entryHour,
            entryMinute,
            true
        ).show()
    }

    private fun showSettingsDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
        }

        val changingInput = createNumberInput(changingMinutes.toString())
        val yellowInput = createNumberInput(yellowMinutes.toString())
        val redInput = createNumberInput(redMinutes.toString())

        addLabel(container, "Время на переодевание, минут")
        container.addView(changingInput, fixedHeight(dp(54)))

        addLabel(container, "Жёлтый уровень, минут")
        container.addView(yellowInput, fixedHeight(dp(54)))

        addLabel(container, "Красный уровень, минут")
        container.addView(redInput, fixedHeight(dp(54)))

        val dialog = AlertDialog.Builder(this)
            .setTitle("Настройки")
            .setView(container)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val changingValue =
                        changingInput.text.toString().toIntOrNull()
                    val yellowValue =
                        yellowInput.text.toString().toIntOrNull()
                    val redValue =
                        redInput.text.toString().toIntOrNull()

                    if (
                        changingValue == null ||
                        yellowValue == null ||
                        redValue == null ||
                        changingValue < 0 ||
                        yellowValue < 0 ||
                        redValue < 0
                    ) {
                        Toast.makeText(
                            this,
                            "Введи целые числа не меньше нуля",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    if (yellowValue + redValue > TOTAL_MINUTES) {
                        Toast.makeText(
                            this,
                            "Сумма жёлтого и красного уровней не должна превышать 75 минут",
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }

                    changingMinutes = changingValue
                    yellowMinutes = yellowValue
                    redMinutes = redValue

                    saveSettings()
                    updateScreen()
                    dialog.dismiss()
                }
        }

        dialog.show()
    }

    private fun fixedHeight(height: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            height
        )

    private fun createNumberInput(value: String): EditText =
        EditText(this).apply {
            setText(value)
            textSize = 20f
            setTextColor(Color.BLACK)
            inputType = InputType.TYPE_CLASS_NUMBER
            gravity = Gravity.CENTER
            selectAll()
        }

    private fun updateScreen() {
        val nowMillis = System.currentTimeMillis()
        val elapsedMillis = nowMillis - entryTimestampMillis
        val waitingForEntry = elapsedMillis < 0L

        val elapsedMinutes = if (waitingForEntry) {
            0
        } else {
            (elapsedMillis / 60_000L).toInt()
        }

        val waitMinutes = if (waitingForEntry) {
            ((-elapsedMillis + 59_999L) / 60_000L).toInt()
        } else {
            0
        }

        val deadlineTimestamp =
            entryTimestampMillis + TOTAL_MINUTES * 60_000L

        val remainingMillis =
            (deadlineTimestamp - nowMillis).coerceAtLeast(0L)

        val remainingSeconds =
            (remainingMillis + 999L) / 1000L

        val redStartMinute = TOTAL_MINUTES - redMinutes
        val yellowStartMinute = redStartMinute - yellowMinutes

        entryTimeButton.text = formatTime(entryHour * 60 + entryMinute)

        poolExitTimeText.text =
            if (waitingForEntry) {
                formatMinutesSeconds(TOTAL_MINUTES * 60L)
            } else {
                formatMinutesSeconds(remainingSeconds)
            }

        val deadlineCalendar = Calendar.getInstance().apply {
            timeInMillis = entryTimestampMillis
            add(Calendar.MINUTE, TOTAL_MINUTES)
        }

        deadlineText.text = formatTime(
            deadlineCalendar.get(Calendar.HOUR_OF_DAY) * 60 +
                deadlineCalendar.get(Calendar.MINUTE)
        )

        currentTimeText.text = formatTime(
            Calendar.getInstance().let {
                it.get(Calendar.HOUR_OF_DAY) * 60 +
                    it.get(Calendar.MINUTE)
            }
        )

        remainingText.text = when {
            waitingForEntry ->
                "До входа: ${formatDuration(waitMinutes)}"

            elapsedMillis < changingMinutes / 2 * 60_000L ->
                "Переодевание перед бассейном"

            elapsedMinutes < redStartMinute - changingMinutes ->
                "Время плавать!"

            elapsedMinutes < TOTAL_MINUTES - redMinutes ->
                "Скоро пора выходить"

            elapsedMillis < TOTAL_MINUTES * 60_000L ->
                "РЕЗЕРВ! ПОРА К ТУРНИКЕТУ"

            else ->
                "ВРЕМЯ ВЫХОДИТЬ УЖЕ ПРОШЛО"
        }

        val poolColor = when {
            waitingForEntry ->
                Color.rgb(35, 105, 72)

            elapsedMillis >= TOTAL_MINUTES * 60_000L ->
                Color.rgb(121, 85, 72)

            elapsedMinutes >= redStartMinute ->
                Color.rgb(190, 35, 42)

            elapsedMinutes >= yellowStartMinute ->
                Color.rgb(190, 160, 20)

            else ->
                Color.rgb(35, 105, 72)
        }

        poolExitPanel.background =
            roundedBackground(poolColor, 22)

        deadlinePanel.background =
            roundedBackground(Color.rgb(125, 35, 42), 22)

        currentTimePanel.background =
            roundedBackground(Color.BLACK, 22)

        settingsSummary.text =
            "Переодевание: $changingMinutes мин.  •  " +
                "Жёлтый: $yellowMinutes мин.  •  " +
                "Красный: $redMinutes мин."
    }

    private fun formatMinutesSeconds(totalSeconds: Long): String {
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L

        return String.format(
            Locale.getDefault(),
            "%02d:%02d",
            minutes,
            seconds
        )
    }

    private fun formatTime(totalMinutes: Int): String {
        val normalized = (totalMinutes % 1440 + 1440) % 1440

        return String.format(
            Locale.getDefault(),
            "%02d:%02d",
            normalized / 60,
            normalized % 60
        )
    }

    private fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val remaining = minutes % 60

        return if (hours > 0) {
            "${hours} ч ${remaining} мин"
        } else {
            "$remaining мин"
        }
    }

    private fun roundedBackground(
        color: Int,
        radiusDp: Int
    ): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
