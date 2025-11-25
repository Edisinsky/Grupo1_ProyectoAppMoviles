package com.grupo.uno.proyecto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.grupo.uno.proyecto.data.AppDatabase
import com.grupo.uno.proyecto.data.Task
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: TaskAdapter
    private val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configurar RecyclerView
        val rvTasks = findViewById<RecyclerView>(R.id.rvTasks)
        rvTasks.layoutManager = LinearLayoutManager(this)

        adapter = TaskAdapter(
            emptyList(),
            onDeleteClick = { task -> showDeleteConfirmation(task) },
            onCheckClick = { task -> toggleTaskCompletion(task) }
        )
        rvTasks.adapter = adapter

        // Botón agregar tarea
        findViewById<FloatingActionButton>(R.id.fabAddTask).setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }

        // Botón Logout
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Observar datos de Room
        lifecycleScope.launch {
            database.taskDao().getAllTasks().collect { tasks ->
                adapter.updateTasks(tasks)
            }
        }
    }

    private fun toggleTaskCompletion(task: Task) {
        lifecycleScope.launch {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            database.taskDao().update(updatedTask)
        }
    }

    private fun showDeleteConfirmation(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Tarea")
            .setMessage("¿Estás seguro de eliminar '${task.title}'?")
            .setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch { database.taskDao().delete(task) }
            }
            .setNegativeButton("No", null)
            .show()
    }
}