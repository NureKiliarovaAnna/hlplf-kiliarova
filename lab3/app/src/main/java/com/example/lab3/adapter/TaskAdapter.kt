package com.example.lab3.adapter

import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.lab3.R
import com.example.lab3.data.Task
import com.example.lab3.data.TaskPriority
import com.example.lab3.manager.TaskManager
import com.google.android.material.button.MaterialButton

class TaskAdapter(
    private val showOwner: Boolean,
    private val onDetailsClick: (Task) -> Unit,
    private val onEditClick: (Task) -> Unit,
    private val onToggleClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val tasks = mutableListOf<Task>()

    fun submitList(newTasks: List<Task>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int {
        return tasks.size
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val statusStripe: View = itemView.findViewById(R.id.statusStripe)
        private val titleTextView: TextView = itemView.findViewById(R.id.taskTitleTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.taskDescriptionTextView)
        private val metaTextView: TextView = itemView.findViewById(R.id.taskMetaTextView)
        private val statusTextView: TextView = itemView.findViewById(R.id.taskStatusTextView)
        private val priorityTextView: TextView = itemView.findViewById(R.id.taskPriorityTextView)
        private val detailsButton: MaterialButton = itemView.findViewById(R.id.detailsButton)
        private val editButton: MaterialButton = itemView.findViewById(R.id.editButton)
        private val completeButton: MaterialButton = itemView.findViewById(R.id.completeButton)
        private val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteButton)

        fun bind(task: Task) {
            titleTextView.text = task.title
            descriptionTextView.text = task.description.ifBlank { "Опис не додано" }
            metaTextView.text = if (showOwner) {
                "Дедлайн: ${TaskManager.formatDate(task.deadlineMillis)} | Автор: ${task.ownerEmail}"
            } else {
                "Дедлайн: ${TaskManager.formatDate(task.deadlineMillis)}"
            }

            if (task.isCompleted) {
                titleTextView.paintFlags = titleTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                titleTextView.paintFlags = titleTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            val statusColor = when {
                task.isCompleted -> ContextCompat.getColor(itemView.context, R.color.success)
                TaskManager.isOverdue(task) -> ContextCompat.getColor(itemView.context, R.color.danger)
                else -> ContextCompat.getColor(itemView.context, R.color.primary)
            }

            statusStripe.setBackgroundColor(statusColor)
            statusTextView.text = when {
                task.isCompleted -> "Виконано"
                TaskManager.isOverdue(task) -> "Прострочено"
                else -> "Активна"
            }
            statusTextView.background = roundedBackground(statusColor)

            val priorityColor = priorityColor(task.priority)
            priorityTextView.text = task.priority.displayName
            priorityTextView.background = roundedBackground(priorityColor)

            completeButton.contentDescription = if (task.isCompleted) "Активувати" else "Позначити виконаною"
            detailsButton.setOnClickListener { onDetailsClick(task) }
            editButton.setOnClickListener { onEditClick(task) }
            completeButton.setOnClickListener { onToggleClick(task) }
            deleteButton.setOnClickListener { onDeleteClick(task) }
        }

        private fun priorityColor(priority: TaskPriority): Int {
            val colorRes = when (priority) {
                TaskPriority.LOW -> R.color.priority_low
                TaskPriority.MEDIUM -> R.color.priority_medium
                TaskPriority.HIGH -> R.color.priority_high
            }
            return ContextCompat.getColor(itemView.context, colorRes)
        }

        private fun roundedBackground(color: Int): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f
                setColor(color)
            }
        }
    }
}
