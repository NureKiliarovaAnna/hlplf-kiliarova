package com.example.lab3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lab3.manager.AuthManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: створення екрана авторизації")
        setContentView(R.layout.activity_login)

        AuthManager.init(this)
        if (AuthManager.isLoggedIn()) {
            openMainScreen()
            return
        }

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)

        loginButton.setOnClickListener { login() }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: екран авторизації видимий")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: користувач може вводити email і пароль")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: авторизація тимчасово призупинена")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: екран авторизації не видимий")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: екран авторизації знищено")
    }

    private fun login() {
        val email = emailEditText.text?.toString()?.trim().orEmpty()
        val password = passwordEditText.text?.toString().orEmpty()

        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Заповніть email і пароль", Toast.LENGTH_SHORT).show()
            return
        }

        if (AuthManager.login(email, password)) {
            Toast.makeText(this, "Вхід виконано", Toast.LENGTH_SHORT).show()
            openMainScreen()
        } else {
            Toast.makeText(this, "Невірний email або пароль", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openMainScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}
