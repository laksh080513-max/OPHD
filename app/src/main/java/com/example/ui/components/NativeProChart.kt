package com.example.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CandlePoint
import com.example.data.model.getCurrencySymbol
import com.example.ui.theme.*
import java.util.Date
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalTextApi::class)
@Composable
fun NativeProChart(
    symbol: String,
    timeframe: String,
    candles: List<CandlePoint>,
    showSMA: Boolean,
    showEMA: Boolean,
    showRSI: Boolean,
    showUTBot: Boolean,
    countdownText: String,
    averageEntryPrice: Double? = null,
    smaPeriod: Int = 20,
    emaPeriod: Int = 50,
    rsiPeriod: Int = 14,
    utBotSensitivity: Float = 2.0f,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val isDark = isDarkThemeEnabled.value
    
    // Zoom & Pan state
    var visibleCandleCount by remember { mutableStateOf(40) }
    var horizontalOffset by remember { mutableStateOf(0) }
    var horizontalPanPx by remember { mutableStateOf(0f) }
    
    // Y-Axis Zoom & Pan state
    var yZoomFactor by remember { mutableStateOf(1f) }
    var yPanOffset by remember { mutableStateOf(0f) }
    
    var isDraggingPriceBar by remember { mutableStateOf(false) }
    var isPanningChart by remember { mutableStateOf(false) }
    
    // Crosshair state (null if not dragging or long-pressing)
    var dragOffset by remember { mutableStateOf<Offset?>(null) }
    
    // Helper to clear crosshair on tap
    val resetCrosshair = { dragOffset = null }
 
    val currencySymbol = getCurrencySymbol(symbol)
 
    Box(
        modifier = modifier
            .background(BgMidnight)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { dragOffset = null },
                    onLongPress = { offset -> dragOffset = offset }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val priceSidebarWidthPx = 65.dp.toPx()
                    if (centroid.x >= size.width - priceSidebarWidthPx) {
                        if (zoom != 1f) yZoomFactor = (yZoomFactor * zoom).coerceIn(0.1f, 10f)
                        if (pan.y != 0f) yPanOffset += pan.y
                    } else {
                        if (zoom != 1f) {
                            val zoomChange = if (zoom > 1.02f) -1 else if (zoom < 0.98f) 1 else 0
                            if (zoomChange != 0) visibleCandleCount = (visibleCandleCount + zoomChange).coerceIn(15, 100)
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val priceSidebarWidthPx = 65.dp.toPx()
                        if (offset.x >= size.width - priceSidebarWidthPx) {
                            isDraggingPriceBar = true
                            isPanningChart = false
                            dragOffset = null
                        } else {
                            isDraggingPriceBar = false
                            if (dragOffset != null) {
                                dragOffset = offset
                                isPanningChart = false
                            } else {
                                isPanningChart = true
                                horizontalPanPx = 0f
                            }
                        }
                    },
                    onDragEnd = {
                        isDraggingPriceBar = false
                        isPanningChart = false
                        if (dragOffset != null && !isPanningChart) dragOffset = null
                    },
                    onDragCancel = {
                        isDraggingPriceBar = false
                        isPanningChart = false
                        if (dragOffset != null && !isPanningChart) dragOffset = null
                    },
                    onDrag = { change, dragAmount ->
                        if (isDraggingPriceBar) {
                            val zoomChange = dragAmount.y * 0.005f
                            yZoomFactor = (yZoomFactor - zoomChange).coerceIn(0.1f, 10f)
                            yPanOffset += dragAmount.y * 0.5f
                        } else if (isPanningChart) {
                            val candleWidth = (size.width - 65.dp.toPx()) / visibleCandleCount
                            horizontalPanPx += dragAmount.x
                            yPanOffset += dragAmount.y
                            
                            if (horizontalPanPx > candleWidth) {
                                val shift = (horizontalPanPx / candleWidth).toInt()
                                horizontalOffset = (horizontalOffset + shift).coerceAtMost(max(0, candles.size - visibleCandleCount))
                                horizontalPanPx -= shift * candleWidth
                            } else if (horizontalPanPx < -candleWidth) {
                                val shift = (-horizontalPanPx / candleWidth).toInt()
                                horizontalOffset = (horizontalOffset - shift).coerceAtLeast(0)
                                horizontalPanPx += shift * candleWidth
                            }
                        } else if (dragOffset != null) {
                            dragOffset = change.position
                        }
                    }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (candles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(color = AccentOrange)
                        Text("Constructing ultra-low latency real-time candles...", color = TextMuted, fontSize = 13.sp)
                    }
                }
            } else {
                // Main Chart Viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Filter the most recent N candles to show on screen
                    val displayCandles = remember(candles, visibleCandleCount, horizontalOffset) {
                        val startIndex = max(0, candles.size - visibleCandleCount - horizontalOffset)
                        val endIndex = min(candles.size, startIndex + visibleCandleCount)
                        if (startIndex < endIndex) candles.subList(startIndex, endIndex) else emptyList()
                    }
                    
                    // Compute Technical Indicator Lines for the display subset
                    val sma20 = remember(candles, showSMA, smaPeriod) {
                        if (showSMA) calculateSMA(candles, smaPeriod) else emptyList()
                    }
                    val ema50 = remember(candles, showEMA, emaPeriod) {
                        if (showEMA) calculateEMA(candles, emaPeriod) else emptyList()
                    }
                    val rsi14 = remember(candles, showRSI, rsiPeriod) {
                        if (showRSI) calculateRSI(candles, rsiPeriod) else emptyList()
                    }
                    
                    // Dynamic UT Bot EMA period based on sensitivity
                    val utBotEmaPeriod = remember(utBotSensitivity) {
                        max(5, (50 / (utBotSensitivity / 2f)).toInt())
                    }
                    val utBotEma = remember(candles, utBotEmaPeriod) {
                        calculateEMA(candles, utBotEmaPeriod)
                    }

                    // Render dynamic canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        
                        // Right-hand sidebar space for price labels (e.g. 60dp)
                        val priceSidebarWidth = 65.dp.toPx()
                        // Bottom space for time labels (e.g. 25dp)
                        val timeAxisHeight = 22.dp.toPx()
                        
                        // Active drawing area dimensions
                        val chartWidth = width - priceSidebarWidth
                        
                        // Calculate RSI height ratio if enabled
                        val rsiSectionHeight = if (showRSI) height * 0.22f else 0f
                        val mainChartHeight = height - timeAxisHeight - rsiSectionHeight
                        
                        // Find min and max price for scaling the main viewport
                        val startIndex = max(0, candles.size - visibleCandleCount - horizontalOffset)
                        val endIndex = min(candles.size, startIndex + visibleCandleCount)
                        val activeSubList = if (startIndex < endIndex) candles.subList(startIndex, endIndex) else emptyList()
                        
                        var maxPrice = activeSubList.maxOfOrNull { it.high } ?: 100.0
                        var minPrice = activeSubList.minOfOrNull { it.low } ?: 0.0
                        
                        // Include SMA / EMA in boundary calculations if they are visible
                        if (showSMA && sma20.isNotEmpty()) {
                            val activeSma = sma20.subList(min(sma20.size, startIndex), min(sma20.size, endIndex))
                            if (activeSma.isNotEmpty()) {
                                maxPrice = max(maxPrice, activeSma.maxOrNull() ?: maxPrice)
                                minPrice = min(minPrice, activeSma.minOrNull() ?: minPrice)
                            }
                        }
                        if (showEMA && ema50.isNotEmpty()) {
                            val activeEma = ema50.subList(min(ema50.size, startIndex), min(ema50.size, endIndex))
                            if (activeEma.isNotEmpty()) {
                                maxPrice = max(maxPrice, activeEma.maxOrNull() ?: maxPrice)
                                minPrice = min(minPrice, activeEma.minOrNull() ?: minPrice)
                            }
                        }
                        
                        // Padding to ensure candles don't touch extreme top/bottom edges
                        val priceRange = maxPrice - minPrice
                        val paddingRatio = 0.08
                        val baseMaxPrice = maxPrice + (priceRange * paddingRatio)
                        val baseMinPrice = max(0.0, minPrice - (priceRange * paddingRatio))
                        
                        // Apply Y Zoom and Y Pan
                        val centerPrice = (baseMaxPrice + baseMinPrice) / 2.0
                        val halfRange = (baseMaxPrice - baseMinPrice) / 2.0
                        val zoomedHalfRange = halfRange / yZoomFactor
                        
                        val pricePerPixel = if (mainChartHeight > 0) (zoomedHalfRange * 2) / mainChartHeight else 0.0
                        val panPriceShift = yPanOffset * pricePerPixel
                        
                        val paddedMaxPrice = centerPrice + zoomedHalfRange + panPriceShift
                        val paddedMinPrice = max(0.0, centerPrice - zoomedHalfRange + panPriceShift)
                        val finalPriceRange = paddedMaxPrice - paddedMinPrice
                        
                        // Helper scaling functions
                        fun getPriceY(price: Double): Float {
                            return (mainChartHeight - ((price - paddedMinPrice) / finalPriceRange) * mainChartHeight).toFloat()
                        }
                        
                        val numCandles = displayCandles.size
                        val colWidth = chartWidth / numCandles
                        val candleWidth = colWidth * 0.72f
                        
                        // --- 0. Draw Background Watermark ---
                        val watermarkText = "$symbol, ${
                            when (timeframe) {
                                "5S" -> "5s"
                                "15S" -> "15s"
                                "30S" -> "30s"
                                "1M" -> "1m"
                                "5M" -> "5m"
                                "15M" -> "15m"
                                "1H" -> "1h"
                                "4H" -> "4h"
                                "1D" -> "1d"
                                "1W" -> "1w"
                                else -> timeframe
                            }
                        }"
                        val watermarkStyle = TextStyle(
                            color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        val wmLayout = textMeasurer.measure(watermarkText, watermarkStyle)
                        drawText(
                            textMeasurer = textMeasurer,
                            text = watermarkText,
                            topLeft = Offset(
                                (chartWidth - wmLayout.size.width) / 2f,
                                (mainChartHeight - wmLayout.size.height) / 2f
                            ),
                            style = watermarkStyle
                        )
                        
                        // --- 1. Draw Gridlines ---
                        val gridPaintColor = if (isDark) Color(0xFF1B233A) else Color(0xFFE5E9F0)
                        
                        // Horizontal Gridlines
                        val numGridlines = 5
                        for (i in 0..numGridlines) {
                            val ratio = i.toFloat() / numGridlines
                            val y = ratio * mainChartHeight
                            drawLine(
                                color = gridPaintColor,
                                start = Offset(0f, y),
                                end = Offset(chartWidth, y),
                                strokeWidth = 1f
                            )
                            
                            // Draw Price Label on right axis
                            val gridPrice = paddedMaxPrice - ratio * finalPriceRange
                            val labelText = String.format("%,.2f", gridPrice)
                            drawText(
                                textMeasurer = textMeasurer,
                                text = labelText,
                                topLeft = Offset(chartWidth + 6.dp.toPx(), y - 8.dp.toPx()),
                                style = TextStyle(
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                        
                        // Vertical Gridlines & Time Labels
                        val step = max(1, numCandles / 5)
                        for (i in displayCandles.indices step step) {
                            val x = i * colWidth + colWidth / 2f
                            drawLine(
                                color = gridPaintColor,
                                start = Offset(x, 0f),
                                end = Offset(x, mainChartHeight),
                                strokeWidth = 1f
                            )
                            
                            // Draw Time label
                            val candle = displayCandles[i]
                            val timeStr = DateFormat.format("HH:mm:ss", Date(candle.timestamp)).toString()
                            drawText(
                                textMeasurer = textMeasurer,
                                text = timeStr,
                                topLeft = Offset(x - 20.dp.toPx(), mainChartHeight + 4.dp.toPx()),
                                style = TextStyle(
                                    color = TextMuted,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        
                        // --- 2. Draw Candlesticks & UT Bot Alerts ---
                        displayCandles.forEachIndexed { index, candle ->
                            val x = index * colWidth + colWidth / 2f
                            val openY = getPriceY(candle.open)
                            val closeY = getPriceY(candle.close)
                            val highY = getPriceY(candle.high)
                            val lowY = getPriceY(candle.low)
                            
                            val isBullish = candle.close >= candle.open
                            val candleColor = if (isBullish) BullGreen else BearRed
                            
                            // Draw wick
                            drawLine(
                                color = candleColor,
                                start = Offset(x, highY),
                                end = Offset(x, lowY),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            
                            // Draw body
                            val top = min(openY, closeY)
                            val bottom = max(openY, closeY)
                            val bodyHeight = max(1f, bottom - top)
                            drawRect(
                                color = candleColor,
                                topLeft = Offset(x - candleWidth / 2f, top),
                                size = Size(candleWidth, bodyHeight)
                            )

                            // UT Bot / SuperTrend indicators overlay
                            if (showUTBot) {
                                val globalIdx = startIndex + index
                                if (globalIdx > 0 && globalIdx < utBotEma.size) {
                                    val currentEma = utBotEma[globalIdx]
                                    val prevEma = utBotEma[globalIdx - 1]
                                    val prevCandle = candles[globalIdx - 1]
                                    
                                    // Signals generated based on price crossing the dynamic UT Bot EMA
                                    val isCrossOver = candle.close > currentEma && prevCandle.close <= prevEma
                                    val isCrossUnder = candle.close < currentEma && prevCandle.close >= prevEma
                                    
                                    if (isCrossOver) {
                                        // Draw beautiful filled BUY green rounded box (similar to TradingView UT Bot Alerts)
                                        val boxWidth = 32.dp.toPx()
                                        val boxHeight = 16.dp.toPx()
                                        val boxX = x - boxWidth / 2f
                                        val boxY = lowY + 12.dp.toPx()
                                        
                                        drawRoundRect(
                                            color = BullGreen,
                                            topLeft = Offset(boxX, boxY),
                                            size = Size(boxWidth, boxHeight),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                        )
                                        
                                        drawText(
                                            textMeasurer = textMeasurer,
                                            text = "BUY",
                                            topLeft = Offset(x - 9.dp.toPx(), boxY + 2.dp.toPx()),
                                            style = TextStyle(color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                                        )
                                    } else if (isCrossUnder) {
                                        // Draw beautiful filled SELL red rounded box
                                        val boxWidth = 32.dp.toPx()
                                        val boxHeight = 16.dp.toPx()
                                        val boxX = x - boxWidth / 2f
                                        val boxY = highY - 28.dp.toPx()
                                        
                                        drawRoundRect(
                                            color = BearRed,
                                            topLeft = Offset(boxX, boxY),
                                            size = Size(boxWidth, boxHeight),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                        )
                                        
                                        drawText(
                                            textMeasurer = textMeasurer,
                                            text = "SELL",
                                            topLeft = Offset(x - 10.dp.toPx(), boxY + 2.dp.toPx()),
                                            style = TextStyle(color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // --- 3. Draw Moving Average Overlays ---
                        if (showSMA && sma20.isNotEmpty()) {
                            val path = Path()
                            var isFirst = true
                            displayCandles.forEachIndexed { index, _ ->
                                val globalIdx = startIndex + index
                                if (globalIdx < sma20.size) {
                                    val smaVal = sma20[globalIdx]
                                    val x = index * colWidth + colWidth / 2f
                                    val y = getPriceY(smaVal)
                                    if (isFirst) {
                                        path.moveTo(x, y)
                                        isFirst = false
                                    } else {
                                        path.lineTo(x, y)
                                    }
                                }
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFF2962FF),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                        
                        if (showEMA && ema50.isNotEmpty()) {
                            val path = Path()
                            var isFirst = true
                            displayCandles.forEachIndexed { index, _ ->
                                val globalIdx = startIndex + index
                                if (globalIdx < ema50.size) {
                                    val emaVal = ema50[globalIdx]
                                    val x = index * colWidth + colWidth / 2f
                                    val y = getPriceY(emaVal)
                                    if (isFirst) {
                                        path.moveTo(x, y)
                                        isFirst = false
                                    } else {
                                        path.lineTo(x, y)
                                    }
                                }
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFFE040FB),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                        
                        // --- 4. Draw Average Entry Price Indicator Line ---
                        averageEntryPrice?.let { entryPrice ->
                            if (entryPrice in paddedMinPrice..paddedMaxPrice) {
                                val entryY = getPriceY(entryPrice)
                                drawLine(
                                    color = Color(0xFF00E676),
                                    start = Offset(0f, entryY),
                                    end = Offset(chartWidth, entryY),
                                    strokeWidth = 1.2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = "Avg Buy: $currencySymbol${String.format("%,.2f", entryPrice)}",
                                    topLeft = Offset(10.dp.toPx(), entryY - 14.dp.toPx()),
                                    style = TextStyle(
                                        color = Color(0xFF00E676),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        background = Color.Black.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }
                        
                        // --- 5. Draw RSI panel (Secondary bottom pane) ---
                        if (showRSI && rsiSectionHeight > 0) {
                            val rsiStartY = height - timeAxisHeight - rsiSectionHeight
                            
                            // RSI box background
                            drawRect(
                                color = if (isDark) Color(0xFF080C14) else Color(0xFFF0F4F8),
                                topLeft = Offset(0f, rsiStartY),
                                size = Size(chartWidth, rsiSectionHeight)
                            )
                            
                            // Horizontal references at RSI 30, 50, 70
                            val rsiLevels = listOf(30f, 50f, 70f)
                            rsiLevels.forEach { lvl ->
                                val lvlRatio = (lvl / 100f)
                                val y = rsiStartY + rsiSectionHeight * (1f - lvlRatio)
                                
                                drawLine(
                                    color = if (lvl == 50f) Color.Gray.copy(alpha = 0.25f) else Color(0xFF3949AB).copy(alpha = 0.4f),
                                    start = Offset(0f, y),
                                    end = Offset(chartWidth, y),
                                    strokeWidth = 1f,
                                    pathEffect = if (lvl != 50f) PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f) else null
                                )
                                
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = lvl.toInt().toString(),
                                    topLeft = Offset(chartWidth + 6.dp.toPx(), y - 7.dp.toPx()),
                                    style = TextStyle(
                                        color = TextMuted,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            
                            // Plot RSI 14 Line
                            if (rsi14.isNotEmpty()) {
                                val path = Path()
                                var isFirst = true
                                displayCandles.forEachIndexed { index, _ ->
                                    val globalIdx = startIndex + index
                                    if (globalIdx < rsi14.size) {
                                        val rsiVal = rsi14[globalIdx]
                                        val x = index * colWidth + colWidth / 2f
                                        val ratio = (rsiVal / 100.0).toFloat().coerceIn(0f, 1f)
                                        val y = rsiStartY + rsiSectionHeight * (1f - ratio)
                                        
                                        if (isFirst) {
                                            path.moveTo(x, y)
                                            isFirst = false
                                        } else {
                                            path.lineTo(x, y)
                                        }
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Color(0xFFFDD835), // Electric yellow RSI
                                    style = Stroke(width = 1.2f.dp.toPx())
                                )
                            }
                            
                            // RSI Title Label
                            drawText(
                                textMeasurer = textMeasurer,
                                text = "RSI (14)",
                                topLeft = Offset(10.dp.toPx(), rsiStartY + 4.dp.toPx()),
                                style = TextStyle(
                                    color = Color(0xFFFDD835),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        
                        // --- 6. Draw Crosshair / Interactive HUD ---
                        dragOffset?.let { offset ->
                            val touchX = offset.x
                            if (touchX in 0f..chartWidth) {
                                val hoveredCandleIdx = (touchX / colWidth).toInt().coerceIn(0, numCandles - 1)
                                val candle = displayCandles[hoveredCandleIdx]
                                
                                val x = hoveredCandleIdx * colWidth + colWidth / 2f
                                
                                // Follow the finger's exact vertical touch position
                                val touchY = offset.y.coerceIn(0f, mainChartHeight)
                                // Calculate the precise price at the exact touch height
                                val touchPrice = paddedMaxPrice - (touchY / mainChartHeight) * finalPriceRange
                                
                                // Vertical line
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.5f),
                                    start = Offset(x, 0f),
                                    end = Offset(x, height - timeAxisHeight),
                                    strokeWidth = 1f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                )
                                
                                // Horizontal line
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.5f),
                                    start = Offset(0f, touchY),
                                    end = Offset(chartWidth, touchY),
                                    strokeWidth = 1f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                )
                                
                                // Draw high-contrast text metrics overlay inside a nice popup following the finger position
                                val detailStr = "O: ${String.format("%.2f", candle.open)}  H: ${String.format("%.2f", candle.high)}  L: ${String.format("%.2f", candle.low)}  C: ${String.format("%.2f", candle.close)}"
                                val textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val textLayoutResult = textMeasurer.measure(
                                    text = detailStr,
                                    style = textStyle
                                )
                                val textWidth = textLayoutResult.size.width.toFloat()
                                val textHeight = textLayoutResult.size.height.toFloat()
                                
                                val padX = 10.dp.toPx()
                                val padY = 6.dp.toPx()
                                val boxWidth = textWidth + padX * 2f
                                val boxHeight = textHeight + padY * 2f
                                
                                // Position the box slightly above the crosshair touch coordinate
                                var boxX = x - boxWidth / 2f
                                var boxY = touchY - boxHeight - 16.dp.toPx()
                                
                                // Keep within screen boundary safety checks
                                if (boxX < 6.dp.toPx()) {
                                    boxX = 6.dp.toPx()
                                } else if (boxX + boxWidth > chartWidth - 6.dp.toPx()) {
                                    boxX = chartWidth - boxWidth - 6.dp.toPx()
                                }
                                
                                if (boxY < 6.dp.toPx()) {
                                    // If too high, display below the crosshair coordinate instead
                                    boxY = touchY + 16.dp.toPx()
                                }
                                
                                // Draw a modern tooltip rounded rectangle background
                                drawRoundRect(
                                    color = Color(0xFF0F1626), // Deep midnight blue
                                    topLeft = Offset(boxX, boxY),
                                    size = Size(boxWidth, boxHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )
                                // Draw a thin orange border for the tooltip popup
                                drawRoundRect(
                                    color = AccentOrange.copy(alpha = 0.85f),
                                    topLeft = Offset(boxX, boxY),
                                    size = Size(boxWidth, boxHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                                
                                // Draw the text centered inside the popup
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = detailStr,
                                    topLeft = Offset(boxX + padX, boxY + padY),
                                    style = textStyle
                                )
                                
                                // Right-side price tag showing the precise price at crosshair point
                                val tagPriceText = String.format("%,.2f", touchPrice)
                                drawRect(
                                    color = AccentOrange,
                                    topLeft = Offset(chartWidth, touchY - 8.dp.toPx()),
                                    size = Size(priceSidebarWidth, 16.dp.toPx())
                                )
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = tagPriceText,
                                    topLeft = Offset(chartWidth + 4.dp.toPx(), touchY - 7.dp.toPx()),
                                    style = TextStyle(
                                        color = Color.Black,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                    
                    // --- 7. Top right floating HUD showing countdown timer ---
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.85f)),
                        border = BorderStroke(1.dp, BorderNavy),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(AccentOrange, RoundedCornerShape(50))
                            )
                            Text(
                                text = "Next candle: $countdownText",
                                color = TextWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
