package com.hjw.qbremote.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.ColorUtils
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class SavedCustomBackground(
    val filePath: String,
    val toneIsLight: Boolean,
)

internal data class CustomBackgroundImageState(
    val image: ImageBitmap? = null,
    val isReady: Boolean = true,
)

private const val CUSTOM_THEME_DIR = "theme"
private const val CUSTOM_THEME_FILE_PREFIX = "custom_background"
private const val CUSTOM_THEME_ANALYSIS_SIZE = 96
private const val CUSTOM_THEME_LIGHT_THRESHOLD = 0.58

internal fun saveCustomBackgroundSelection(
    context: Context,
    sourceUri: Uri,
): SavedCustomBackground {
    val targetDirectory = File(context.filesDir, CUSTOM_THEME_DIR).apply { mkdirs() }
    val extension = resolveCustomBackgroundExtension(context, sourceUri)
    val targetFile = File(
        targetDirectory,
        "${CUSTOM_THEME_FILE_PREFIX}_${System.currentTimeMillis()}$extension",
    )

    targetDirectory.listFiles()
        .orEmpty()
        .filter { candidate ->
            candidate.isFile &&
                candidate.name.startsWith(CUSTOM_THEME_FILE_PREFIX) &&
                candidate.absolutePath != targetFile.absolutePath
        }
        .forEach { it.delete() }

    context.contentResolver.openInputStream(sourceUri)?.use { input ->
        targetFile.outputStream().use { output ->
            input.copyTo(output)
        }
    } ?: error("Unable to read the selected image.")

    val toneIsLight = analyzeImageTone(targetFile)
    return SavedCustomBackground(
        filePath = targetFile.absolutePath,
        toneIsLight = toneIsLight,
    )
}

internal fun isCustomBackgroundFileValid(path: String): Boolean {
    val trimmed = path.trim()
    if (trimmed.isBlank()) return false
    val file = File(trimmed)
    if (!file.isFile || file.length() <= 0L) return false
    return decodeSampledBitmapFromFile(
        filePath = trimmed,
        maxDimension = 64,
    )?.let { bitmap ->
        bitmap.recycle()
        true
    } ?: false
}

@Composable
internal fun rememberCustomBackgroundImageState(
    path: String,
    targetMaxDimensionPx: Int,
): CustomBackgroundImageState {
    val normalizedTarget = targetMaxDimensionPx.coerceAtLeast(1)
    val state by produceState(
        initialValue = CustomBackgroundImageState(),
        key1 = path,
        key2 = normalizedTarget,
    ) {
        val trimmed = path.trim()
        if (trimmed.isBlank()) {
            value = CustomBackgroundImageState(image = null, isReady = true)
            return@produceState
        }

        value = CustomBackgroundImageState(image = null, isReady = false)
        val image = withContext(Dispatchers.IO) {
            decodeSampledBitmapFromFile(
                filePath = trimmed,
                maxDimension = normalizedTarget,
            )?.asImageBitmap()
        }
        value = CustomBackgroundImageState(
            image = image,
            isReady = true,
        )
    }
    return state
}

private fun resolveCustomBackgroundExtension(
    context: Context,
    sourceUri: Uri,
): String {
    val displayName = readDisplayName(context, sourceUri)
    val extensionFromName = displayName
        .substringAfterLast('.', "")
        .trim()
        .lowercase()
    if (extensionFromName.isNotBlank()) {
        return ".$extensionFromName"
    }

    return when (context.contentResolver.getType(sourceUri).orEmpty().lowercase()) {
        "image/png" -> ".png"
        "image/webp" -> ".webp"
        "image/gif" -> ".gif"
        else -> ".jpg"
    }
}

private fun analyzeImageTone(file: File): Boolean {
    val bitmap = decodeSampledBitmapFromFile(
        filePath = file.absolutePath,
        maxDimension = CUSTOM_THEME_ANALYSIS_SIZE,
    ) ?: return false

    return try {
        var totalLuminance = 0.0
        var sampleCount = 0
        val stepX = max(1, bitmap.width / 12)
        val stepY = max(1, bitmap.height / 12)
        for (y in 0 until bitmap.height step stepY) {
            for (x in 0 until bitmap.width step stepX) {
                totalLuminance += ColorUtils.calculateLuminance(bitmap.getPixel(x, y))
                sampleCount += 1
            }
        }
        sampleCount > 0 &&
            (totalLuminance / sampleCount.toDouble()) >= CUSTOM_THEME_LIGHT_THRESHOLD
    } finally {
        bitmap.recycle()
    }
}

private fun decodeSampledBitmapFromFile(
    filePath: String,
    maxDimension: Int,
): Bitmap? {
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(filePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds, maxDimension)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeFile(filePath, options)
}

private fun calculateInSampleSize(
    bounds: BitmapFactory.Options,
    maxDimension: Int,
): Int {
    var sampleSize = 1
    while (
        bounds.outWidth / sampleSize > maxDimension ||
            bounds.outHeight / sampleSize > maxDimension
    ) {
        sampleSize *= 2
    }
    return sampleSize.coerceAtLeast(1)
}
