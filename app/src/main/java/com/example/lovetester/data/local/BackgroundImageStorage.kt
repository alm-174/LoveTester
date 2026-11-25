package com.example.lovetester.data.local

import android.content.Context
import android.net.Uri
import com.example.lovetester.data.local.storage.ImageDownscaler
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
                // Shrinks to <= 1440px on Dispatchers.IO; the original is never copied as-is.
                ImageDownscaler.saveDownscaled(context, uri, destination)

                check(destination.length() > 0L) {
                    "Saved image is empty"
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
