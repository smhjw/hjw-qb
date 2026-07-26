package com.hjw.qbremote.ui

import android.content.Context
import android.util.Xml
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import com.hjw.qbremote.data.model.CountryUploadRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import kotlin.math.min

private data class MapShape(
    val regionCode: String,
    val path: Path,
)

private data class ParsedWorldMap(
    val minX: Float,
    val minY: Float,
    val width: Float,
    val height: Float,
    val shapes: List<MapShape>,
)

// Parsing the 55KB SVG (hundreds of PathParser paths) takes long enough to drop
// frames; remember{} alone re-parsed it every time the page was rebuilt (server
// switch, theme change). Parse once per process, off the main thread.
private object WorldMapAssetCache {
    @Volatile
    private var cached: ParsedWorldMap? = null

    fun peek(): ParsedWorldMap? = cached

    fun load(context: Context): ParsedWorldMap {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val parsed = context.assets.open("world-map.min.svg").bufferedReader().use { reader ->
                parseWorldMapSvg(reader.readText())
            }
            cached = parsed
            return parsed
        }
    }
}

@Composable
fun WorldMapChart(
    countries: List<CountryUploadRecord>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    var parsedMapOrNull by remember { mutableStateOf(WorldMapAssetCache.peek()) }
    if (parsedMapOrNull == null) {
        LaunchedEffect(Unit) {
            parsedMapOrNull = withContext(Dispatchers.Default) {
                WorldMapAssetCache.load(context)
            }
        }
    }
    val merged = remember(countries) { mergeCountryUploadRecordsForDisplay(countries) }
    val maxUploaded = merged.maxOfOrNull { it.uploadedBytes }?.coerceAtLeast(1L) ?: 1L
    val colorScheme = MaterialTheme.colorScheme
    val isLightTheme = colorScheme.background.luminance() > 0.5f
    val defaultFill = if (isLightTheme) {
        Color(0xFFD3DCE6)
    } else {
        Color(0xFF2B3948)
    }
    val strokeColor = if (isLightTheme) {
        Color(0xFF7A8796)
    } else {
        Color(0xFF6F8194)
    }
    val countryColors = remember(merged, maxUploaded, isLightTheme) {
        buildMap {
            merged.forEach { record ->
                val code = normalizeCountryCodeForDisplay(record.countryCode)
                if (code.isBlank()) return@forEach
                val progress = (record.uploadedBytes.toFloat() / maxUploaded.toFloat()).coerceIn(0f, 1f)
                val color = heatColor(
                    progress = 0.22f + (progress * 0.78f),
                    isLightTheme = isLightTheme,
                )
                put(code, color)
                if (code == "CN") put("TW", color)
            }
        }
    }

    val parsedMap = parsedMapOrNull
    if (parsedMap == null) {
        // Keep the card's footprint stable while the map parses off-thread.
        Canvas(modifier = modifier) {}
        return
    }

    Canvas(modifier = modifier) {
        val scale = min(size.width / parsedMap.width, size.height / parsedMap.height)
        val contentWidth = parsedMap.width * scale
        val contentHeight = parsedMap.height * scale
        val offsetX = (size.width - contentWidth) / 2f
        val offsetY = (size.height - contentHeight) / 2f
        val strokeWidth = (0.55f / scale).coerceAtLeast(0.15f)

        parsedMap.shapes.forEach { shape ->
            val fill = countryColors[shape.regionCode] ?: defaultFill
            withTransform({
                translate(left = offsetX, top = offsetY)
                scale(scaleX = scale, scaleY = scale)
                translate(left = -parsedMap.minX, top = -parsedMap.minY)
            }) {
                drawPath(
                    path = shape.path,
                    color = fill,
                )
                drawPath(
                    path = shape.path,
                    color = strokeColor,
                    style = Stroke(width = strokeWidth),
                )
            }
        }
    }
}

private fun parseWorldMapSvg(svg: String): ParsedWorldMap {
    val parser = Xml.newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    parser.setInput(StringReader(svg))

    var currentGroupId: String? = null
    val shapes = mutableListOf<MapShape>()

    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
        when (parser.eventType) {
            XmlPullParser.START_TAG -> {
                when (parser.name) {
                    "svg" -> {
                        Unit
                    }

                    "g" -> {
                        currentGroupId = parser.getAttributeValue(null, "id")
                            ?.trim()
                            ?.uppercase()
                            ?.takeIf { it.isNotBlank() }
                    }

                    "path" -> {
                        val rawId = parser.getAttributeValue(null, "id")
                            ?.trim()
                            ?.uppercase()
                            ?.takeIf { it.isNotBlank() }
                            ?: currentGroupId
                            ?: ""
                        val regionCode = rawId.removePrefix("_")
                        val pathData = parser.getAttributeValue(null, "d").orEmpty()
                        if (regionCode.isNotBlank() && pathData.isNotBlank()) {
                            val path = PathParser().parsePathString(pathData).toPath()
                            shapes += MapShape(regionCode = regionCode, path = path)
                        }
                    }
                }
            }

            XmlPullParser.END_TAG -> {
                if (parser.name == "g") currentGroupId = null
            }
        }
        parser.next()
    }

    var actualMinX = Float.POSITIVE_INFINITY
    var actualMinY = Float.POSITIVE_INFINITY
    var actualMaxX = Float.NEGATIVE_INFINITY
    var actualMaxY = Float.NEGATIVE_INFINITY

    shapes.forEach { shape ->
        val bounds = shape.path.getBounds()
        if (bounds.left < actualMinX) actualMinX = bounds.left
        if (bounds.top < actualMinY) actualMinY = bounds.top
        if (bounds.right > actualMaxX) actualMaxX = bounds.right
        if (bounds.bottom > actualMaxY) actualMaxY = bounds.bottom
    }

    if (!actualMinX.isFinite() || !actualMinY.isFinite() || !actualMaxX.isFinite() || !actualMaxY.isFinite()) {
        actualMinX = 0f
        actualMinY = 0f
        actualMaxX = 1f
        actualMaxY = 1f
    }

    return ParsedWorldMap(
        minX = actualMinX,
        minY = actualMinY,
        width = (actualMaxX - actualMinX).coerceAtLeast(1f),
        height = (actualMaxY - actualMinY).coerceAtLeast(1f),
        shapes = shapes,
    )
}

private fun heatColor(progress: Float, isLightTheme: Boolean): Color {
    val clamped = progress.coerceIn(0f, 1f)
    val low = if (isLightTheme) Color(0xFFA8C7F0) else Color(0xFF3D5D82)
    val high = if (isLightTheme) Color(0xFF1E63BF) else Color(0xFF65A7FF)
    return Color(
        red = lerpChannel(low.red, high.red, clamped),
        green = lerpChannel(low.green, high.green, clamped),
        blue = lerpChannel(low.blue, high.blue, clamped),
        alpha = 1f,
    )
}

private fun lerpChannel(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}
