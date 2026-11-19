package com.example.lovetester.data.repository

import android.net.Uri
import com.example.lovetester.data.local.database.MemoryDao
import com.example.lovetester.data.local.database.MemoryEntity
import com.example.lovetester.data.local.storage.MemoryImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MemoryRepository @Inject constructor(
    private val dao: MemoryDao,
    private val imageStorage: MemoryImageStorage
) {

    fun observeMemories(): Flow<List<MemoryEntity>> {
        return dao.observeAll()
    }

    suspend fun addMemory(
        title: String,
        date: String,
        imageUri: Uri
    ) = withContext(Dispatchers.IO) {

        val imagePath = imageStorage.saveImage(imageUri)

        try {
            dao.insert(
                MemoryEntity(
                    title = title,
                    date = date,
                    imagePath = imagePath
                )
            )
        } catch (e: Exception) {
            imageStorage.deleteImage(imagePath)
            throw e
        }
    }

    suspend fun deleteMemory(id: Long) = withContext(Dispatchers.IO) {
        val memory = dao.findById(id) ?: return@withContext
        dao.delete(memory)
        imageStorage.deleteImage(memory.imagePath)
    }
}