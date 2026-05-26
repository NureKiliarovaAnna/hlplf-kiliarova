package com.example.lab3

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.lab3.data.Task
import com.example.lab3.manager.AuthManager
import com.example.lab3.manager.TaskManager
import com.google.android.material.button.MaterialButton

class TaskDetailsActivity : AppCompatActivity() {
    private lateinit var titleTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var priorityTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var deadlineTextView: TextView
    private lateinit var createdTextView: TextView
    private lateinit var ownerTextView: TextView
    private lateinit var reminderTextView: TextView
    private lateinit var backButton: MaterialButton
    private lateinit var reminderButton: MaterialButton

    private var task: Task? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: створення екрана деталей задачі")
        setContentView(R.layout.activity_task_details)

        AuthManager.init(this)
        TaskManager.init(this)
        bindViews()

        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        task = TaskManager.getTaskById(taskId)

        if (task == null) {
            Toast.makeText(this, "Задачу не знайдено", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showTaskDetails(task!!)
        backButton.setOnClickListener { finish() }
        reminderButton.setOnClickListener { showEmailReminder(task!!) }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: екран деталей стає видимим")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: користувач переглядає повну інформацію про задачу")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: екран деталей частково перекритий")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: екран деталей більше не видимий")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: екран деталей знищено")
    }

    private fun bindViews() {
        titleTextView = findViewById(R.id.detailsTitleTextView)
        descriptionTextView = findViewById(R.id.detailsDescriptionTextView)
        priorityTextView = findViewById(R.id.detailsPriorityTextView)
        statusTextView = findViewById(R.id.detailsStatusTextView)
        deadlineTextView = findViewById(R.id.detailsDeadlineTextView)
        createdTextView = findViewById(R.id.detailsCreatedTextView)
        ownerTextView = findViewById(R.id.detailsOwnerTextView)
        reminderTextView = findViewById(R.id.detailsReminderTextView)
        backButton = findViewById(R.id.backButton)
        reminderButton = findViewById(R.id.reminderButton)
    }

    private fun showTaskDetails(task: Task) {
        titleTextView.text = task.title
        descriptionTextView.text = task.description.ifBlank { "Опис не додано" }
        priorityTextView.text = "Пріоритет: ${task.priority.displayName}"
        statusTextView.text = "Статус: ${statusText(task)}"
        deadlineTextView.text = "Дедлайн: ${TaskManager.formatDate(task.deadlineMillis)}"
        createdTextView.text = "Дата створення: ${TaskManager.formatDateTime(task.createdAtMillis)}"
        ownerTextView.text = "Власник задачі: ${task.ownerEmail}"
        reminderTextView.text = reminderText(task)
        reminderTextView.background = reminderBackground(task)
    }

    private fun statusText(task: Task): String {
        return when {
            task.isCompleted -> "виконано"
            TaskManager.isOverdue(task) -> "прострочено"
            else -> "активна"
        }
    }

    private fun reminderText(task: Task): String {
        return when {
            task.isCompleted -> "Нагадування не потрібне: задачу вже виконано."
            TaskManager.isOverdue(task) -> "Локальне повідомлення: дедлайн уже минув."
            else -> "Локальне повідомлення: дедлайн ${TaskManager.formatDate(task.deadlineMillis)}."
        }
    }

    private fun reminderBackground(task: Task): GradientDrawable {
        val colorRes = when {
            task.isCompleted -> R.color.success
            TaskManager.isOverdue(task) -> R.color.danger
            else -> R.color.warning
        }
        return GradientDrawable().apply {
            cornerRadius = 12f
            setColor(ContextCompat.getColor(this@TaskDetailsActivity, colorRes))
            alpha = 35
        }
    }

    private fun showEmailReminder(task: Task) {
        Toast.makeText(
            this,
            "Email-нагадування імітовано: ${task.ownerEmail}, задача «${task.title}»",
            Toast.LENGTH_LONG
        ).show()
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        private const val TAG = "TaskDetailsActivity"
    }
}
