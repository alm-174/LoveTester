package com.example.lovetester.ui.memory

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun decodeMemoryBitmap(resolver: ContentResolver, uri: Uri): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            val maxSide = 1024
            if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                    val size = info.size
                    val longestSide = maxOf(size.width, size.height)
                    if (longestSide > maxSide) {
                        decoder.setTargetSize(
                            (size.width.toLong() * maxSide / longestSide).toInt().coerceAtLeast(1),
                            (size.height.toLong() * maxSide / longestSide).toInt().coerceAtLeast(1)
                        )
                    }
                }
            } else {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                if (options.outWidth <= 0 || options.outHeight <= 0) return@withContext null

                options.inSampleSize = 1
                while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > maxSide) {
                    options.inSampleSize *= 2
                }
                options.inJustDecodeBounds = false
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            }
        } catch (_: java.io.IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }
