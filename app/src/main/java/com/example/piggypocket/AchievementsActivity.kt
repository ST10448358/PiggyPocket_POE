package com.example.piggypocket

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Screen displaying the user's unlocked milestones and financial goals progress.
 * Achievements are dynamically updated based on spending habits and user-set targets.
 */
class AchievementsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        // Database and session initialization
        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)

        // Setup back navigation to the Dashboard
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        findViewById<View>(R.id.cvGameButton).setOnClickListener {
            startActivity(Intent(this, SavingsGoalRunnerActivity::class.java))
        }

        // Trigger achievement check
        checkAchievements()
    }

    /**
     * Evaluates current user data, expenses, and goals to determine which achievements are unlocked.
     */
    private fun checkAchievements() {
        lifecycleScope.launch {
            val userId = sessionManager.getUserId()
            if (userId == -1) return@launch
            
            val user = db.userDao().getUserById(userId)
            val expenses = db.expenseDao().getAllExpenses(userId)
            
            // Determine monthly spending for goal-based achievement logic
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startOfMonth = calendar.timeInMillis
            val monthlySpent = expenses.filter { it.date >= startOfMonth }.sumOf { it.amount }

            var unlockedCount = 0
            var totalPossible = 4

            // Achievement 1: First Step - Unlocks after logging the first expense
            if (expenses.isNotEmpty()) {
                unlockAchievement(R.id.ivA1, R.id.tvA1Title, R.id.tvA1Desc, "First Step 🐣", "You've hatched your first expense!", android.R.drawable.btn_star_big_on)
                unlockedCount++
            }

            // Achievement 2: Savings Sprout - Unlocks after logging 5 expenses
            if (expenses.size >= 5) {
                unlockAchievement(R.id.ivA2, R.id.tvA2Title, R.id.tvA2Desc, "Savings Sprout 🌱", "Your garden is growing!", android.R.drawable.ic_menu_gallery)
                unlockedCount++
            }

            // Achievement 3: Budget Boss - Unlocks after logging 10 expenses
            if (expenses.size >= 10) {
                unlockAchievement(R.id.ivA3, R.id.tvA3Title, R.id.tvA3Desc, "Budget Boss 💼", "Managing like a pro!", android.R.drawable.ic_menu_manage)
                unlockedCount++
            }

            // Achievement 4: Money Master - Unlocks when total spending exceeds R5000
            val totalSpent = expenses.sumOf { it.amount }
            if (totalSpent >= 5000) {
                unlockAchievement(R.id.ivA4, R.id.tvA4Title, R.id.tvA4Desc, "Money Master 👑", "The ultimate financial guru!", android.R.drawable.btn_star_big_on)
                unlockedCount++
            }

            // Goal-Linked Achievements: Dynamically shown based on user configuration
            user?.let {
                // Minimum Spending Goal Tracking
                if (it.minMonthlyGoal > 0) {
                    totalPossible++
                    runOnUiThread { findViewById<View>(R.id.cvMinGoal).visibility = View.VISIBLE }
                    if (monthlySpent >= it.minMonthlyGoal) {
                        unlockAchievement(R.id.ivMinGoal, R.id.tvMinGoalTitle, R.id.tvMinGoalDesc, "Minimum Goal Achieved! 🎯", "You've reached your minimum spending target.", android.R.drawable.checkbox_on_background)
                        unlockedCount++
                    }
                }

                // Maximum Spending Goal Tracking (Staying in the Safe Zone)
                if (it.maxMonthlyGoal > 0) {
                    totalPossible++
                    runOnUiThread { findViewById<View>(R.id.cvMaxGoal).visibility = View.VISIBLE }
                    if (expenses.isNotEmpty() && monthlySpent <= it.maxMonthlyGoal) {
                        unlockAchievement(R.id.ivMaxGoal, R.id.tvMaxGoalTitle, R.id.tvMaxGoalDesc, "Safe Zone Keeper 🛡️", "Stayed under your maximum spending goal.", android.R.drawable.checkbox_on_background)
                        unlockedCount++
                    }
                }
            }

            // Update the progress summary text
            runOnUiThread {
                findViewById<TextView>(R.id.tvProgress).text = "Your Progress 📈\n$unlockedCount / $totalPossible"
            }
        }
    }

    /**
     * Updates the UI for a specific achievement card to reflect its "Unlocked" state.
     */
    private fun unlockAchievement(iconId: Int, titleId: Int, descId: Int, title: String, desc: String, iconRes: Int) {
        runOnUiThread {
            // Apply visual changes to show completion
            findViewById<ImageView>(iconId).apply {
                setImageResource(iconRes)
                alpha = 1.0f
                setColorFilter(Color.parseColor("#FFD700")) // Highlight with gold color
            }
            findViewById<TextView>(titleId).apply {
                text = title
                setTextColor(Color.BLACK) // Bold active title
            }
            findViewById<TextView>(descId).apply {
                text = desc
                setTextColor(Color.GRAY) // Active description
            }
        }
    }
}