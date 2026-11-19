package com.example.lovetester.di

import android.content.Context
import androidx.room.Room
import com.example.lovetester.data.local.database.MemoryDao
import com.example.lovetester.data.local.database.MemoryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMemoryDatabase(
        @ApplicationContext context: Context
    ): MemoryDatabase {
        return Room.databaseBuilder(
            context,
            MemoryDatabase::class.java,
            "love_tester_memory.db"
        ).build()
    }

    @Provides
    fun provideMemoryDao(
        database: MemoryDatabase
    ): MemoryDao {
        return database.memoryDao()
    }
}