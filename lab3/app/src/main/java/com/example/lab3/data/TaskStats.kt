package com.example.lab3.data

data class TaskStats(
    val total: Int,
    val completed: Int,
    val overdue: Int,
    val active: Int,
    val lowPriority: Int,
    val mediumPriority: Int,
    val highPriority: Int,
    val completionPercent: Int
)
