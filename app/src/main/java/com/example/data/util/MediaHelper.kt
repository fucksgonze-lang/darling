package com.example.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

data class PreparedVideo(
    val playableUri: String,
    val thumbnailDataUri: String?,
    val durationMs: Long,
    val averageBrightness: Float
)

object MediaHelper {
    private const val MAX_DIMENSION = 720
    private const val MAX_BYTES = 140_000

    fun isVideoMimeType(context: Context, uri: Uri): Boolean {
        val mime = context.contentResolver.getType(uri).orEmpty().lowercase()
        if (mime.startsWith("video/")) return true
        val path = uri.toString().lowercase()
        return path.endsWith(".mp4") || path.endsWith(".mov") || path.endsWith(".mkv") || path.endsWith(".webm")
    }

    fun uriToCompactDataUri(context: Context, uri: Uri): String? {
        return try {
            val resolver = context.contentResolver
            val originalBytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val maxSide = max(bounds.outWidth, bounds.outHeight)
            var sampleSize = 1
            while (maxSide / sampleSize > MAX_DIMENSION * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size, decodeOpts)
                ?: return null

            bitmapToCompactDataUri(decoded)
        } catch (_: Exception) {
            null
        }
    }

    fun preparePickedVideo(context: Context, uri: Uri): PreparedVideo? {
        return try {
            val reelsDir = File(context.filesDir, "reels").apply { mkdirs() }
            val destFile = File(reelsDir, "reel_${System.currentTimeMillis()}.mp4")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            if (!destFile.exists() || destFile.length() == 0L) return null

            val retriever = MediaMetadataRetriever()
            var durationMs = 0L
            var frameBitmap: Bitmap? = null
            try {
                retriever.setDataSource(destFile.absolutePath)
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?.coerceAtLeast(0L)
                    ?: 0L
                val frameTimeUs = if (durationMs > 1000L) 500_000L else 0L
                frameBitmap = retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
            } finally {
                runCatching { retriever.release() }
            }

            val brightness = frameBitmap?.let { estimateBrightness(it) } ?: 0.5f
            val thumbnailUri = frameBitmap?.let { bitmapToCompactDataUri(it) }

            PreparedVideo(
                playableUri = Uri.fromFile(destFile).toString(),
                thumbnailDataUri = thumbnailUri,
                durationMs = durationMs,
                averageBrightness = brightness
            )
        } catch (_: Exception) {
            null
        }
    }

    fun estimateBrightnessFromDataUri(dataUri: String?): Float? {
        val bmp = decodeDataUriToBitmap(dataUri) ?: return null
        return estimateBrightness(bmp).also { bmp.recycle() }
    }

    private fun estimateBrightness(bitmap: Bitmap): Float {
        val w = bitmap.width.coerceAtLeast(1)
        val h = bitmap.height.coerceAtLeast(1)
        val stepX = (w / 10).coerceAtLeast(1)
        val stepY = (h / 10).coerceAtLeast(1)
        var totalLuma = 0f
        var count = 0
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val px = bitmap.getPixel(x, y)
                val r = Color.red(px) / 255f
                val g = Color.green(px) / 255f
                val b = Color.blue(px) / 255f
                totalLuma += 0.299f * r + 0.587f * g + 0.114f * b
                count++
                x += stepX
            }
            y += stepY
        }
        return if (count > 0) (totalLuma / count).coerceIn(0f, 1f) else 0.5f
    }

    private fun bitmapToCompactDataUri(decoded: Bitmap): String? {
        return try {
            val scale = minOf(1f, MAX_DIMENSION.toFloat() / max(decoded.width, decoded.height))
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).roundToInt().coerceAtLeast(1),
                    (decoded.height * scale).roundToInt().coerceAtLeast(1),
                    true
                ).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            val out = ByteArrayOutputStream()
            var quality = 80
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            while (out.size() > MAX_BYTES && quality > 30) {
                out.reset()
                quality -= 12
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            scaled.recycle()

            val encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$encoded"
        } catch (_: Exception) {
            null
        }
    }

    fun decodeDataUriToBitmap(dataUri: String?): Bitmap? {
        if (dataUri.isNullOrBlank() || !dataUri.startsWith("data:image")) return null
        return try {
            val commaIndex = dataUri.indexOf(',')
            if (commaIndex == -1) return null
            val base64Part = dataUri.substring(commaIndex + 1)
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun isPlayableVideoUri(context: Context, uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return try {
            val parsed = Uri.parse(uriString)
            when (parsed.scheme?.lowercase()) {
                "http", "https" -> true
                "file" -> parsed.path?.let { File(it).exists() } == true
                "content" -> context.contentResolver.openAssetFileDescriptor(parsed, "r")?.use { true } ?: false
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun formatDuration(durationMs: Long?): String {
        if (durationMs == null || durationMs <= 0L) return ""
        val totalSec = (durationMs / 1000L).toInt()
        val mins = totalSec / 60
        val secs = totalSec % 60
        return "%d:%02d".format(mins, secs)
    }
}
