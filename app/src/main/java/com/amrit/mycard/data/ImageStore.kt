package com.amrit.mycard.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.LruCache
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File

/** Encrypted photo storage. Every photo is its own AES-GCM encrypted file. */
class ImageStore(private val context: Context, private val crypto: SecureStore) {

    private val cache = object : LruCache<String, Bitmap>(((Runtime.getRuntime().maxMemory() / 1024L) / 8L).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    private fun file(id: String) = File(context.filesDir, "img_$id.enc")

    fun save(id: String, jpeg: ByteArray) {
        file(id).writeBytes(crypto.encryptBytes(jpeg))
        invalidate(id)
    }

    fun delete(id: String) {
        if (id.isEmpty()) return
        file(id).delete()
        invalidate(id)
    }

    fun invalidate(id: String) {
        val keys = cache.snapshot().keys.filter { it.startsWith("$id@") }
        keys.forEach { cache.remove(it) }
    }

    fun load(id: String, maxDim: Int): Bitmap? {
        if (id.isEmpty()) return null
        val key = "$id@$maxDim"
        val hit = cache.get(key)
        if (hit != null) return hit
        val f = file(id)
        if (!f.exists()) return null
        return try {
            val bytes = crypto.decryptBytes(f.readBytes())
            val bmp = decodeBytes(bytes, maxDim)
            if (bmp != null) cache.put(key, bmp)
            bmp
        } catch (e: Exception) {
            null
        }
    }

    fun purgeExcept(keep: Set<String>) {
        val files = context.filesDir.listFiles() ?: return
        for (f in files) {
            if (f.name.startsWith("img_") && f.name.endsWith(".enc")) {
                val id = f.name.removePrefix("img_").removeSuffix(".enc")
                if (id !in keep) f.delete()
            }
        }
    }

    private fun decodeBytes(bytes: ByteArray, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    /** Reads a picked image, fixes its rotation and limits its size. */
    fun decodeUri(uri: Uri, maxDim: Int = 2048): Bitmap? {
        try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val raw = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
            val orientation = try {
                resolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }
            return scaleDown(applyExif(raw, orientation), maxDim)
        } catch (e: Exception) {
            return null
        }
    }

    private fun applyExif(bmp: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                m.postRotate(90f)
                m.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                m.postRotate(-90f)
                m.postScale(-1f, 1f)
            }
            else -> return bmp
        }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    private fun scaleDown(bmp: Bitmap, maxDim: Int): Bitmap {
        val longest = maxOf(bmp.width, bmp.height)
        if (longest <= maxDim) return bmp
        val k = maxDim.toFloat() / longest
        return Bitmap.createScaledBitmap(bmp, (bmp.width * k).toInt().coerceAtLeast(1), (bmp.height * k).toInt().coerceAtLeast(1), true)
    }

    companion object {
        fun toJpeg(bmp: Bitmap, quality: Int = 90): ByteArray {
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
            return out.toByteArray()
        }
    }
}
