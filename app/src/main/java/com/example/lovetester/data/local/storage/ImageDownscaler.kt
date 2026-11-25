package com.example.lovetester.data.local.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Saves a picked/captured image into app storage, shrunk so its longest side is at most
 * [MAX_EDGE] px. Applies the EXIF rotation (the saved file has no EXIF) and runs on Dispatchers.IO.
 */
object ImageDownscaler {

    const val MAX_EDGE = 1440

    suspend fun saveDownscaled(
        context: Context,
        uri: Uri,
        destination: File,
        maxEdge: Int = MAX_EDGE
    ) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // 1. Read only the size.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: error("Cannot open selected image")
        check(bounds.outWidth > 0 && bounds.outHeight > 0) { "Selected file is not an image" }

        // 2. Decode with a power-of-two sample that keeps the longest side >= maxEdge.
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Cannot decode selected image")

        // 3. EXIF rotation + exact downscale in one matrix.
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val scale = minOf(1f, maxEdge / maxOf(decoded.width, decoded.height).toFloat())
        val matrix = Matrix().apply {
            applyExifOrientation(orientation)
            postScale(scale, scale)
        }
        val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        try {
            // PNG keeps transparency; everything else is stored as JPEG.
            val format = if (result.hasAlpha()) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            destination.outputStream().use { out ->
                check(result.compress(format, 90, out)) { "Cannot write image" }
            }
        } finally {
            if (result !== decoded) decoded.recycle()
            result.recycle()
        }
    }

    private fun Matrix.applyExifOrientation(orientation: Int) {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                postRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                postRotate(270f)
                postScale(-1f, 1f)
            }
        }
    }
}
