package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun AnalyticsSection(
    modifier: Modifier = Modifier
) {
    var selectedSymbol by remember { mutableStateOf("BTCUSD") }
    val symbols = listOf("BTCUSD", "ETHUSD", "AAPL", "TSLA", "MSFT")
    
    // Hour simulation slider (9.0f representing 9:00 AM, up to 16.0f representing 4:00 PM)
    var simulatedHour by remember { mutableStateOf(10.5f) } // Default 10:30 AM
    
    val scrollState = rememberScrollState()

    // Mock analytical data generator for each symbol
    val analyticsData = remember(selectedSymbol) {
        when (selectedSymbol) {
            "BTCUSD" -> SymbolAnalytics(
                peakBuyTime = "10:00 AM - 12:00 PM & 08:30 PM - 10:30 PM",
                lessBuyTime = "04:00 AM - 06:00 AM & 01:00 PM - 02:30 PM",
                peakExplanation = "High institutional liquidity from US market open, followed by Asian retail evening rush.",
                lessExplanation = "Global lull as US traders exit and European session hasn't gained full momentum.",
                hourlyBuyPressure = listOf(65, 82, 75, 40, 35, 55, 60, 88), // 9 AM to 4 PM
                hourlySellPressure = listOf(35, 18, 25, 60, 65, 45, 40, 12)
            )
            "ETHUSD" -> SymbolAnalytics(
                peakBuyTime = "02:00 PM - 04:00 PM",
                lessBuyTime = "11:00 PM - 01:00 AM",
                peakExplanation = "DeFi and smart contract interactions peak coinciding with London-New York overlap.",
                lessExplanation = "Gas fees subside and activity tapers down during Asian midnight hours.",
                hourlyBuyPressure = listOf(55, 60, 78, 85, 50, 42, 60, 72),
                hourlySellPressure = listOf(45, 40, 22, 15, 50, 58, 40, 28)
            )
            "AAPL" -> SymbolAnalytics(
                peakBuyTime = "09:30 AM - 10:30 AM",
                lessBuyTime = "12:30 PM - 01:30 PM",
                peakExplanation = "Opening bell high volume blocks and earnings-related momentum trades.",
                lessExplanation = "Mid-day lunch hour consolidation with minimum retail flow.",
                hourlyBuyPressure = listOf(90, 72, 50, 30, 28, 45, 65, 80),
                hourlySellPressure = listOf(10, 28, 50, 70, 72, 55, 35, 20)
            )
            "TSLA" -> SymbolAnalytics(
                peakBuyTime = "03:00 PM - 04:00 PM",
                lessBuyTime = "11:30 AM - 12:30 PM",
                peakExplanation = "Power hour squeeze and high speculative option hedging before close.",
                lessExplanation = "European session exit causing temporary dry liquidity pool.",
                hourlyBuyPressure = listOf(70, 60, 40, 32, 50, 72, 85, 92),
                hourlySellPressure = listOf(30, 40, 60, 68, 50, 28, 15, 8)
            )
            else -> SymbolAnalytics(
                peakBuyTime = "11:00 AM - 12:00 PM",
                lessBuyTime = "02:00 PM - 03:00 PM",
                peakExplanation = "Enterprise allocation rebalancing and index funds routing execution.",
                lessExplanation = "Pre-closing volume drain before options expiration sweeps.",
                hourlyBuyPressure = listOf(60, 75, 85, 48, 38, 42, 58, 68),
                hourlySellPressure = listOf(40, 25, 15, 52, 62, 58, 42, 32)
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgMidnight)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = "Analytics",
                tint = AccentOrange,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = "Time-Volume Intelligence",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Statistical Peak Buying & Less Buying hours",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        // Horizontal Symbol Selector
        ScrollableTabRow(
            selectedTabIndex = symbols.indexOf(selectedSymbol),
            containerColor = Color.Transparent,
            contentColor = AccentOrange,
            edgePadding = 0.dp,
            divider = {}
        ) {
            symbols.forEachIndexed { index, sym ->
                Tab(
                    selected = selectedSymbol == sym,
                    onClick = { selectedSymbol = sym },
                    text = {
                        Text(
                            text = sym,
                            fontSize = 12.sp,
                            fontWeight = if (selectedSymbol == sym) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedSymbol == sym) AccentOrange else TextWhite
                        )
                    }
                )
            }
        }

        // Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "$selectedSymbol Historical Schedule",
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                // Peak Buying Time Card Block
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BullGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Peak Buying Time",
                        tint = BullGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PEAK BUYING TIME",
                            color = BullGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = analyticsData.peakBuyTime,
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = analyticsData.peakExplanation,
                            color = TextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Less Buying Time Card Block
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BearRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Less Buying Time",
                        tint = BearRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LESS BUYING / LOW VOL",
                            color = BearRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = analyticsData.lessBuyTime,
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = analyticsData.lessExplanation,
                            color = TextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Live Hour Simulation Slider (requested feature: "make a slider bar")
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Time Simulator",
                        tint = AccentOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Hour Intensity Simulator",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val formattedHour = remember(simulatedHour) {
                    val integerHour = simulatedHour.toInt()
                    val minutes = ((simulatedHour - integerHour) * 60).toInt()
                    val ampm = if (integerHour >= 12) "PM" else "AM"
                    val hr12 = if (integerHour > 12) integerHour - 12 else if (integerHour == 0) 12 else integerHour
                    String.format("%02d:%02d %s", hr12, minutes, ampm)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formattedHour,
                            color = AccentOrange,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        
                        // Calculate simulated buying pressure at that hour
                        // Hours from 9 AM (0) to 4 PM (7)
                        val index = ((simulatedHour - 9f).coerceIn(0f, 7f)).toInt()
                        val buyPct = analyticsData.hourlyBuyPressure[index]
                        val sellPct = analyticsData.hourlySellPressure[index]

                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Buy Power: $buyPct%", color = BullGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Sell Power: $sellPct%", color = BearRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Linear progress indicator representing buy/sell split
                        LinearProgressIndicator(
                            progress = { buyPct / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .padding(vertical = 4.dp),
                            color = BullGreen,
                            trackColor = BearRed,
                        )
                    }
                }

                Slider(
                    value = simulatedHour,
                    onValueChange = { simulatedHour = it },
                    valueRange = 9.0f..16.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentOrange,
                        activeTrackColor = AccentOrange,
                        inactiveTrackColor = Color.DarkGray
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("09:00 AM", color = TextMuted, fontSize = 10.sp)
                    Text("12:00 PM", color = TextMuted, fontSize = 10.sp)
                    Text("04:00 PM", color = TextMuted, fontSize = 10.sp)
                }
            }
        }

        // Hourly Trading Density Chart
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Hourly Buy vs Sell Volume Power",
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val hourLabels = listOf("9A", "10A", "11A", "12P", "1P", "2P", "3P", "4P")
                    
                    analyticsData.hourlyBuyPressure.forEachIndexed { i, buy ->
                        val sell = analyticsData.hourlySellPressure[i]
                        val total = buy + sell
                        val buyHeightFraction = buy.toFloat() / 100f
                        val sellHeightFraction = sell.toFloat() / 100f

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                // Stacked split bar representing buy (bottom) and sell (top) volume
                                Column(
                                    modifier = Modifier.fillMaxHeight(),
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    // Sell Bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(sellHeightFraction)
                                            .background(BearRed, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                    )
                                    // Buy Bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(buyHeightFraction)
                                            .background(BullGreen, RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = hourLabels[i],
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).background(BullGreen).padding(end = 4.dp))
                    Text(" Buy Intensity", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.size(8.dp).background(BearRed).padding(end = 4.dp))
                    Text(" Sell Intensity", color = TextMuted, fontSize = 11.sp)
                }
            }
        }

        // Pro AI Timing Report Block
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "👑 Premium Timing Intelligence",
                        color = AccentOrange,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isProUserEnabled.value) {
                    Text(
                        text = "Pro timing model activated successfully! Our quantitative analytics recommend establishing buy limits on $selectedSymbol at Tuesdays 10:15 AM EST for a statistically backed 1.8% intraday edge.",
                        color = TextWhite,
                        fontSize = 12.sp
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray)
                        Text(
                            text = "Timing models are locked. Enter promo code OPDH IS BEST6785 in Settings to unlock 1-Year FREE Pro Feature Access!",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

data class SymbolAnalytics(
    val peakBuyTime: String,
    val lessBuyTime: String,
    val peakExplanation: String,
    val lessExplanation: String,
    val hourlyBuyPressure: List<Int>,
    val hourlySellPressure: List<Int>
)
