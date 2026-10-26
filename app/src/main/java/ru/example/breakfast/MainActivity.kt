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
import android.view.View
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
        private const val DEFAULT_RESERVE_MINUTES = 5

        private const val PREFS_NAME = "pool_time_settings"
        private const val KEY_ENTRY_HOUR = "entry_hour"
        private const val KEY_ENTRY_MINUTE = "entry_minute"
        private const val KEY_ENTRY_TIMESTAMP = "entry_timestamp"
        private const val KEY_CHANGING = "changing_minutes"
        private const val KEY_RESERVE = "reserve_minutes"
        private const val KEY_INITIALIZED = "initialized"
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: android.content.SharedPreferences

    private lateinit var entryTimeButton: Button
    private lateinit var poolExitTimeText: TextView
    private lateinit var deadlineText: TextView
    private lateinit var remainingText: TextView
    private lateinit var poolExitPanel: LinearLayout
    private lateinit var deadlinePanel: LinearLayout
    private lateinit var settingsSummary: TextView

    private var entryHour = 0
    private var entryMinute = 0
    private var entryTimestampMillis = 0L
    private var changingMinutes = DEFAULT_CHANGING_MINUTES
    private var reserveMinutes = DEFAULT_RESERVE_MINUTES

    private val updateTask = object : Runnable {
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
        handler.removeCallbacks(updateTask)
        handler.post(updateTask)
    }

    override fun onPause() {
        handler.removeCallbacks(updateTask)
        super.onPause()
    }

    private fun loadSettings() {
        val now = Calendar.getInstance()
        val initialized = prefs.getBoolean(KEY_INITIALIZED, false)

        entryHour = prefs.getInt(KEY_ENTRY_HOUR, now.get(Calendar.HOUR_OF_DAY))
        entryMinute = prefs.getInt(KEY_ENTRY_MINUTE, now.get(Calendar.MINUTE))
        changingMinutes = prefs.getInt(KEY_CHANGING, DEFAULT_CHANGING_MINUTES)
        reserveMinutes = prefs.getInt(KEY_RESERVE, DEFAULT_RESERVE_MINUTES)

        entryTimestampMillis = prefs.getLong(KEY_ENTRY_TIMESTAMP, 0L)

        if (!initialized || entryTimestampMillis == 0L) {
            // Для старых установок восстанавливаем сегодняшнюю дату
            // из сохранённого времени. Будущее время остаётся будущим.
            entryTimestampMillis = calendarForToday(entryHour, entryMinute).timeInMillis
            saveSettings()
        }
    }

    private fun saveSettings() {
        prefs.edit()
            .putInt(KEY_ENTRY_HOUR, entryHour)
            .putInt(KEY_ENTRY_MINUTE, entryMinute)
            .putLong(KEY_ENTRY_TIMESTAMP, entryTimestampMillis)
            .putInt(KEY_CHANGING, changingMinutes)
            .putInt(KEY_RESERVE, reserveMinutes)
            .putBoolean(KEY_INITIALIZED, true)
            .apply()
    }

    private fun calendarForToday(hour: Int, minute: Int): Calendar =
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
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(24))
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
        header.addView(titleBlock, LinearLayout.LayoutParams(0, dp(64), 1f))

        val settingsButton = Button(this).apply {
            text = "⚙"
            textSize = 24f
            setTextColor(Color.WHITE)
            background = roundedBackground(Color.rgb(45, 57, 76), 14)
            setOnClickListener { showSettingsDialog() }
        }
        header.addView(settingsButton, LinearLayout.LayoutParams(dp(58), dp(52)))
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
        addLabel(poolExitPanel, "ПОРА ВЫХОДИТЬ ИЗ БАССЕЙНА")

        poolExitTimeText = TextView(this).apply {
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        poolExitPanel.addView(poolExitTimeText, matchWrap().apply { topMargin = dp(8) })

        remainingText = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        poolExitPanel.addView(remainingText, matchWrap().apply { topMargin = dp(8) })

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
        deadlinePanel.addView(deadlineText, matchWrap().apply { topMargin = dp(8) })

        val deadlineHint = TextView(this).apply {
            text = "Последний срок выхода через турникет"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        deadlinePanel.addView(deadlineHint, matchWrap().apply { topMargin = dp(8) })

        root.addView(
            deadlinePanel,
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

    private fun matchWrap(): .LayoutParams =
        .LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

    private fun createPanel(color: Int):  =
        (this).apply {
            orientation = .VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(22), dp(16), dp(22))
            background = roundedBackground(color, 22)
        }

    private fun addLabel(parent: LinearLayout, value: String) {
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
                entryTimestampMillis = calendarForToday(hour, minute).timeInMillis
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
        val reserveInput = createNumberInput(reserveMinutes.toString())

        addLabel(container, "Время на переодевание, минут")
        container.addView(changingInput, fixedHeight(dp(54)))
        addLabel(container, "Неприкосновенный запас, минут")
        container.addView(reserveInput, fixedHeight(dp(54)))

        val dialog = AlertDialog.Builder(this)
            .setTitle("Настройки")
            .setView(container)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val changingValue = changingInput.text.toString().toIntOrNull()
                val reserveValue = reserveInput.text.toString().toIntOrNull()

                if (changingValue == null || reserveValue == null ||
                    changingValue < 0 || reserveValue < 0
                ) {
                    Toast.makeText(
                        this,
                        "Введи целое число не меньше нуля",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                if (changingValue + reserveValue > TOTAL_MINUTES) {
                    Toast.makeText(
                        this,
                        "Сумма настроек не должна превышать 75 минут",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                changingMinutes = changingValue
                reserveMinutes = reserveValue
                saveSettings()
                updateScreen()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun fixedHeight(height: Int) =
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
        val entryTotal = entryHour * 60 + entryMinute
        val elapsedMillis = nowMillis - entryTimestampMillis
        val waitingForEntry = elapsedMillis < 0L

        // Если выбранное время ещё впереди, не считаем его вчерашним.
        // Время ожидания округляется вверх, чтобы отображать 1 минуту,
        // пока до входа остаётся меньше минуты.
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

        val poolExitOffset = TOTAL_MINUTES - changingMinutes - reserveMinutes
        val poolExitTime = (entryTotal + poolExitOffset) % 1440
        val deadlineTime = (entryTotal + TOTAL_MINUTES) % 1440

        val poolMinutesLeft = poolExitOffset - elapsedMinutes
        val deadlineMinutesLeft = TOTAL_MINUTES - elapsedMinutes

        entryTimeButton.text = formatTime(entryTotal)
        poolExitTimeText.text = formatTime(poolExitTime)
        deadlineText.text = formatTime(deadlineTime)

        remainingText.text = when {
            waitingForEntry -> "До входа: ${formatDuration(waitMinutes)}"
            poolMinutesLeft > 0 ->
                "До выхода из воды: ${formatDuration(poolMinutesLeft)}"
            poolMinutesLeft == 0 -> "ВЫХОДИТЬ ИЗ ВОДЫ СЕЙЧАС!"
            else -> "ВРЕМЯ ВЫХОДИТЬ ИЗ ВОДЫ УЖЕ ПРОШЛО"
        }

        val poolColor = when {
            waitingForEntry -> Color.rgb(35, 105, 72)
            poolMinutesLeft > 10 -> Color.rgb(35, 105, 72)
            poolMinutesLeft > 5 -> Color.rgb(190, 135, 20)
            else -> Color.rgb(190, 35, 42)
        }
        poolExitPanel.background = roundedBackground(poolColor, 22)

        val deadlineColor = when {
            waitingForEntry -> Color.rgb(125, 35, 42)
            deadlineMinutesLeft > 10 -> Color.rgb(125, 35, 42)
            deadlineMinutesLeft > 5 -> Color.rgb(190, 35, 42)
            else -> Color.rgb(110, 0, 0)
        }
        deadlinePanel.background = roundedBackground(deadlineColor, 22)

        settingsSummary.text =
            "Переодевание: $changingMinutes мин.  •  Запас: $reserveMinutes мин."
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

    private fun roundedBackground(color: Int, radiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
