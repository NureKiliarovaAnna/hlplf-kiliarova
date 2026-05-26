package com.example.lab3.data

enum class TaskPriority(val displayName: String) {
    LOW("Низький"),
    MEDIUM("Середній"),
    HIGH("Високий");

    companion object {
        fun fromDisplayName(value: String): TaskPriority {
            return entries.firstOrNull { it.displayName == value } ?: MEDIUM
        }
    }
}
