package com.rizal.catatemas.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * Foto item disimpan di penyimpanan internal aplikasi (bukan cuma referensi uri),
 * supaya tidak butuh permission & tetap ada walau file asli dihapus.
 */
class PhotoStore(private val context: Context) {

    private val dir: File get() = File(context.filesDir, "photos").apply { mkdirs() }

    fun saveFromUri(uri: Uri): String? {
        val bitmap = decodeUri(context, uri, 1200) ?: return null
        val file = File(dir, "gold_${System.currentTimeMillis()}_${(0..999).random()}.jpg")
        val ok = runCatching {
            file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 82, out) }
        }.isSuccess
        bitmap.recycle()
        return if (ok) file.absolutePath else null
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    companion object {
        fun decodeUri(context: Context, uri: Uri, maxSize: Int): Bitmap? = runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val width = bounds.outWidth
            val height = bounds.outHeight
            if (width <= 0 || height <= 0) return null
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(width, height, maxSize) }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }.getOrNull()

        fun decodeFile(path: String?, maxSize: Int): Bitmap? {
            if (path.isNullOrBlank()) return null
            if (!File(path).exists()) return null
            return runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSize)
                }
                BitmapFactory.decodeFile(path, options)
            }.getOrNull()
        }

        private fun sampleSize(width: Int, height: Int, maxSize: Int): Int {
            var sample = 1
            val largest = maxOf(width, height)
            while (largest / (sample * 2) >= maxSize) sample *= 2
            return sample
        }
    }
}
