package com.example.lab3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lab3.data.Task
import com.example.lab3.data.UserRole
import com.example.lab3.manager.AuthManager
import com.example.lab3.manager.TaskManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator

class AdminReportActivity : AppCompatActivity() {
    private lateinit var statusReportTextView: TextView
    private lateinit var completionRateTextView: TextView
    private lateinit var statusNumbersTextView: TextView
    private lateinit var reportCompletionProgressBar: LinearProgressIndicator
    private lateinit var priorityReportTextView: TextView
    private lateinit var deadlineReportTextView: TextView
    private lateinit var backButton: MaterialButton
    private lateinit var adminBottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: створення екрана звітності")
        setContentView(R.layout.activity_admin_report)

        AuthManager.init(this)
        TaskManager.init(this)
        if (AuthManager.currentRole() != UserRole.ADMIN) {
            Toast.makeText(this, "Цей екран доступний тільки адміністратору", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()
        setupNavigation()
        showReport()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: екран звітності видимий")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: оновлення звіту")
        if (::statusReportTextView.isInitialized) {
            showReport()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: звітність призупинена")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: екран звітності не видимий")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: екран звітності знищено")
    }

    private fun bindViews() {
        statusReportTextView = findViewById(R.id.statusReportTextView)
        completionRateTextView = findViewById(R.id.completionRateTextView)
        statusNumbersTextView = findViewById(R.id.statusNumbersTextView)
        reportCompletionProgressBar = findViewById(R.id.reportCompletionProgressBar)
        priorityReportTextView = findViewById(R.id.priorityReportTextView)
        deadlineReportTextView = findViewById(R.id.deadlineReportTextView)
        backButton = findViewById(R.id.backButton)
        adminBottomNavigation = findViewById(R.id.adminBottomNavigation)
    }

    private fun setupNavigation() {
        backButton.setOnClickListener { finish() }
        adminBottomNavigation.selectedItemId = R.id.nav_report
        adminBottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tasks -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_users -> {
                    startActivity(Intent(this, AdminUsersActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_report -> true
                else -> false
            }
        }
    }

    private fun showReport() {
        val tasks = TaskManager.getAllTasks()
        val stats = TaskManager.calculateStats(tasks)

        completionRateTextView.text = "Виконання: ${stats.completionPercent}%"
        reportCompletionProgressBar.progress = stats.completionPercent
        statusNumbersTextView.text = "Усього: ${stats.total}     Активні: ${stats.active}\nВиконані: ${stats.completed}     Прострочені: ${stats.overdue}"

        priorityReportTextView.text = """
            Пріоритети
            
            Низький: ${stats.lowPriority}     Середній: ${stats.mediumPriority}     Високий: ${stats.highPriority}
        """.trimIndent()

        deadlineReportTextView.text = """
            Найближчі дедлайни
            ${formatTaskList(TaskManager.nearestDeadlines(tasks))}
            
            Прострочені задачі
            ${formatTaskList(tasks.filter { TaskManager.isOverdue(it) })}
        """.trimIndent()
    }

    private fun formatTaskList(tasks: List<Task>): String {
        if (tasks.isEmpty()) {
            return "Поки немає задач"
        }
        return tasks.joinToString(separator = "\n") { task ->
            "${TaskManager.formatDate(task.deadlineMillis)} - ${task.title} (${task.ownerEmail})"
        }
    }

    companion object {
        private const val TAG = "AdminReportActivity"
    }
}
