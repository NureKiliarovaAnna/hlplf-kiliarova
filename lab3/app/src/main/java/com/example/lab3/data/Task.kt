package com.example.lab3.data

data class Task(
    val id: Long,
    var title: String,
    var description: String,
    var deadlineMillis: Long,
    var priority: TaskPriority,
    var isCompleted: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val ownerEmail: String
)
