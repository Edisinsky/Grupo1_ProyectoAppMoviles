package com.grupo.uno.proyecto

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.grupo.uno.proyecto.data.AppDatabase
import com.grupo.uno.proyecto.data.Task
import com.grupo.uno.proyecto.utils.NotificationReceiver
import kotlinx.coroutines.launch
import java.util.Calendar

class AddTaskActivity: AppCompatActivity() {
    private var selectedTimestamp: Long = 0
    private val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        val etTitle = findViewById<TextInputEditText>(R.id.etTitle)
        val etDesc = findViewById<TextInputEditText>(R.id.etDesc)
        val tvDate = findViewById<TextView>(R.id.tvSelectedDate)

        findViewById<Button>(R.id.btnDate).setOnClickListener {
            showDateTimePicker(tvDate)
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val title = etTitle.text.toString()
            val desc = etDesc.text.toString()

            if (title.isNotEmpty() && selectedTimestamp > 0) {
                lifecycleScope.launch {
                    val task = Task(title = title, description = desc, dueDate = selectedTimestamp)
                    val id = database.taskDao().insert(task)

                    scheduleNotification(title, desc, selectedTimestamp, id.toInt())

                    finish() // Volver a main
                }
            } else {
                Toast.makeText(this, "Completa título y fecha", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDateTimePicker(tvDisplay: TextView) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                TimePickerDialog(this, { _, hour, minute ->
                    calendar.set(year, month, day, hour, minute)
                    selectedTimestamp = calendar.timeInMillis
                    tvDisplay.text = "$day/${month + 1}/$year $hour:$minute"
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun scheduleNotification(title: String, message: String, time: Long, id: Int) {
        // En Android 13+ deberías pedir permiso POST_NOTIFICATIONS aquí,
        // pero para este prototipo asumimos permiso concedido o versiones menores.

        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
        } catch (_: SecurityException) {
            Toast.makeText(this, "Permiso de alarma no concedido", Toast.LENGTH_SHORT).show()
        }
    }
}