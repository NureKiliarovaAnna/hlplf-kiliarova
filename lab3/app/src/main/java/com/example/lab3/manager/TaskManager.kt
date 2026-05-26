package com.example.lab3.manager

import android.content.Context
import android.content.SharedPreferences
import com.example.lab3.data.Task
import com.example.lab3.data.TaskPriority
import com.example.lab3.data.TaskStats
import com.example.lab3.data.UserRole
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TaskManager {
    private const val PREFS_NAME = "task_prefs"
    private const val KEY_TASKS = "tasks"

    private lateinit var preferences: SharedPreferences
    private val tasks = mutableListOf<Task>()
    private var initialized = false

    fun init(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!initialized) {
            loadTasks()
            initialized = true
        }
    }

    fun addTask(task: Task) {
        tasks.add(0, task)
        saveTasks()
    }

    fun updateTask(updatedTask: Task) {
        val index = tasks.indexOfFirst { it.id == updatedTask.id }
        if (index != -1) {
            tasks[index] = updatedTask
            saveTasks()
        }
    }

    fun deleteTask(taskId: Long) {
        tasks.removeAll { it.id == taskId }
        saveTasks()
    }

    fun toggleCompleted(taskId: Long) {
        val task = tasks.firstOrNull { it.id == taskId } ?: return
        task.isCompleted = !task.isCompleted
        saveTasks()
    }

    fun getTaskById(taskId: Long): Task? {
        return tasks.firstOrNull { it.id == taskId }
    }

    fun getAllTasks(): List<Task> {
        return tasks.sortedBy { it.deadlineMillis }
    }

    fun getVisibleTasks(role: UserRole, email: String): List<Task> {
        return if (role == UserRole.ADMIN) {
            tasks.sortedBy { it.deadlineMillis }
        } else {
            tasks.filter { it.ownerEmail == email }.sortedBy { it.deadlineMillis }
        }
    }

    fun canModify(task: Task, role: UserRole, email: String): Boolean {
        return role == UserRole.ADMIN || task.ownerEmail == email
    }

    fun calculateStats(visibleTasks: List<Task>): TaskStats {
        val total = visibleTasks.size
        val completed = visibleTasks.count { it.isCompleted }
        val overdue = visibleTasks.count { isOverdue(it) }
        val active = visibleTasks.count { !it.isCompleted && !isOverdue(it) }
        val completionPercent = if (total == 0) 0 else (completed * 100) / total

        return TaskStats(
            total = total,
            completed = completed,
            overdue = overdue,
            active = active,
            lowPriority = visibleTasks.count { it.priority == TaskPriority.LOW },
            mediumPriority = visibleTasks.count { it.priority == TaskPriority.MEDIUM },
            highPriority = visibleTasks.count { it.priority == TaskPriority.HIGH },
            completionPercent = completionPercent
        )
    }

    fun isOverdue(task: Task): Boolean {
        return !task.isCompleted && task.deadlineMillis < startOfTodayMillis()
    }

    fun nearestDeadlines(visibleTasks: List<Task>): List<Task> {
        return visibleTasks
            .filter { !it.isCompleted && it.deadlineMillis >= startOfTodayMillis() }
            .sortedBy { it.deadlineMillis }
            .take(3)
    }

    fun tasksForReminder(visibleTasks: List<Task>): List<Task> {
        val now = startOfTodayMillis()
        val threeDays = now + 3L * 24L * 60L * 60L * 1000L
        return visibleTasks
            .filter { !it.isCompleted && it.deadlineMillis in now..threeDays }
            .sortedBy { it.deadlineMillis }
    }

    fun formatDate(timeMillis: Long): String {
        return SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("uk-UA")).format(Date(timeMillis))
    }

    fun formatDateTime(timeMillis: Long): String {
        return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("uk-UA")).format(Date(timeMillis))
    }

    fun startOfTodayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun loadTasks() {
        tasks.clear()
        val rawJson = preferences.getString(KEY_TASKS, "[]") ?: "[]"
        val jsonArray = JSONArray(rawJson)
        for (index in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(index)
            tasks.add(
                Task(
                    id = item.getLong("id"),
                    title = item.getString("title"),
                    description = item.getString("description"),
                    deadlineMillis = item.getLong("deadlineMillis"),
                    priority = TaskPriority.valueOf(item.getString("priority")),
                    isCompleted = item.getBoolean("isCompleted"),
                    createdAtMillis = item.getLong("createdAtMillis"),
                    ownerEmail = item.getString("ownerEmail")
                )
            )
        }
    }

    private fun saveTasks() {
        val jsonArray = JSONArray()
        tasks.forEach { task ->
            val item = JSONObject()
                .put("id", task.id)
                .put("title", task.title)
                .put("description", task.description)
                .put("deadlineMillis", task.deadlineMillis)
                .put("priority", task.priority.name)
                .put("isCompleted", task.isCompleted)
                .put("createdAtMillis", task.createdAtMillis)
                .put("ownerEmail", task.ownerEmail)
            jsonArray.put(item)
        }
        preferences.edit().putString(KEY_TASKS, jsonArray.toString()).apply()
    }
}
