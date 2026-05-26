package com.example.lab3.manager

import android.content.Context
import android.content.SharedPreferences
import com.example.lab3.data.User
import com.example.lab3.data.UserRole

object AuthManager {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_EMAIL = "email"
    private const val KEY_ROLE = "role"
    private const val KEY_LOGGED_IN = "logged_in"

    private lateinit var preferences: SharedPreferences

    private val users = listOf(
        User("admin@example.com", "admin123", UserRole.ADMIN),
        User("user@example.com", "user123", UserRole.USER)
    )

    fun init(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun login(email: String, password: String): Boolean {
        val user = users.firstOrNull { it.email == email.trim() && it.password == password }
        return if (user != null) {
            preferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_EMAIL, user.email)
                .putString(KEY_ROLE, user.role.displayName)
                .apply()
            true
        } else {
            false
        }
    }

    fun logout() {
        preferences.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return preferences.getBoolean(KEY_LOGGED_IN, false)
    }

    fun currentEmail(): String {
        return preferences.getString(KEY_EMAIL, "") ?: ""
    }

    fun currentRole(): UserRole {
        return UserRole.fromValue(preferences.getString(KEY_ROLE, UserRole.USER.displayName) ?: UserRole.USER.displayName)
    }

    fun getUsers(): List<User> {
        return users
    }
}
