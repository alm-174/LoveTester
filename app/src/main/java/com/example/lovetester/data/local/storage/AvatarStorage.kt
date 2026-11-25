
package com.example.lovetester.data.local.storage

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

class AvatarStorage @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    suspend fun saveAvatar(uri: Uri): String {
        return withContext(Dispatchers.IO) {
            val directory = File(context.filesDir, "avatars")

            if (!directory.exists()) {
                check(directory.mkdirs() || directory.isDirectory) {
                    "Cannot create avatars directory"
                }
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
    }

    suspend fun deleteAvatar(path: String) {
        withContext(Dispatchers.IO) {
            val directory = File(context.filesDir, "avatars")
            val file = File(path)

            // Chỉ cho phép xóa file nằm trực tiếp trong thư mục avatars.
            if (file.canonicalFile.parentFile == directory.canonicalFile) {
                if (file.exists() && !file.delete()) {
                    error("Cannot delete avatar file")
                }
            }
        }
    }
}
