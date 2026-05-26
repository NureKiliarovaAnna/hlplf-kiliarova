package com.example.lab3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab3.adapter.UserAdapter
import com.example.lab3.data.UserRole
import com.example.lab3.manager.AuthManager
import com.example.lab3.manager.TaskManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

class AdminUsersActivity : AppCompatActivity() {
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var backButton: MaterialButton
    private lateinit var adminBottomNavigation: BottomNavigationView
    private lateinit var userAdapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: створення екрана користувачів")
        setContentView(R.layout.activity_admin_users)

        AuthManager.init(this)
        TaskManager.init(this)
        if (AuthManager.currentRole() != UserRole.ADMIN) {
            Toast.makeText(this, "Цей екран доступний тільки адміністратору", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()
        setupRecyclerView()
        setupNavigation()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: екран користувачів видимий")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: оновлення списку користувачів")
        if (::userAdapter.isInitialized) {
            userAdapter.submitList(AuthManager.getUsers())
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: екран користувачів призупинено")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: екран користувачів не видимий")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: екран користувачів знищено")
    }

    private fun bindViews() {
        usersRecyclerView = findViewById(R.id.usersRecyclerView)
        backButton = findViewById(R.id.backButton)
        adminBottomNavigation = findViewById(R.id.adminBottomNavigation)
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter()
        usersRecyclerView.layoutManager = LinearLayoutManager(this)
        usersRecyclerView.adapter = userAdapter
        userAdapter.submitList(AuthManager.getUsers())
    }

    private fun setupNavigation() {
        backButton.setOnClickListener { finish() }
        adminBottomNavigation.selectedItemId = R.id.nav_users
        adminBottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tasks -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_users -> true
                R.id.nav_report -> {
                    startActivity(Intent(this, AdminReportActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    companion object {
        private const val TAG = "AdminUsersActivity"
    }
}
