package com.example.piggypocket

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.TransitionManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * Screen for setting and managing financial goals (Minimum and Maximum monthly targets).
 * It features a view mode with progress visualizer and an edit mode.
 */
class GoalsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    
    // View mode components
    private lateinit var layoutViewGoals: LinearLayout
    private lateinit var tvDisplayMinGoal: TextView
    private lateinit var tvDisplayMaxGoal: TextView
    private lateinit var btnEditGoals: ImageButton
    
    // Progress components
    private lateinit var tvSafeZoneStatus: TextView
    private lateinit var vSpendingMarker: View
    
    // Edit mode components
    private lateinit var layoutEditGoals: LinearLayout
    private lateinit var etMinGoal: EditText
    private lateinit var etMaxGoal: EditText
    private lateinit var btnSaveGoals: AppCompatButton
    
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)

        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)

        // Initialize UI components
        layoutViewGoals = findViewById(R.id.layoutViewGoals)
        tvDisplayMinGoal = findViewById(R.id.tvDisplayMinGoal)
        tvDisplayMaxGoal = findViewById(R.id.tvDisplayMaxGoal)
        btnEditGoals = findViewById(R.id.btnEditGoals)
        
        tvSafeZoneStatus = findViewById(R.id.tvSafeZoneStatus)
        vSpendingMarker = findViewById(R.id.vSpendingMarker)
        
        layoutEditGoals = findViewById(R.id.layoutEditGoals)
        etMinGoal = findViewById(R.id.etMinGoal)
        etMaxGoal = findViewById(R.id.etMaxGoal)
        btnSaveGoals = findViewById(R.id.btnSaveGoals)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            if (isEditMode) {
                toggleEditMode(false)
            } else {
                finish()
            }
        }

        btnEditGoals.setOnClickListener {
            toggleEditMode(true)
        }

        loadUserGoalsAndProgress()

        btnSaveGoals.setOnClickListener {
            saveGoals()
        }
    }

    /**
     * Toggles between view mode and edit mode.
     */
    private fun toggleEditMode(enable: Boolean) {
        isEditMode = enable
        if (enable) {
            layoutViewGoals.visibility = View.GONE
            layoutEditGoals.visibility = View.VISIBLE
            btnSaveGoals.visibility = View.VISIBLE
            btnEditGoals.visibility = View.GONE
            findViewById<TextView>(R.id.tvTitle).text = "Edit Goals"
        } else {
            layoutViewGoals.visibility = View.VISIBLE
            layoutEditGoals.visibility = View.GONE
            btnSaveGoals.visibility = View.GONE
            btnEditGoals.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvTitle).text = "Financial Goals"
            loadUserGoalsAndProgress() // Reload to ensure view is up to date
        }
    }

    /**
     * Retrieves current user's goals and calculates spending progress.
     */
    private fun loadUserGoalsAndProgress() {
        val userId = sessionManager.getUserId()
        lifecycleScope.launch {
            val user = db.userDao().getUserById(userId)
            val expenses = db.expenseDao().getAllExpenses(userId)
            
            // Calculate current month's spending
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val startOfMonth = cal.timeInMillis
            val monthlySpent = expenses.filter { it.date >= startOfMonth }.sumOf { it.amount }

            user?.let {
                runOnUiThread {
                    // Populate View Mode
                    tvDisplayMinGoal.text = String.format(Locale.getDefault(), "R%.2f", it.minMonthlyGoal)
                    tvDisplayMaxGoal.text = String.format(Locale.getDefault(), "R%.2f", it.maxMonthlyGoal)
                    
                    // Update Progress Visuals
                    updateProgressVisuals(monthlySpent, it.minMonthlyGoal, it.maxMonthlyGoal)
                    
                    // Populate Edit Mode
                    etMinGoal.setText(String.format(Locale.getDefault(), "%.2f", it.minMonthlyGoal))
                    etMaxGoal.setText(String.format(Locale.getDefault(), "%.2f", it.maxMonthlyGoal))
                }
            }
        }
    }

    /**
     * Updates the Safe Zone status text and marker position.
     */
    private fun updateProgressVisuals(spent: Double, min: Double, max: Double) {
        if (max <= 0) {
            findViewById<View>(R.id.cvGoalProgress).visibility = View.GONE
            return
        }
        
        findViewById<View>(R.id.cvGoalProgress).visibility = View.VISIBLE

        val status: String
        val markerBias: Float

        // Logic for marker position and status
        if (spent < min) {
            status = "You're below your minimum goal 🧊"
            // Position in first 33% (0 to 0.33)
            val progress = if (min > 0) (spent / min).toFloat() else 0f
            markerBias = progress * 0.33f
        } else if (spent <= max) {
            status = "Great! You're in the Safe Zone ✅"
            // Position between 33% and 67% (0.33 to 0.67)
            val range = max - min
            val offset = spent - min
            val progress = if (range > 0) (offset / range).toFloat() else 0f
            markerBias = 0.33f + progress * 0.34f
        } else {
            status = "Warning: You've exceeded your maximum ⚠️"
            // Position in last 33% (0.67 to 1.0, capped)
            val overMax = spent - max
            val progress = minOf((overMax / max).toFloat(), 1.0f)
            markerBias = 0.67f + progress * 0.33f
        }

        tvSafeZoneStatus.text = status
        
        // Animate the marker to its new position
        val parent = vSpendingMarker.parent as ConstraintLayout
        val constraintSet = ConstraintSet()
        constraintSet.clone(parent)
        constraintSet.setHorizontalBias(R.id.vSpendingMarker, markerBias)
        
        TransitionManager.beginDelayedTransition(parent)
        constraintSet.applyTo(parent)
    }

    /**
     * Validates and saves the new goal values to the user's profile.
     */
    private fun saveGoals() {
        val minGoal = etMinGoal.text.toString().toDoubleOrNull() ?: 0.0
        val maxGoal = etMaxGoal.text.toString().toDoubleOrNull() ?: 0.0

        if (maxGoal > 0 && minGoal > maxGoal) {
            Toast.makeText(this, "Minimum goal cannot be greater than maximum goal", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = sessionManager.getUserId()
        lifecycleScope.launch {
            val user = db.userDao().getUserById(userId)
            user?.let {
                val updatedUser = it.copy(
                    minMonthlyGoal = minGoal,
                    maxMonthlyGoal = maxGoal
                )
                db.userDao().update(updatedUser)
                Toast.makeText(this@GoalsActivity, "Goals saved successfully", Toast.LENGTH_SHORT).show()
                toggleEditMode(false) // Switch back to view mode
            }
        }
    }
}