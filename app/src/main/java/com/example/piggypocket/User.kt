package com.example.piggypocket

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val password: String,
    val email: String,
    var monthlyBudget: Double = 0.0,
    var income: Double = 0.0,
    var minMonthlyGoal: Double = 0.0,
    var maxMonthlyGoal: Double = 0.0,
    
    // Avatar Selection
    var avatarResourceName: String = "ic_person_placeholder",
    
    // Personal Details
    var fullName: String = "",
    var phoneNumber: String = "",
    
    // Bank Card Details
    var bankCardNumber: String = "",
    var bankCardExpiry: String = "",
    var bankCardHolder: String = ""
)
