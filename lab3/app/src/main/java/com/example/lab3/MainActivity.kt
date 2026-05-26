package com.example.lab3

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab3.adapter.TaskAdapter
import com.example.lab3.data.Task
import com.example.lab3.data.TaskPriority
import com.example.lab3.data.UserRole
import com.example.lab3.manager.AuthManager
import com.example.lab3.manager.TaskManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var screenTitleTextView: TextView
    private lateinit var userInfoTextView: TextView
    private lateinit var statisticsTitle: TextView
    private lateinit var statTotalTextView: TextView
    private lateinit var statDoneTextView: TextView
    private lateinit var statActiveTextView: TextView
    private lateinit var statOverdueTextView: TextView
    private lateinit var statPercentTextView: TextView
    private lateinit var completionProgressBar: LinearProgressIndicator
    private lateinit var formTitleTextView: TextView
    private lateinit var listTitleTextView: TextView
    private lateinit var emptyTextView: TextView
    private lateinit var emptyCard: View
    private lateinit var titleEditText: TextInputEditText
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var deadlineEditText: TextInputEditText
    private lateinit var prioritySpinner: Spinner
    private lateinit var saveTaskButton: MaterialButton
    private lateinit var clearFormButton: MaterialButton
    private lateinit var logoutButton: MaterialButton
    private lateinit var mainScrollView: NestedScrollView
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var adminBottomNavigation: BottomNavigationView
    private lateinit var taskAdapter: TaskAdapter

    private var selectedDeadlineMillis: Long = TaskManager.startOfTodayMillis()
    private var editingTaskId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: створення Activity та початкове налаштування UI")
        setContentView(R.layout.activity_main)

        AuthManager.init(this)
        TaskManager.init(this)

        if (!AuthManager.isLoggedIn()) {
            openLoginScreen()
            return
        }

        bindViews()
        setupRoleUi()
        setupPrioritySpinner()
        setupRecyclerView()
        setupListeners()
        resetForm()
        refreshScreen()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: екран стає видимим")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: користувач може взаємодіяти з екраном")
        if (::taskAdapter.isInitialized) {
            refreshScreen()
            showDeadlineReminderIfNeeded()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: Activity частково перекрита або переходить у фон")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: екран більше не видимий")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Activity знищується системою або користувачем")
    }

    private fun bindViews() {
        screenTitleTextView = findViewById(R.id.screenTitleTextView)
        userInfoTextView = findViewById(R.id.userInfoTextView)
        statisticsTitle = findViewById(R.id.statisticsTitle)
        statTotalTextView = findViewById(R.id.statTotalTextView)
        statDoneTextView = findViewById(R.id.statDoneTextView)
        statActiveTextView = findViewById(R.id.statActiveTextView)
        statOverdueTextView = findViewById(R.id.statOverdueTextView)
        statPercentTextView = findViewById(R.id.statPercentTextView)
        completionProgressBar = findViewById(R.id.completionProgressBar)
        formTitleTextView = findViewById(R.id.formTitleTextView)
        listTitleTextView = findViewById(R.id.listTitleTextView)
        emptyTextView = findViewById(R.id.emptyTextView)
        emptyCard = findViewById(R.id.emptyCard)
        titleEditText = findViewById(R.id.taskTitleEditText)
        descriptionEditText = findViewById(R.id.taskDescriptionEditText)
        deadlineEditText = findViewById(R.id.deadlineEditText)
        prioritySpinner = findViewById(R.id.prioritySpinner)
        saveTaskButton = findViewById(R.id.saveTaskButton)
        clearFormButton = findViewById(R.id.clearFormButton)
        logoutButton = findViewById(R.id.logoutButton)
        mainScrollView = findViewById(R.id.mainScrollView)
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView)
        adminBottomNavigation = findViewById(R.id.adminBottomNavigation)
    }

    private fun setupRoleUi() {
        val isAdmin = AuthManager.currentRole() == UserRole.ADMIN
        screenTitleTextView.text = if (isAdmin) "Адмін-панель" else "Мої задачі"
        statisticsTitle.text = if (isAdmin) "Загальна статистика" else "Особиста статистика"
        listTitleTextView.text = if (isAdmin) "Усі задачі користувачів" else "Список власних задач"
        adminBottomNavigation.visibility = if (isAdmin) View.VISIBLE else View.GONE
        adminBottomNavigation.selectedItemId = R.id.nav_tasks
    }

    private fun setupPrioritySpinner() {
        val priorities = TaskPriority.entries.map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, priorities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        prioritySpinner.adapter = adapter
        prioritySpinner.setSelection(TaskPriority.entries.indexOf(TaskPriority.MEDIUM))
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            showOwner = AuthManager.currentRole() == UserRole.ADMIN,
            onDetailsClick = { task ->
                val intent = Intent(this, TaskDetailsActivity::class.java)
                intent.putExtra(TaskDetailsActivity.EXTRA_TASK_ID, task.id)
                startActivity(intent)
            },
            onEditClick = { task -> startEditing(task) },
            onToggleClick = { task ->
                if (!TaskManager.canModify(task, AuthManager.currentRole(), AuthManager.currentEmail())) {
                    Toast.makeText(this, "Недостатньо прав для зміни статусу", Toast.LENGTH_SHORT).show()
                    return@TaskAdapter
                }
                TaskManager.toggleCompleted(task.id)
                Toast.makeText(this, "Статус задачі оновлено", Toast.LENGTH_SHORT).show()
                refreshScreen()
            },
            onDeleteClick = { task -> confirmDelete(task) }
        )

        tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        tasksRecyclerView.adapter = taskAdapter
    }

    private fun setupListeners() {
        deadlineEditText.setOnClickListener { showDatePicker() }
        saveTaskButton.setOnClickListener { saveTask() }
        clearFormButton.setOnClickListener { resetForm() }
        logoutButton.setOnClickListener {
            AuthManager.logout()
            openLoginScreen()
        }

        adminBottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tasks -> {
                    mainScrollView.smoothScrollTo(0, 0)
                    true
                }
                R.id.nav_users -> {
                    openAdminUsers()
                    true
                }
                R.id.nav_report -> {
                    openAdminReport()
                    true
                }
                else -> false
            }
        }
    }

    private fun saveTask() {
        val title = titleEditText.text?.toString()?.trim().orEmpty()
        val description = descriptionEditText.text?.toString()?.trim().orEmpty()
        val priority = TaskPriority.fromDisplayName(prioritySpinner.selectedItem.toString())

        if (title.isBlank()) {
            Toast.makeText(this, "Введіть назву задачі", Toast.LENGTH_SHORT).show()
            return
        }

        val taskId = editingTaskId
        if (taskId == null) {
            val task = Task(
                id = System.currentTimeMillis(),
                title = title,
                description = description,
                deadlineMillis = selectedDeadlineMillis,
                priority = priority,
                ownerEmail = AuthManager.currentEmail()
            )
            TaskManager.addTask(task)
            Toast.makeText(this, "Задачу додано. Email-нагадування імітовано.", Toast.LENGTH_LONG).show()
        } else {
            val oldTask = TaskManager.getTaskById(taskId) ?: return
            if (!TaskManager.canModify(oldTask, AuthManager.currentRole(), AuthManager.currentEmail())) {
                Toast.makeText(this, "Недостатньо прав для редагування", Toast.LENGTH_SHORT).show()
                return
            }
            val updatedTask = oldTask.copy(
                title = title,
                description = description,
                deadlineMillis = selectedDeadlineMillis,
                priority = priority
            )
            TaskManager.updateTask(updatedTask)
            Toast.makeText(this, "Задачу оновлено", Toast.LENGTH_SHORT).show()
        }

        resetForm()
        refreshScreen()
    }

    private fun startEditing(task: Task) {
        if (!TaskManager.canModify(task, AuthManager.currentRole(), AuthManager.currentEmail())) {
            Toast.makeText(this, "Недостатньо прав для редагування", Toast.LENGTH_SHORT).show()
            return
        }
        editingTaskId = task.id
        selectedDeadlineMillis = task.deadlineMillis
        formTitleTextView.text = "Редагування задачі"
        saveTaskButton.text = "Зберегти"
        titleEditText.setText(task.title)
        descriptionEditText.setText(task.description)
        deadlineEditText.setText(TaskManager.formatDate(task.deadlineMillis))
        prioritySpinner.setSelection(TaskPriority.entries.indexOf(task.priority))
        mainScrollView.smoothScrollTo(0, 0)
    }

    private fun confirmDelete(task: Task) {
        if (!TaskManager.canModify(task, AuthManager.currentRole(), AuthManager.currentEmail())) {
            Toast.makeText(this, "Недостатньо прав для видалення", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Видалити задачу?")
            .setMessage("Задача «${task.title}» буде видалена без відновлення.")
            .setPositiveButton("Видалити") { _, _ ->
                TaskManager.deleteTask(task.id)
                Toast.makeText(this, "Задачу видалено", Toast.LENGTH_SHORT).show()
                refreshScreen()
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun refreshScreen() {
        val role = AuthManager.currentRole()
        val email = AuthManager.currentEmail()
        val visibleTasks = TaskManager.getVisibleTasks(role, email)
        val stats = TaskManager.calculateStats(visibleTasks)

        userInfoTextView.text = "$email | роль: ${role.displayName}"
        statTotalTextView.text = "Усього\n${stats.total}"
        statActiveTextView.text = "Активні\n${stats.active}"
        statDoneTextView.text = "Виконані\n${stats.completed}"
        statOverdueTextView.text = "Прострочені\n${stats.overdue}"
        statPercentTextView.text = "Відсоток виконання: ${stats.completionPercent}%"
        completionProgressBar.progress = stats.completionPercent

        emptyCard.visibility = if (visibleTasks.isEmpty()) View.VISIBLE else View.GONE
        taskAdapter.submitList(visibleTasks)
    }

    private fun showDeadlineReminderIfNeeded() {
        val visibleTasks = TaskManager.getVisibleTasks(AuthManager.currentRole(), AuthManager.currentEmail())
        val reminders = TaskManager.tasksForReminder(visibleTasks)
        if (reminders.isNotEmpty()) {
            val firstTask = reminders.first()
            Toast.makeText(
                this,
                "Нагадування: дедлайн «${firstTask.title}» - ${TaskManager.formatDate(firstTask.deadlineMillis)}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDeadlineMillis

        DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedDeadlineMillis = selectedCalendar.timeInMillis
                deadlineEditText.setText(TaskManager.formatDate(selectedDeadlineMillis))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun resetForm() {
        editingTaskId = null
        selectedDeadlineMillis = TaskManager.startOfTodayMillis()
        formTitleTextView.text = "Нова задача"
        saveTaskButton.text = "Додати"
        titleEditText.text?.clear()
        descriptionEditText.text?.clear()
        deadlineEditText.setText(TaskManager.formatDate(selectedDeadlineMillis))
        prioritySpinner.setSelection(TaskPriority.entries.indexOf(TaskPriority.MEDIUM))
    }

    private fun openLoginScreen() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun openAdminUsers() {
        startActivity(Intent(this, AdminUsersActivity::class.java))
    }

    private fun openAdminReport() {
        startActivity(Intent(this, AdminReportActivity::class.java))
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
