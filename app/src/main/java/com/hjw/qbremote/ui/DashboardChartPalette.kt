package com.hjw.qbremote.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class PieLegendEntry(
    val label: String,
    val value: Long,
    val valueText: String,
)

val DashboardPiePalette = listOf(
    Color(0xFF4C8DFF),
    Color(0xFF33BC84),
    Color(0xFFF3A53C),
    Color(0xFFA77AF2),
    Color(0xFFEF6D5E),
    Color(0xFF19B1C3),
    Color(0xFF8F9FB7),
    Color(0xFFFFCF5C),
)
