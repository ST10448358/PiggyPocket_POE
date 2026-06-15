package com.example.piggypocket

import androidx.room.*

@Dao
interface WishListItemDao {
    @Query("SELECT * FROM wish_list_items WHERE userId = :userId ORDER BY targetDate ASC")
    suspend fun getWishListForUser(userId: Int): List<WishListItem>

    @Insert
    suspend fun insert(item: WishListItem): Long

    @Update
    suspend fun update(item: WishListItem)

    @Delete
    suspend fun delete(item: WishListItem)

    @Query("SELECT * FROM wish_list_items WHERE id = :id")
    suspend fun getById(id: Int): WishListItem?
}
