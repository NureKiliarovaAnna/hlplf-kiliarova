package com.example.lab3.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.lab3.R
import com.example.lab3.data.User
import com.example.lab3.data.UserRole
import com.example.lab3.manager.TaskManager

class UserAdapter : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {
    private val users = mutableListOf<User>()

    fun submitList(newUsers: List<User>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int {
        return users.size
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val emailTextView: TextView = itemView.findViewById(R.id.userEmailTextView)
        private val avatarTextView: TextView = itemView.findViewById(R.id.userAvatarTextView)
        private val roleTextView: TextView = itemView.findViewById(R.id.userRoleTextView)
        private val statusTextView: TextView = itemView.findViewById(R.id.userStatusTextView)
        private val statusDot: View = itemView.findViewById(R.id.userStatusDot)
        private val statsTextView: TextView = itemView.findViewById(R.id.userStatsTextView)

        fun bind(user: User) {
            val tasks = if (user.role == UserRole.ADMIN) {
                TaskManager.getAllTasks()
            } else {
                TaskManager.getAllTasks().filter { it.ownerEmail == user.email }
            }
            val stats = TaskManager.calculateStats(tasks)

            emailTextView.text = user.email
            avatarTextView.text = user.email.first().uppercase()
            roleTextView.text = user.role.displayName
            roleTextView.background = badgeBackground(user.role)
            statusTextView.text = if (user.role == UserRole.ADMIN) "online · адміністратор" else "offline · виконавець"
            statusDot.setBackgroundResource(if (user.role == UserRole.ADMIN) R.drawable.bg_online_dot else R.drawable.bg_offline_dot)
            statsTextView.text = "Задачі: ${stats.total}     Активні: ${stats.active}     Виконані: ${stats.completed}     Прострочені: ${stats.overdue}"
        }

        private fun badgeBackground(role: UserRole): GradientDrawable {
            val colorRes = if (role == UserRole.ADMIN) R.color.primary else R.color.accent
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f
                setColor(ContextCompat.getColor(itemView.context, colorRes))
            }
        }
    }
}
