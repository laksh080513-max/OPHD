package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CandlePoint
import com.example.data.model.DatabaseOrder
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.ui.theme.*
import com.example.ui.viewmodel.PositionWithValuation
import com.example.ui.viewmodel.WatchlistItemWithPrice
import kotlin.math.cos
import kotlin.math.sin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistSection(
    items: List<WatchlistItemWithPrice>,
    selectedSymbol: String,
    onSymbolSelect: (String) -> Unit,
    onDeleteSymbol: (String) -> Unit,
    onAddSymbol: (symbol: String, name: String, price: Double, type: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedAdd by remember { mutableStateOf(false) }

    var symInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var typeSelection by remember { mutableStateOf("Stock") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(items, searchQuery) {
        items.filter { 
            it.item.symbol.contains(searchQuery, ignoreCase = true) || 
            it.item.name.contains(searchQuery, ignoreCase = true) 
        }
    }

    Card(
        modifier = modifier.testTag("watchlist_section"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderNavy)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.List, contentDescription = "Watchlist", tint = AccentOrange, modifier = Modifier.size(20.dp))
                    Text("Watchlist", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextWhite)
                }

                IconButton(
                    onClick = { expandedAdd = !expandedAdd },
                    modifier = Modifier.testTag("toggle_add_ticker")
                ) {
                    Icon(
                        if (expandedAdd) Icons.Filled.Close else Icons.Filled.Add,
                        contentDescription = "Add Symbol",
                        tint = AccentOrange,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Expanable Add Ticker form
            AnimatedVisibility(
                visible = expandedAdd,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .testTag("add_ticker_form"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Add Custom Asset", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AccentOrange)
                    
                    OutlinedTextField(
                        value = symInput,
                        onValueChange = { symInput = it },
                        label = { Text("Ticker Symbol (e.g. NSE:RELIANCE or BTCUSD)", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_sym_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange)
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Company Name", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_name_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange)
                    )

                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("Initial Mock Price ($)", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_price_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange)
                    )

                    Column {
                        Text("Asset Type", fontSize = 11.sp, color = TextMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("Stock", "Crypto", "Forex").forEach { typ ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { typeSelection = typ }
                                ) {
                                    RadioButton(
                                        selected = typeSelection == typ,
                                        onClick = { typeSelection = typ },
                                        colors = RadioButtonDefaults.colors(selectedColor = AccentOrange)
                                    )
                                    Text(typ, fontSize = 11.sp, color = TextWhite)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val pr = priceInput.toDoubleOrNull() ?: 100.0
                            if (symInput.isNotBlank() && nameInput.isNotBlank()) {
                                onAddSymbol(symInput.trim(), nameInput.trim(), pr, typeSelection)
                                symInput = ""
                                nameInput = ""
                                priceInput = ""
                                expandedAdd = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                        modifier = Modifier.fillMaxWidth().testTag("save_ticker_button")
                    ) {
                        Text("Add Asset", color = BgMidnight, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search symbols or names...", fontSize = 12.sp, color = TextMuted) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = TextMuted, modifier = Modifier.size(16.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("search_watchlist_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentOrange,
                    unfocusedBorderColor = BorderNavy,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Watchlist Scroll List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredItems, key = { it.item.symbol }) { wrapper ->
                    val isSelected = wrapper.item.symbol == selectedSymbol
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SurfaceCard else Color.Transparent)
                            .border(1.dp, if (isSelected) BorderNavy else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { onSymbolSelect(wrapper.item.symbol) }
                            .padding(horizontal = 8.dp, vertical = 7.dp)
                            .testTag("watchlist_item_${wrapper.item.symbol}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = wrapper.item.symbol,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) AccentOrange else TextWhite,
                                    fontSize = 14.sp
                                )
                                val assetTag = when (wrapper.item.assetType) {
                                    "Crypto" -> "⚡ CRYPTO"
                                    "Forex" -> "💱 FOREX"
                                    else -> "📈 STOCK"
                                }
                                Text(
                                    assetTag,
                                    fontSize = 8.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = wrapper.item.name,
                                fontSize = 11.sp,
                                color = TextMuted,
                                maxLines = 1
                            )
                        }

                        // Price and Tick change indicators
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val tickColor = if (wrapper.isUpTick) BullGreen else BearRed
                            
                            // Glowing container
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = tickColor.copy(alpha = 0.08f)
                                ),
                                border = BorderStroke(0.5.dp, tickColor.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = String.format("%,.4f" , wrapper.price),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = tickColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Delete button if custom
                            if (wrapper.item.isCustom) {
                                IconButton(
                                    onClick = { onDeleteSymbol(wrapper.item.symbol) },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .testTag("delete_${wrapper.item.symbol}")
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = BearRed.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderPlacementSection(
    selectedSymbol: String,
    currentPrice: Double,
    cash: Double,
    onExecuteOrder: (type: String, shares: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var shareInput by remember { mutableStateOf("1") }
    val shares = shareInput.toDoubleOrNull() ?: 0.0
    val costEstimate = shares * currentPrice

    Card(
        modifier = modifier.testTag("order_placement_section"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderNavy)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = "Order", tint = AccentOrange, modifier = Modifier.size(20.dp))
                Text("Order Panel: $selectedSymbol", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextWhite)
            }

            // Cash and ticker balances
            Row(
                modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(8.dp)).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ACCOUNT CASH", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                    Text("$${String.format("%,.2f", cash)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BullGreen)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("LIVE PRICE", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                    Text("$${String.format("%,.4f", currentPrice)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccentOrange)
                }
            }

            // Shares entry
            OutlinedTextField(
                value = shareInput,
                onValueChange = { shareInput = it },
                label = { Text("Quantity (Shares / Units)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("order_shares_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentOrange
                )
            )

            // Valuation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Estimated Value:", fontSize = 12.sp, color = TextMuted)
                Text(
                    text = "$${String.format("%,.2f" , costEstimate)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextWhite
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onExecuteOrder("BUY", shares) },
                    colors = ButtonDefaults.buttonColors(containerColor = BullGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("buy_button")
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("BUY", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Buy", modifier = Modifier.size(20.dp))
                    }
                }

                Button(
                    onClick = { onExecuteOrder("SELL", shares) },
                    colors = ButtonDefaults.buttonColors(containerColor = BearRed, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("sell_button")
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("SELL", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Sell", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TechnicalsGaugeSection(
    symbol: String,
    candles: List<CandlePoint>,
    currentPrice: Double,
    modifier: Modifier = Modifier
) {
    // Generate simple technical indicator readings based on candles
    val analysisReading = remember(candles, currentPrice) {
        if (candles.size < 20) "NEUTRAL" else {
            val sma20 = calculateSMA(candles, 20).lastOrNull() ?: currentPrice
            val ema50 = calculateEMA(candles, 50).lastOrNull() ?: currentPrice
            val rsiVal = calculateRSI(candles, 14).lastOrNull() ?: 50.0

            val isBullishPrice = currentPrice > sma20 && sma20 > ema50
            val isOversold = rsiVal <= 30.0
            val isOverbought = rsiVal >= 70.0

            when {
                isOversold -> "STRONG BUY"
                isOverbought -> "STRONG SELL"
                isBullishPrice && rsiVal > 55.0 -> "BUY"
                !isBullishPrice && rsiVal < 45.0 -> "SELL"
                else -> "NEUTRAL"
            }
        }
    }

    Card(
        modifier = modifier.testTag("technicals_section"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderNavy)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Info, contentDescription = "Technicals", tint = AccentOrange, modifier = Modifier.size(19.dp))
                Text("Technical Analysis", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextWhite)
            }

            // Gauge visualizer (dynamic arc)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val radius = h * 0.8f
                    val center = Offset(w / 2f, h * 0.95f)

                    // Draw Gauge background arc
                    drawArc(
                        brush = Brush.horizontalGradient(
                            colors = listOf(BearRed, Color.Yellow, BullGreen)
                        ),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f)
                    )

                    // Helper to draw indicator arrow
                    val targetAngle = when (analysisReading) {
                        "STRONG SELL" -> 200f
                        "SELL" -> 235f
                        "NEUTRAL" -> 270f
                        "BUY" -> 305f
                        "STRONG BUY" -> 340f
                        else -> 270f
                    }

                    // Draw gauge needle
                    val angleRad = Math.toRadians(targetAngle.toDouble())
                    val needleLen = radius * 0.88f
                    val tipX = center.x + needleLen * cos(angleRad).toFloat()
                    val tipY = center.y + needleLen * sin(angleRad).toFloat()

                    drawLine(
                        color = TextWhite,
                        start = center,
                        end = Offset(tipX, tipY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    drawCircle(
                        color = BgMidnight,
                        radius = 8.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = AccentOrange,
                        radius = 4.dp.toPx(),
                        center = center
                    )
                }

                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = analysisReading,
                        fontWeight = FontWeight.Black,
                        color = when (analysisReading) {
                            "STRONG BUY", "BUY" -> BullGreen
                            "STRONG SELL", "SELL" -> BearRed
                            else -> Color.Yellow
                        },
                        fontSize = 17.sp
                    )
                    Text("INDICATOR SUMMARY", fontSize = 10.sp, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Details list
            val formatStr = if (symbol == "EURUSD") "%,.4f" else "%,.2f"
            val lastClose = candles.lastOrNull()?.close ?: currentPrice
            val firstOpen = candles.firstOrNull()?.open ?: currentPrice
            val rangeLow = candles.minOfOrNull { it.low } ?: currentPrice
            val rangeHigh = candles.maxOfOrNull { it.high } ?: currentPrice

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DetailRow("Close price", String.format(formatStr, lastClose))
                    DetailRow("Avg Vol", String.format("%,.1f", candles.map { it.volume }.average()))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DetailRow("Interval high", String.format(formatStr, rangeHigh))
                    DetailRow("Interval low", String.format(formatStr, rangeLow))
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, valStr: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 9.sp, color = TextMuted)
        Text(valStr, fontSize = 10.sp, color = TextWhite, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MyPortfolioSection(
    summary: com.example.ui.viewmodel.PortfolioSummaryState,
    positions: List<PositionWithValuation>,
    orderHistory: List<DatabaseOrder>,
    onSellFull: (symbol: String, shares: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf("POSITIONS") } // "POSITIONS" or "HISTORY"

    Card(
        modifier = modifier.testTag("portfolio_section"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderNavy)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Home, contentDescription = "Holdings", tint = AccentOrange, modifier = Modifier.size(20.dp))
                    Text("Portfolio & Holdings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextWhite)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (summary.overallPL >= 0) "+" else "",
                        color = if (summary.overallPL >= 0) BullGreen else BearRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$${String.format("%,.2f (%,.2f%%)", summary.overallPL, summary.overallPLPercent)}",
                        color = if (summary.overallPL >= 0) BullGreen else BearRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Stat capsules
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PortfolioStatBox("TOTAL EQUITY", "$${String.format("%,.2f", summary.totalEquity)}", Modifier.weight(1f))
                PortfolioStatBox("HOLDINGS VALUE", "$${String.format("%,.2f", summary.holdingsValue)}", Modifier.weight(1f))
                PortfolioStatBox("CASH", "$${String.format("%,.2f", summary.cash)}", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-tabs switcher (POSITIONS / HISTORY)
            TabRow(
                selectedTabIndex = if (activeTab == "POSITIONS") 0 else 1,
                containerColor = Color.Transparent,
                contentColor = AccentOrange,
                divider = { Divider(color = BorderNavy, thickness = 0.5.dp) }
            ) {
                Tab(
                    selected = activeTab == "POSITIONS",
                    onClick = { activeTab = "POSITIONS" },
                    text = { Text("Open Positions (${positions.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == "HISTORY",
                    onClick = { activeTab = "HISTORY" },
                    text = { Text("Trades History (${orderHistory.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Content
            if (activeTab == "POSITIONS") {
                if (positions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Lock, contentDescription = "No position", tint = TextMuted.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                            Text("No open positions. Buy assets to start paper trading!", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(positions, key = { it.position.symbol }) { h ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceCard, RoundedCornerShape(8.dp))
                                    .border(1.dp, BorderNavy, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(h.position.symbol, fontWeight = FontWeight.Bold, color = AccentOrange, fontSize = 13.sp)
                                    Text("Qty: ${String.format("%.4f", h.position.shares)}", fontSize = 10.sp, color = TextMuted)
                                }

                                Column(
                                    modifier = Modifier.weight(1.5f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text("$${String.format("%,.2f", h.sharesValuation)}", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 13.sp)
                                    Text("Avg cost: $${String.format("%,.4f", h.position.averageEntryPrice)}", fontSize = 9.sp, color = TextMuted)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1.2f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    val plCol = if (h.unrealizedPL >= 0) BullGreen else BearRed
                                    Text(
                                        text = "$${String.format("%,.2f", h.unrealizedPL)}",
                                        fontWeight = FontWeight.Bold,
                                        color = plCol,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "${if (h.unrealizedPL >= 0) "+" else ""}${String.format("%.2f%%", h.unrealizedPLPercent)}",
                                        fontWeight = FontWeight.Bold,
                                        color = plCol,
                                        fontSize = 9.sp
                                    )
                                }

                                IconButton(
                                    onClick = { onSellFull(h.position.symbol, h.position.shares) },
                                    modifier = Modifier.padding(start = 4.dp).size(28.dp).testTag("close_pos_${h.position.symbol}")
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Close position",
                                        tint = BearRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val exportLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("text/csv")
                ) { uri ->
                    if (uri != null) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                    val writer = outputStream.bufferedWriter()
                                    writer.write("Symbol,Type,Price,Shares,Timestamp\n")
                                    orderHistory.forEach { order ->
                                        val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(order.timestamp))
                                        writer.write("${order.symbol},${order.type},${order.price},${order.shares},${date}\n")
                                    }
                                    writer.flush()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

                if (orderHistory.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { exportLauncher.launch("transaction_history.csv") },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp).testTag("export_csv_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Share, contentDescription = "Export CSV", modifier = Modifier.size(12.dp))
                                Text("Export CSV", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (orderHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No recorded trade executions.", color = TextMuted, fontSize = 11.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(orderHistory) { order ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceCard, RoundedCornerShape(6.dp))
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val isBuy = order.type == "BUY"
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isBuy) BullGreen.copy(alpha = 0.15f) else BearRed.copy(alpha = 0.15f)
                                        )
                                    ) {
                                        Text(
                                            text = order.type,
                                            color = if (isBuy) BullGreen else BearRed,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    Column {
                                        Text(order.symbol, fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 12.sp)
                                        Text(
                                            text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(order.timestamp)),
                                            fontSize = 8.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$${String.format("%,.2f", order.price * order.shares)}",
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "${String.format("%.4f", order.shares)} units @ $${String.format("%,.4f", order.price)}",
                                        fontSize = 9.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PortfolioStatBox(title: String, valStr: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.5.dp, BorderNavy)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 8.sp, color = TextMuted, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                valStr,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

fun calculateSMA(candles: List<com.example.data.model.CandlePoint>, period: Int): List<Double> {
    val smaList = mutableListOf<Double>()
    for (i in candles.indices) {
        if (i < period - 1) {
            smaList.add(candles[i].close)
        } else {
            var sum = 0.0
            for (j in 0 until period) {
                sum += candles[i - j].close
            }
            smaList.add(sum / period)
        }
    }
    return smaList
}

fun calculateEMA(candles: List<com.example.data.model.CandlePoint>, period: Int): List<Double> {
    val emaList = mutableListOf<Double>()
    val multiplier = 2.0 / (period + 1)
    var prevEMA = candles.firstOrNull()?.close ?: 0.0
    for (i in candles.indices) {
        if (i == 0) {
            emaList.add(prevEMA)
        } else {
            val close = candles[i].close
            val ema = (close - prevEMA) * multiplier + prevEMA
            emaList.add(ema)
            prevEMA = ema
        }
    }
    return emaList
}

fun calculateRSI(candles: List<com.example.data.model.CandlePoint>, period: Int = 14): List<Double> {
    val rsiList = mutableListOf<Double>()
    var avgGain = 0.0
    var avgLoss = 0.0

    for (i in candles.indices) {
        if (i == 0) {
            rsiList.add(50.0)
            continue
        }

        val change = candles[i].close - candles[i - 1].close
        val gain = if (change > 0) change else 0.0
        val loss = if (change < 0) -change else 0.0

        if (i < period) {
            avgGain += gain
            avgLoss += loss
            if (i == period - 1) {
                avgGain /= period
                avgLoss /= period
                val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
                rsiList.add(if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + rs)))
            } else {
                rsiList.add(50.0)
            }
        } else {
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
            val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
            rsiList.add(if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + rs)))
        }
    }
    return rsiList
}
