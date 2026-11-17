
package com.example.lovetester.data.local.storage

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AvatarStorage(
    private val context: Context
) {

    suspend fun saveAvatar(uri: Uri): String {
        return withContext(Dispatchers.IO) {
            val directory = File(context.filesDir, "avatars")

            if (!directory.exists()) {
                check(directory.mkdirs() || directory.isDirectory) {
                    "Không thể tạo thư mục avatars"
                }
            }

            val destination = File(
                directory,
                "${UUID.randomUUID()}.img"
            )

            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: error("Không thể đọc ảnh đã chọn")

                input.use { source ->
                    destination.outputStream().use { output ->
                        source.copyTo(output)
                    }
                }

                check(destination.length() > 0L) {
                    "Ảnh được lưu không có dữ liệu"
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
                    error("Không thể xóa ảnh Avatar")
                }
            }
        }
    }
}
