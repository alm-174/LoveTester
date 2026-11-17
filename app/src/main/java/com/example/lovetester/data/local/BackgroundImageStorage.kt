package com.example.lovetester.data.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class BackgroundImageStorage(
    private val context: Context
) {

    suspend fun saveImage(uri: Uri): String =
        withContext(Dispatchers.IO) {

            val directory = File(
                context.filesDir,
                "backgrounds"
            )

            if (!directory.exists() && !directory.mkdirs()) {
                error("Cannot create backgrounds directory")
            }

            val destination = File(
                directory,
                "${UUID.randomUUID()}.img"
            )

            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: error("Cannot open selected image")

                input.use { source ->
                    destination.outputStream().use { target ->
                        source.copyTo(target)
                    }
                }

                destination.absolutePath
            } catch (e: Exception) {
                destination.delete()
                throw e
            }
        }

    suspend fun getSavedImages(): List<String> =
        withContext(Dispatchers.IO) {

            val directory = File(
                context.filesDir,
                "backgrounds"
            )

            if (!directory.exists()) {
                return@withContext emptyList()
            }

            directory.listFiles()
                ?.filter { it.isFile && it.extension == "img" }
                ?.sortedByDescending { it.lastModified() }
                ?.map { it.absolutePath }
                ?: emptyList()
        }
}