package com.example.lovetester.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MemoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class MemoryDatabase : RoomDatabase() {

    abstract fun memoryDao(): MemoryDao
}