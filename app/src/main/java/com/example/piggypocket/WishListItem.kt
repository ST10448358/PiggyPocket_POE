package com.example.piggypocket

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wish_list_items")
data class WishListItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: Long,
    val icon: String = "🎁"
)
