package com.example.lovetester.data.local.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Query("SELECT * FROM memories ORDER BY id DESC")
    fun observeAll(): Flow<List<MemoryEntity>>

    @Insert
    suspend fun insert(memory: MemoryEntity): Long

    @Query("SELECT * FROM memories WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): MemoryEntity?

    @Delete
    suspend fun delete(memory: MemoryEntity)
}