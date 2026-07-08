package com.example.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

val isDarkThemeEnabled = mutableStateOf(true)
val isProUserEnabled = mutableStateOf(false)

val BgMidnight: Color get() = if (isDarkThemeEnabled.value) Color(0xFF121212) else Color(0xFFF0F2F5)
val SurfaceDark: Color get() = if (isDarkThemeEnabled.value) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
val SurfaceCard: Color get() = if (isDarkThemeEnabled.value) Color(0xFF262626) else Color(0xFFFAFAFA)
val BullGreen: Color get() = Color(0xFF00D09C)
val BearRed: Color get() = Color(0xFFEB5B3C)
val AccentOrange: Color get() = Color(0xFF00D09C) // Groww Green
val ChartGridLine: Color get() = if (isDarkThemeEnabled.value) Color(0xFF333333) else Color(0xFFE0E0E0)
val TextWhite: Color get() = if (isDarkThemeEnabled.value) Color(0xFFF1F1F1) else Color(0xFF1E1E1E)
val TextMuted: Color get() = if (isDarkThemeEnabled.value) Color(0xFFA0A0A0) else Color(0xFF757575)
val BorderNavy: Color get() = if (isDarkThemeEnabled.value) Color(0xFF333333) else Color(0xFFE0E0E0)
