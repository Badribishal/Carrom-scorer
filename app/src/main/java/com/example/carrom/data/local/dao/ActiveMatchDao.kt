package com.example.carrom.data.local.dao

import androidx.room.*
import com.example.carrom.data.local.entity.ActiveMatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveMatchDao {
    @Query("SELECT * FROM active_match WHERE id = 1 LIMIT 1")
    fun getActiveMatchFlow(): Flow<ActiveMatchEntity?>

    @Query("SELECT * FROM active_match WHERE id = 1 LIMIT 1")
    suspend fun getActiveMatch(): ActiveMatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveActiveMatch(activeMatch: ActiveMatchEntity)

    @Query("DELETE FROM active_match WHERE id = 1")
    suspend fun clearActiveMatch()
}
