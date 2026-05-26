package com.example.lab3.data

enum class UserRole(val displayName: String) {
    ADMIN("admin"),
    USER("user");

    companion object {
        fun fromValue(value: String): UserRole {
            return entries.firstOrNull { it.displayName == value } ?: USER
        }
    }
}
