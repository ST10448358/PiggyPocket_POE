package com.example.piggypocket

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Main dashboard screen that provides an overview of the user's financial status,
 * including remaining budget, total expenses, income, and recent transactions.
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager

    private lateinit var tvUsername: TextView
    private lateinit var tvRemainingAmount: TextView
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvAchievementCount: TextView
    private lateinit var rvCategoryProgress: RecyclerView
    private lateinit var rvRecentExpenses: RecyclerView
    
    // Smart Alerts Components
    private lateinit var cvSmartAlerts: CardView
    private lateinit var tvAlertContent: TextView
    private lateinit var tvNotificationBadge: TextView
    private lateinit var tvWishProgress: TextView
    private val smartNotifications = mutableListOf<SmartNotification>()

    private lateinit var tvNoExpenses: TextView
    
    // User Icon
    private lateinit var ivUserAvatar: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize database and session management
        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)

        // Initialize UI components
        tvUsername = findViewById(R.id.tvUsername)
        tvRemainingAmount = findViewById(R.id.tvRemainingAmount)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvAchievementCount = findViewById(R.id.tvAchievementCount)
        rvCategoryProgress = findViewById(R.id.rvCategoryProgress)
        rvRecentExpenses = findViewById(R.id.rvRecentExpenses)
        tvNoExpenses = findViewById(R.id.tvNoExpenses)
        
        ivUserAvatar = findViewById(R.id.ivUserAvatar)
        
        cvSmartAlerts = findViewById(R.id.cvSmartAlerts)
        tvAlertContent = findViewById(R.id.tvAlertContent)
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge)
        tvWishProgress = findViewById(R.id.tvWishProgress)

        // Setup RecyclerViews with Linear Layout Managers
        rvCategoryProgress.layoutManager = LinearLayoutManager(this)
        rvRecentExpenses.layoutManager = LinearLayoutManager(this)

        // Navigation: Profile & Avatar Customization
        findViewById<View>(R.id.cvUserIcon).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Navigation: Edit Budget (Settings)
        findViewById<View>(R.id.ivEditBudget).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Navigation: Add New Expense
        findViewById<LinearLayout>(R.id.btnAdd).setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        // Navigation: View Spending Summary (Analysis)
        findViewById<View>(R.id.tvAnalyze)?.setOnClickListener {
            startActivity(Intent(this, SpendingSummaryActivity::class.java))
        }

        // Navigation: View All Expenses
        findViewById<View>(R.id.tvViewAll)?.setOnClickListener {
            startActivity(Intent(this, ExpenseHistoryActivity::class.java))
        }

        // Navigation: History Screen
        findViewById<LinearLayout>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, ExpenseHistoryActivity::class.java))
        }

        // Navigation: Categories Screen
        findViewById<LinearLayout>(R.id.btnCategories).setOnClickListener {
            startActivity(Intent(this, CategoriesActivity::class.java))
        }

        // Navigation: Goals Screen
        findViewById<LinearLayout>(R.id.btnGoals).setOnClickListener {
            startActivity(Intent(this, GoalsActivity::class.java))
        }

        // Navigation: Settings Screen
        findViewById<LinearLayout>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Navigation: Achievements Screen
        findViewById<View>(R.id.clAchievement).setOnClickListener {
            startActivity(Intent(this, AchievementsActivity::class.java))
        }

        // Navigation: Wish List Screen
        findViewById<View>(R.id.cvWishList).setOnClickListener {
            startActivity(Intent(this, WishListActivity::class.java))
        }

        // Navigation: Show Smart Notifications Dialog
        findViewById<View>(R.id.rlNotifications).setOnClickListener {
            showNotificationsDialog()
        }

        // Logout functionality
        findViewById<View>(R.id.btnLogout).setOnClickListener {
            sessionManager.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh dashboard data every time the screen becomes visible
        loadDashboardData()
    }

    /**
     * Fetches user data, expenses, and categories from the database to calculate
     * and display current financial statistics.
     */
    private fun loadDashboardData() {
        val userId = sessionManager.getUserId()
        // Redirect to login if session is invalid
        if (userId == -1) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        lifecycleScope.launch {
            val user = db.userDao().getUserById(userId)
            val allExpenses = db.expenseDao().getAllExpenses(userId)
            val allCategories = db.categoryDao().getAllCategories(userId)
            val wishItems = db.wishListDao().getWishListForUser(userId)
            
            // Calculate start of the current month to filter expenses
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startOfMonth = calendar.timeInMillis
            
            // Filter expenses and calculate totals
            val monthlyExpenses = allExpenses.filter { it.date >= startOfMonth }
            val totalSpent = monthlyExpenses.sumOf { it.amount }
            val income = user?.income ?: 0.0
            val budget = user?.monthlyBudget ?: 0.0
            val remaining = budget - totalSpent

            // Map categories to their respective spending amounts for progress visualization
            val categoryWithSpent = allCategories.map { category ->
                val spent = monthlyExpenses.filter { it.categoryId == category.id }.sumOf { it.amount }
                CategoryWithSpent(category, spent)
            }

            val categoryMap = allCategories.associate { it.id to it.name }

            // Basic logic for tracking achievement progress
            var unlockedCount = 0
            if (allExpenses.isNotEmpty()) unlockedCount++
            if (allExpenses.size >= 5) unlockedCount++
            if (allExpenses.size >= 10) unlockedCount++
            if (allExpenses.sumOf { it.amount } >= 5000) unlockedCount++

            // Update UI components on the main thread
            runOnUiThread {
                val displayName = if (user != null && user.fullName.isNotEmpty()) user.fullName else user?.username ?: "User"
                tvUsername.text = displayName
                
                // Update Profile Picture
                user?.let { u ->
                    val resId = resources.getIdentifier(u.avatarResourceName, "drawable", packageName)
                    if (resId != 0) {
                        ivUserAvatar.setImageResource(resId)
                    } else {
                        ivUserAvatar.setImageResource(R.drawable.ic_person_placeholder)
                    }
                }
                
                tvRemainingAmount.text = "R${String.format("%.2f", remaining)}"
                tvTotalExpenses.text = "R${String.format("%.2f", totalSpent)}"
                tvTotalIncome.text = "R${String.format("%.2f", income)}"
                tvAchievementCount.text = "$unlockedCount / 4 Unlocked"
                
                // Update Wish List Progress text
                if (wishItems.isNotEmpty()) {
                    val totalTarget = wishItems.sumOf { it.targetAmount }
                    val totalSaved = wishItems.sumOf { it.currentAmount }
                    val percent = if (totalTarget > 0) (totalSaved / totalTarget * 100).toInt() else 0
                    tvWishProgress.text = "$percent% saved towards your goals"
                } else {
                    tvWishProgress.text = "Start saving towards goals"
                }
                
                rvCategoryProgress.adapter = CategoryProgressAdapter(categoryWithSpent)
                rvRecentExpenses.adapter = ExpenseAdapter(allExpenses.take(5), categoryMap)
                
                // Show empty state message if no expenses exist
                if (allExpenses.isEmpty()) {
                    tvNoExpenses.visibility = View.VISIBLE
                    rvRecentExpenses.visibility = View.GONE
                } else {
                    tvNoExpenses.visibility = View.GONE
                    rvRecentExpenses.visibility = View.VISIBLE
                }

                // Check and show Smart Alerts
                generateSmartAlerts(user, monthlyExpenses, categoryWithSpent)
            }
        }
    }

    /**
     * Analyzes spending patterns and goals to generate proactive alerts.
     */
    private fun generateSmartAlerts(user: User?, monthlyExpenses: List<Expense>, categoryProgress: List<CategoryWithSpent>) {
        smartNotifications.clear()

        // 1. Check Category Limits
        categoryProgress.forEach { item ->
            if (item.category.budgetLimit > 0) {
                val percent = (item.spent / item.category.budgetLimit) * 100
                val catName = item.category.name.split(" ").first() // Remove emoji for cleaner text

                if (percent >= 100) {
                    smartNotifications.add(SmartNotification("Budget Exceeded", "You have exceeded your $catName budget by R${String.format("%.2f", item.spent - item.category.budgetLimit)}.", "⚠️"))
                } else if (percent >= 80) {
                    smartNotifications.add(SmartNotification("Budget Warning", "You have used ${percent.toInt()}% of your $catName budget.", "🔔"))
                }
            }
        }

        // 2. Check Daily Log Commitment
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        val hasLoggedToday = monthlyExpenses.any { it.date >= today }
        if (!hasLoggedToday && monthlyExpenses.isNotEmpty()) {
            smartNotifications.add(SmartNotification("Daily Reminder", "Don't forget to log today's expenses.", "📝"))
        }

        // 3. Check Overall Monthly Goals
        user?.let { u ->
            val totalSpent = monthlyExpenses.sumOf { it.amount }
            
            // Close to Min Goal (Savings Goal)
            if (u.minMonthlyGoal > 0 && totalSpent < u.minMonthlyGoal) {
                val percent = (totalSpent / u.minMonthlyGoal) * 100
                if (percent >= 90) {
                    smartNotifications.add(SmartNotification("Savings Goal", "You are close to reaching your monthly savings goal!", "🎯"))
                }
            }

            // Exceeding Max Goal
            if (u.maxMonthlyGoal > 0 && totalSpent > u.maxMonthlyGoal) {
                smartNotifications.add(SmartNotification("Spending Alert", "Warning: You have exceeded your maximum spending goal of R${u.maxMonthlyGoal}!", "🚫"))
            }
        }

        // Update notification UI
        if (smartNotifications.isNotEmpty()) {
            tvNotificationBadge.visibility = View.VISIBLE
            tvNotificationBadge.text = smartNotifications.size.toString()
            cvSmartAlerts.visibility = View.VISIBLE
            tvAlertContent.text = smartNotifications.first().message
        } else {
            tvNotificationBadge.visibility = View.GONE
            cvSmartAlerts.visibility = View.GONE
        }
    }

    private fun showNotificationsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_smart_notifications, null)
        val rvNotif = dialogView.findViewById<RecyclerView>(R.id.rvNotifications)
        val tvCount = dialogView.findViewById<TextView>(R.id.tvNotificationCount)
        val tvNoNotif = dialogView.findViewById<TextView>(R.id.tvNoNotifications)
        val btnDismissAll = dialogView.findViewById<TextView>(R.id.btnDismissAll)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)

        val dialog = AlertDialog.Builder(this, R.style.CustomDialog)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        tvCount.text = smartNotifications.size.toString()
        
        if (smartNotifications.isEmpty()) {
            tvNoNotif.visibility = View.VISIBLE
            rvNotif.visibility = View.GONE
            btnDismissAll.visibility = View.GONE
        } else {
            tvNoNotif.visibility = View.GONE
            rvNotif.visibility = View.VISIBLE
            btnDismissAll.visibility = View.VISIBLE
            rvNotif.layoutManager = LinearLayoutManager(this)
            rvNotif.adapter = NotificationAdapter(smartNotifications)
        }

        btnDismissAll.setOnClickListener {
            smartNotifications.clear()
            tvNotificationBadge.visibility = View.GONE
            cvSmartAlerts.visibility = View.GONE
            dialog.dismiss()
            Toast.makeText(this, "All notifications dismissed", Toast.LENGTH_SHORT).show()
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}