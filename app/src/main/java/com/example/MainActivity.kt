package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradingViewModel
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgMidnight
                ) {
                    TradeAppMainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeAppMainScreen(
    viewModel: TradingViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Observe StateFlows
    val watchlist by viewModel.watchlist.collectAsState()
    val selectedSymbol by viewModel.selectedSymbol.collectAsState()
    val selectedTimeframe by viewModel.selectedTimeframe.collectAsState()
    val candles by viewModel.candles.collectAsState()
    val showSMA by viewModel.showSMA.collectAsState()
    val showEMA by viewModel.showEMA.collectAsState()
    val showRSI by viewModel.showRSI.collectAsState()
    val showUTBot by viewModel.showUTBot.collectAsState()
    val activeDrawingTool by viewModel.activeDrawingTool.collectAsState()
    val crosshairPoint by viewModel.crosshairPoint.collectAsState()
    val activeTrendlines by viewModel.activeTrendlines.collectAsState()
    val portfolioSummary by viewModel.portfolioSummary.collectAsState()
    val positions by viewModel.positions.collectAsState()
    val orderHistory by viewModel.orderHistory.collectAsState()

    // Pine editor state
    var showPineEditor by remember { mutableStateOf(false) }

    // Find current selected price
    val activePrice = remember(watchlist, selectedSymbol) {
        watchlist.find { it.item.symbol == selectedSymbol }?.price ?: 100.0
    }

    var currentScreen by remember { mutableStateOf("HOME") }

    // Capture Toast events
    LaunchedEffect(Unit) {
        viewModel.notification.collectLatest { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Glowing logo accent
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "π",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Text(
                            "Max View",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgMidnight
                ),
                actions = {
                    // Quick stats pill
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "PAPER BALANCE: $${String.format("%,.0f", portfolioSummary.totalEquity)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }
                    }

                    // Search Button
                    IconButton(
                        onClick = { currentScreen = "WATCHLIST" },
                        modifier = Modifier.testTag("search_btn")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = TextWhite)
                    }

                    // Settings Button
                    IconButton(
                        onClick = { currentScreen = "SETTINGS" },
                        modifier = Modifier.testTag("settings_btn")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextWhite)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = BgMidnight
            ) {
                NavigationBarItem(
                    selected = currentScreen == "HOME",
                    onClick = { currentScreen = "HOME" },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentOrange,
                        selectedTextColor = AccentOrange,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = SurfaceCard
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == "WATCHLIST",
                    onClick = { currentScreen = "WATCHLIST" },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Markets") },
                    label = { Text("Markets") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentOrange,
                        selectedTextColor = AccentOrange,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = SurfaceCard
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == "CHART",
                    onClick = { currentScreen = "CHART" },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Chart") },
                    label = { Text("Chart") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentOrange,
                        selectedTextColor = AccentOrange,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = SurfaceCard
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == "PAPER_TRADING",
                    onClick = { currentScreen = "PAPER_TRADING" },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Paper Trading") },
                    label = { Text("Trade") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentOrange,
                        selectedTextColor = AccentOrange,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = SurfaceCard
                    )
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BgMidnight)
        ) {
            if (currentScreen == "HOME") {
                WatchlistSection(
                    items = watchlist,
                    selectedSymbol = selectedSymbol,
                    onSymbolSelect = { 
                        viewModel.selectSymbol(it)
                        currentScreen = "CHART"
                    },
                    onDeleteSymbol = { viewModel.deleteTicker(it) },
                    onAddSymbol = { sym, name, pr, typ -> viewModel.addCustomTicker(sym, name, pr, typ) },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (currentScreen == "WATCHLIST") {
                var selectedMarket by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("india") }
                var searchQuick by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                
                androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.material3.OutlinedTextField(
                        value = searchQuick,
                        onValueChange = { searchQuick = it },
                        placeholder = { Text("Search any symbol (e.g. MCX:GOLD1!, NSE:RELIANCE)", color = Color.Gray, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = {
                            if (searchQuick.isNotBlank()) {
                                viewModel.selectSymbol(searchQuick.trim().uppercase())
                                focusManager.clearFocus()
                                currentScreen = "CHART"
                            }
                        }),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = AccentOrange,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )

                    androidx.compose.material3.ScrollableTabRow(
                        selectedTabIndex = when (selectedMarket) {
                            "india" -> 0
                            "nse" -> 1
                            "bse" -> 2
                            "mcx" -> 3
                            "america" -> 4
                            "crypto" -> 5
                            "forex" -> 6
                            else -> 7
                        },
                        containerColor = BgMidnight,
                        contentColor = AccentOrange,
                        edgePadding = 8.dp
                    ) {
                        androidx.compose.material3.Tab(
                            selected = selectedMarket == "india",
                            onClick = { selectedMarket = "india" },
                            text = { androidx.compose.material3.Text("IN Stocks") }
                        )
                        androidx.compose.material3.Tab(
                            selected = selectedMarket == "nse",
                            onClick = { selectedMarket = "nse" },
                            text = { androidx.compose.material3.Text("NSE") }
                        )
                        androidx.compose.material3.Tab(
                            selected = selectedMarket == "bse",
                            onClick = { selectedMarket = "bse" },
                            text = { androidx.compose.material3.Text("BSE") }
                        )
                        androidx.compose.material3.Tab(
                            selected = selectedMarket == "mcx",
                            onClick = { selectedMarket = "mcx" },
                            text = { androidx.compose.material3.Text("MCX") }
                        )
                        androidx.compose.material3.Tab(
                            selected = selectedMarket == "america",
                            onClick = { selectedMarket = "america" },
                            text = { androidx.compose.material3.Text("US Stocks") }
                        )
                        androidx.compose.material3.Tab(
                            selected = selectedMarket == "crypto",
                            onClick = { selectedMarket = "crypto" },
                            text = { androidx.compose.material3.Text("Crypto") }
                        )
                        androidx.compose.material3.Tab(
                            selected = selectedMarket == "forex",
                            onClick = { selectedMarket = "forex" },
                            text = { androidx.compose.material3.Text("Forex") }
                        )
                    }
                    com.example.ui.components.TradingViewScreenerWidget(
                        market = selectedMarket,
                        onSymbolClick = { sym -> 
                            viewModel.selectSymbol(sym)
                            currentScreen = "CHART"
                        },
                        modifier = Modifier.fillMaxSize().weight(1f)
                    )
                }
            } else if (currentScreen == "CHART") {
                val lastCandle = candles.lastOrNull()
                var countdownText by remember(lastCandle, selectedTimeframe) { mutableStateOf("") }
                LaunchedEffect(lastCandle, selectedTimeframe) {
                    if (lastCandle != null) {
                        val intervalMs = when (selectedTimeframe) {
                            "1M" -> 60_000L
                            "5M" -> 300_000L
                            "15M" -> 900_000L
                            "1H" -> 3_600_000L
                            "4H" -> 14_400_000L
                            "1D" -> 86_400_000L
                            "1W" -> 604_800_000L
                            "1Mo" -> 2_592_000_000L
                            else -> 86_400_000L
                        }
                        while (true) {
                            val now = System.currentTimeMillis()
                            val nextCandleTime = lastCandle.timestamp + intervalMs
                            val diffMs = nextCandleTime - now
                            if (diffMs <= 0) {
                                countdownText = "Generating..."
                            } else {
                                val totalSecs = diffMs / 1000
                                val hours = totalSecs / 3600
                                val mins = (totalSecs % 3600) / 60
                                val secs = totalSecs % 60
                                countdownText = when {
                                    hours > 0 -> String.format("%02dh %02dm %02ds", hours, mins, secs)
                                    mins > 0 -> String.format("%02dm %02ds", mins, secs)
                                    else -> String.format("%02ds", secs)
                                }
                            }
                            kotlinx.coroutines.delay(1000)
                        }
                    }
                }

                androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
                    val position = positions.find { it.position.symbol == selectedSymbol }
                    val currentPrice = watchlist.find { it.item.symbol == selectedSymbol }?.price ?: 0.0
                    
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(com.example.ui.theme.BgMidnight)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        val isInWatchlist = watchlist.any { it.item.symbol.equals(selectedSymbol, ignoreCase = true) }
                        
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                        ) {
                            androidx.compose.material3.Text(
                                text = selectedSymbol,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            
                            androidx.compose.material3.IconButton(
                                onClick = {
                                    if (isInWatchlist) {
                                        viewModel.deleteTicker(selectedSymbol)
                                    } else {
                                        viewModel.addSymbolToWatchlist(selectedSymbol)
                                    }
                                }
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = if (isInWatchlist) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = if (isInWatchlist) "Remove from Watchlist" else "Add to Watchlist",
                                    tint = if (isInWatchlist) com.example.ui.theme.BullGreen else com.example.ui.theme.AccentOrange
                                )
                            }
                        }
                        
                        androidx.compose.foundation.layout.Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.End
                        ) {
                            androidx.compose.material3.Text(
                                text = "$${String.format("%,.2f", currentPrice)}",
                                color = com.example.ui.theme.AccentOrange,
                                fontSize = 20.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            if (countdownText.isNotEmpty()) {
                                androidx.compose.material3.Text(
                                    text = "Next candle in: $countdownText",
                                    color = com.example.ui.theme.TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    
                    if (position != null) {
                        val pl = (currentPrice - position.position.averageEntryPrice) * position.position.shares
                        val isProfit = pl >= 0
                        
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(com.example.ui.theme.SurfaceDark)
                                .padding(12.dp)
                        ) {
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                androidx.compose.foundation.layout.Column {
                                    androidx.compose.material3.Text(
                                        "Your Position: ${position.position.shares} shares", 
                                        color = com.example.ui.theme.TextWhite,
                                        fontSize = 14.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    androidx.compose.material3.Text(
                                        "Avg Buy: $${String.format("%,.2f", position.position.averageEntryPrice)}", 
                                        color = com.example.ui.theme.TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                                androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                    androidx.compose.material3.Text(
                                        "Current: $${String.format("%,.2f", currentPrice)}", 
                                        color = com.example.ui.theme.TextWhite,
                                        fontSize = 14.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    androidx.compose.material3.Text(
                                        "P&L: ${if (isProfit) "+" else ""}$${String.format("%,.2f", pl)}", 
                                        color = if (isProfit) com.example.ui.theme.BullGreen else com.example.ui.theme.BearRed,
                                        fontSize = 12.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                            }
                            
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color.DarkGray,
                                thickness = 0.5.dp
                            )
                            
                            var slInput by remember(position.position.stopLoss) { 
                                mutableStateOf(position.position.stopLoss?.let { String.format("%.2f", it) } ?: "") 
                            }
                            var tpInput by remember(position.position.takeProfit) { 
                                mutableStateOf(position.position.takeProfit?.let { String.format("%.2f", it) } ?: "") 
                            }
                            
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                                    androidx.compose.material3.Text("Stop Loss (SL)", color = com.example.ui.theme.TextMuted, fontSize = 11.sp)
                                    androidx.compose.material3.OutlinedTextField(
                                        value = slInput,
                                        onValueChange = { slInput = it },
                                        placeholder = { Text("None", color = Color.Gray, fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.Black,
                                            unfocusedContainerColor = Color.Black,
                                            focusedBorderColor = com.example.ui.theme.BearRed,
                                            unfocusedBorderColor = Color.DarkGray
                                        )
                                    )
                                    androidx.compose.foundation.layout.Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(-1, -2, -5).forEach { pct ->
                                            androidx.compose.material3.Card(
                                                onClick = {
                                                    val targetPrice = position.position.averageEntryPrice * (1 + pct / 100.0)
                                                    slInput = String.format("%.2f", targetPrice)
                                                },
                                                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.4f)),
                                                modifier = Modifier.height(24.dp).clickable {
                                                    val targetPrice = position.position.averageEntryPrice * (1 + pct / 100.0)
                                                    slInput = String.format("%.2f", targetPrice)
                                                }
                                            ) {
                                                Box(contentAlignment = androidx.compose.ui.Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp).fillMaxHeight()) {
                                                    Text("$pct%", color = com.example.ui.theme.BearRed, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                                    androidx.compose.material3.Text("Take Profit (TP)", color = com.example.ui.theme.TextMuted, fontSize = 11.sp)
                                    androidx.compose.material3.OutlinedTextField(
                                        value = tpInput,
                                        onValueChange = { tpInput = it },
                                        placeholder = { Text("None", color = Color.Gray, fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.Black,
                                            unfocusedContainerColor = Color.Black,
                                            focusedBorderColor = com.example.ui.theme.BullGreen,
                                            unfocusedBorderColor = Color.DarkGray
                                        )
                                    )
                                    androidx.compose.foundation.layout.Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(2, 5, 10).forEach { pct ->
                                            androidx.compose.material3.Card(
                                                onClick = {
                                                    val targetPrice = position.position.averageEntryPrice * (1 + pct / 100.0)
                                                    tpInput = String.format("%.2f", targetPrice)
                                                },
                                                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.4f)),
                                                modifier = Modifier.height(24.dp).clickable {
                                                    val targetPrice = position.position.averageEntryPrice * (1 + pct / 100.0)
                                                    tpInput = String.format("%.2f", targetPrice)
                                                }
                                            ) {
                                                Box(contentAlignment = androidx.compose.ui.Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp).fillMaxHeight()) {
                                                    Text("+$pct%", color = com.example.ui.theme.BullGreen, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        val slVal = slInput.toDoubleOrNull()
                                        val tpVal = tpInput.toDoubleOrNull()
                                        viewModel.setStopLossAndTakeProfit(selectedSymbol, slVal, tpVal)
                                    },
                                    modifier = Modifier
                                        .padding(top = 16.dp)
                                        .size(36.dp)
                                        .background(com.example.ui.theme.AccentOrange, androidx.compose.foundation.shape.CircleShape)
                                ) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Save SL/TP",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(com.example.ui.theme.SurfaceDark)
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        listOf("1M", "5M", "15M", "1H", "4H", "1D", "1W").forEach { tf ->
                            androidx.compose.material3.FilterChip(
                                selected = selectedTimeframe == tf,
                                onClick = { viewModel.selectTimeframe(tf) },
                                label = { androidx.compose.material3.Text(tf) },
                                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = com.example.ui.theme.AccentOrange,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                        
                        androidx.compose.material3.FilterChip(
                            selected = showSMA,
                            onClick = { viewModel.toggleSMA() },
                            label = { androidx.compose.material3.Text("SMA") },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = com.example.ui.theme.AccentOrange,
                                selectedLabelColor = Color.White
                            )
                        )
                        androidx.compose.material3.FilterChip(
                            selected = showEMA,
                            onClick = { viewModel.toggleEMA() },
                            label = { androidx.compose.material3.Text("EMA") },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = com.example.ui.theme.AccentOrange,
                                selectedLabelColor = Color.White
                            )
                        )
                        androidx.compose.material3.FilterChip(
                            selected = showRSI,
                            onClick = { viewModel.toggleRSI() },
                            label = { androidx.compose.material3.Text("RSI") },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = com.example.ui.theme.AccentOrange,
                                selectedLabelColor = Color.White
                            )
                        )
                        androidx.compose.material3.FilterChip(
                            selected = showUTBot,
                            onClick = { viewModel.toggleUTBot() },
                            label = { androidx.compose.material3.Text("UT Bot") },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = com.example.ui.theme.AccentOrange,
                                selectedLabelColor = Color.White
                            )
                        )
                        androidx.compose.material3.TextButton(
                            onClick = { showPineEditor = true }
                        ) {
                            androidx.compose.material3.Text(
                                "Pine Editor { }",
                                color = com.example.ui.theme.AccentOrange,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }

                    com.example.ui.components.TradingViewWidget(
                        symbol = selectedSymbol,
                        interval = selectedTimeframe,
                        showSMA = showSMA,
                        showEMA = showEMA,
                        showRSI = showRSI,
                        showUTBot = showUTBot,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                    
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(com.example.ui.theme.SurfaceDark)
                            .padding(16.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                    ) {
                        androidx.compose.material3.Button(
                            onClick = { currentScreen = "PAPER_TRADING" },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = com.example.ui.theme.BullGreen,
                                contentColor = Color.Black
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            androidx.compose.material3.Text("BUY", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                        androidx.compose.material3.Button(
                            onClick = { currentScreen = "PAPER_TRADING" },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = com.example.ui.theme.BearRed,
                                contentColor = Color.White
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            androidx.compose.material3.Text("SELL", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                    }
                }
            } else if (currentScreen == "PAPER_TRADING") {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                ) {
                    val currentPrice = watchlist.find { it.item.symbol == selectedSymbol }?.price ?: 0.0
                    com.example.ui.components.OrderPlacementSection(
                        selectedSymbol = selectedSymbol,
                        currentPrice = currentPrice,
                        cash = portfolioSummary.cash,
                        onExecuteOrder = { type, shares -> viewModel.executePaperTrade(type, shares) }
                    )
                    com.example.ui.components.MyPortfolioSection(
                        summary = portfolioSummary,
                        positions = positions,
                        orderHistory = orderHistory,
                        onSellFull = { symbol, qty -> viewModel.executeSellDirect(symbol, qty) },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }
            } else {
                com.example.ui.components.SettingsSection(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    
    if (showPineEditor) {
        PineEditorDialog(
            onDismiss = { showPineEditor = false },
            onApply = { /* Apply logic */ showPineEditor = false }
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PineEditorDialog(onDismiss: () -> Unit, onApply: (String) -> Unit) {
    var code by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("//@version=5\nindicator(\"My Script\")\nplot(close)\n") }
    var selectedVersion by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("v5") }
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Text("Pine Editor", color = com.example.ui.theme.TextWhite)
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.FilterChip(
                        selected = selectedVersion == "v5",
                        onClick = { selectedVersion = "v5"; code = "//@version=5\nindicator(\"My Script v5\")\nplot(close)\n" },
                        label = { androidx.compose.material3.Text("v5") }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = selectedVersion == "v6",
                        onClick = { selectedVersion = "v6"; code = "//@version=6\nindicator(\"My Script v6\")\nplot(close)\n" },
                        label = { androidx.compose.material3.Text("v6") }
                    )
                }
            }
        },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = com.example.ui.theme.TextWhite
                ),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedBorderColor = com.example.ui.theme.AccentOrange,
                    unfocusedBorderColor = Color.DarkGray
                )
            )
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { onApply(code) },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.AccentOrange)
            ) {
                androidx.compose.material3.Text("Add to Chart", color = Color.White)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = com.example.ui.theme.SurfaceDark,
        textContentColor = com.example.ui.theme.TextWhite,
        titleContentColor = com.example.ui.theme.TextWhite
    )
}

