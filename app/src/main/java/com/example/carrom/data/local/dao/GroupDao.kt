package com.example.carrom.data.local.dao

import androidx.room.*
import com.example.carrom.data.local.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups ORDER BY name ASC")
    suspend fun getAllGroupsList(): List<GroupEntity>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroupById(id: Long): GroupEntity?

    @Query("SELECT * FROM groups WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getGroupByName(name: String): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<GroupEntity>): List<Long>

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteGroupById(id: Long)

    @Query("DELETE FROM groups")
    suspend fun deleteAllGroups()
}
