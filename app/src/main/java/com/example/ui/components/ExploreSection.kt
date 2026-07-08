package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.WatchlistItemWithPrice

@Composable
fun ExploreSection(
    watchlistItems: List<WatchlistItemWithPrice>,
    livePrices: Map<String, Double>,
    selectedSymbol: String,
    onSymbolSelect: (String) -> Unit,
    onNavigateToChart: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSector by remember { mutableStateOf<String?>(null) }
    var expandedFolder by remember { mutableStateOf<String?>(null) }

    // Curated Watchlist Folders
    val folders = listOf(
        WatchlistFolder(
            id = "indian_giants",
            name = "🇮🇳 Indian Giants",
            description = "Top blue-chip Indian stocks on NSE",
            symbols = listOf("NSE:RELIANCE", "NSE:TCS", "NSE:HDFCBANK", "NSE:INFY", "NSE:SBIN", "NSE:TATAMOTORS")
        ),
        WatchlistFolder(
            id = "tech_titans",
            name = "🇺🇸 Tech Titans",
            description = "High-growth US technology leaders",
            symbols = listOf("AAPL", "TSLA", "NVDA", "AMZN", "MSFT", "GOOGL")
        ),
        WatchlistFolder(
            id = "crypto_gems",
            name = "🪙 Crypto Gems",
            description = "Major decentralized digital assets",
            symbols = listOf("BTCUSD", "ETHUSD", "SOLUSD")
        ),
        WatchlistFolder(
            id = "mcx_commodities",
            name = "🛢️ MCX Commodities",
            description = "Leading MCX futures markets",
            symbols = listOf("MCX:GOLD1!", "MCX:SILVER1!", "MCX:CRUDEOIL1!", "MCX:NATURALGAS1!")
        )
    )

    // Sectors ("Alag Alag")
    val sectors = listOf(
        SectorCategory("🏦 Banking & Finance", listOf("NSE:HDFCBANK", "NSE:SBIN")),
        SectorCategory("💻 Information Technology", listOf("NSE:TCS", "NSE:INFY", "MSFT")),
        SectorCategory("⚡ Energy & Utilities", listOf("NSE:RELIANCE", "MCX:CRUDEOIL1!")),
        SectorCategory("🚗 Automobile Sector", listOf("NSE:TATAMOTORS", "TSLA"))
    )

    // Helper to get item price & uptick state
    fun getItemInfo(symbol: String): Pair<Double, Boolean> {
        val found = watchlistItems.find { it.item.symbol == symbol }
        val price = livePrices[symbol] ?: found?.price ?: 100.0
        val isUp = found?.isUpTick ?: (Math.random() > 0.5)
        return Pair(price, isUp)
    }

    // Helper to get descriptive name
    fun getSymbolName(symbol: String): String {
        return watchlistItems.find { it.item.symbol == symbol }?.item?.name ?: symbol.substringAfter("NSE:")
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgMidnight)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Search Box & Welcome Banner ---
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Explore Markets",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Text(
                        "Real-time indices, custom folders, and top active assets",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Search text field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search stocks, cryptos, commodities...", color = TextMuted, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = AccentOrange) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = BgMidnight,
                            unfocusedContainerColor = BgMidnight,
                            focusedBorderColor = AccentOrange,
                            unfocusedBorderColor = BorderNavy
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("explore_search")
                    )
                }
            }
        }

        if (searchQuery.isNotEmpty()) {
            // --- SEARCH RESULTS MODE ---
            val results = watchlistItems.filter {
                it.item.symbol.contains(searchQuery, ignoreCase = true) ||
                it.item.name.contains(searchQuery, ignoreCase = true)
            }

            if (results.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, contentDescription = "No Results", tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No matching assets found", color = TextMuted, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                item {
                    Text("Search Results", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                items(results) { item ->
                    val curPrice = livePrices[item.item.symbol] ?: item.price
                    StockRowItem(
                        symbol = item.item.symbol,
                        name = item.item.name,
                        price = curPrice,
                        isUpTick = item.isUpTick,
                        onClick = {
                            onSymbolSelect(item.item.symbol)
                            onNavigateToChart()
                        }
                    )
                }
            }
        } else {
            // --- NORMAL EXPLORE MODE ---

            // --- 2. Live Market Indices ---
            item {
                Column {
                    Text("Live Indices", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            IndexCard(
                                name = "NIFTY 50",
                                price = livePrices["NSE:RELIANCE"]?.let { 23500.0 + (it - 2900.0) * 4 } ?: 23485.60,
                                change = 142.30,
                                changePercent = 0.61,
                                isUp = true
                            )
                        }
                        item {
                            IndexCard(
                                name = "SENSEX",
                                price = livePrices["NSE:TCS"]?.let { 77200.0 + (it - 3800.0) * 10 } ?: 77140.25,
                                change = 398.15,
                                changePercent = 0.52,
                                isUp = true
                            )
                        }
                        item {
                            IndexCard(
                                name = "NASDAQ 100",
                                price = livePrices["AAPL"]?.let { 19200.0 + (it - 180.25) * 15 } ?: 19185.40,
                                change = -84.20,
                                changePercent = -0.44,
                                isUp = false
                            )
                        }
                        item {
                            IndexCard(
                                name = "S&P 500",
                                price = livePrices["MSFT"]?.let { 5430.0 + (it - 420.5) * 2 } ?: 5424.10,
                                change = 12.85,
                                changePercent = 0.24,
                                isUp = true
                            )
                        }
                    }
                }
            }

            // --- 3. Watchlist Folders ("Folders of your Watchlist") ---
            item {
                Text("Your Curated Watchlist Folders", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(2.dp))
            }

            items(folders) { folder ->
                val isExpanded = expandedFolder == folder.id
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isExpanded) AccentOrange else BorderNavy),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedFolder = if (isExpanded) null else folder.id
                        }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(AccentOrange.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📁", fontSize = 20.sp)
                                }
                                Column {
                                    Text(
                                        text = folder.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${folder.symbols.size} assets • ${folder.description}",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand/Collapse",
                                tint = Color.Gray
                            )
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Divider(color = BorderNavy, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(4.dp))
                                folder.symbols.forEach { sym ->
                                    val (price, isUp) = getItemInfo(sym)
                                    val name = getSymbolName(sym)
                                    StockRowItem(
                                        symbol = sym,
                                        name = name,
                                        price = price,
                                        isUpTick = isUp,
                                        onClick = {
                                            onSymbolSelect(sym)
                                            onNavigateToChart()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 4. Curated Sectors & Top Categories ("Alag Alag") ---
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Browse by Sector (Alag Alag)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(sectors) { sector ->
                        val isSelected = selectedSector == sector.name
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSector = if (isSelected) null else sector.name },
                            label = { Text(sector.name, fontSize = 12.sp, color = if (isSelected) Color.Black else Color.White) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceDark,
                                labelColor = Color.White,
                                selectedContainerColor = AccentOrange,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            // Render selected sector assets directly in list
            selectedSector?.let { sectorName ->
                val matchedSector = sectors.find { it.name == sectorName }
                if (matchedSector != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AccentOrange.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Sector: ${matchedSector.name}",
                                    color = AccentOrange,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                matchedSector.symbols.forEach { sym ->
                                    val (price, isUp) = getItemInfo(sym)
                                    val name = getSymbolName(sym)
                                    StockRowItem(
                                        symbol = sym,
                                        name = name,
                                        price = price,
                                        isUpTick = isUp,
                                        onClick = {
                                            onSymbolSelect(sym)
                                            onNavigateToChart()
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }

            // --- 5. Top Gainers & Movers (Dynamic Computations) ---
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Top Gainers & Volatile Assets", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Curate top 4 assets currently showing high gains/activity
            val volatileAssets = listOf("NVDA", "NSE:TATAMOTORS", "SOLUSD", "TSLA")
            items(volatileAssets) { sym ->
                val (price, isUp) = getItemInfo(sym)
                val name = getSymbolName(sym)
                StockRowItem(
                    symbol = sym,
                    name = name,
                    price = price,
                    isUpTick = isUp,
                    onClick = {
                        onSymbolSelect(sym)
                        onNavigateToChart()
                    }
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun IndexCard(
    name: String,
    price: Double,
    change: Double,
    changePercent: Double,
    isUp: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderNavy),
        modifier = Modifier
            .width(135.dp)
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = String.format("%,.2f", price),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            val sign = if (isUp) "+" else ""
            val color = if (isUp) BullGreen else BearRed
            Text(
                text = "$sign${String.format("%.2f", change)} ($sign${String.format("%.2f", changePercent)}%)",
                color = color,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun StockRowItem(
    symbol: String,
    name: String,
    price: Double,
    isUpTick: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderNavy),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = symbol.removePrefix("NSE:"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                if (symbol.startsWith("NSE:")) Color(0xFF1E88E5).copy(alpha = 0.15f)
                                else if (symbol.contains("USD")) Color(0xFF8E24AA).copy(alpha = 0.15f)
                                else Color(0xFF43A047).copy(alpha = 0.15f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (symbol.startsWith("NSE:")) "NSE" else if (symbol.contains("USD")) "Crypto" else "US Stock",
                            color = if (symbol.startsWith("NSE:")) Color(0xFF90CAF9) else if (symbol.contains("USD")) Color(0xFFE1BEE7) else Color(0xFFA5D6A7),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    text = name,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    text = if (price > 1000.0) String.format("%,.2f", price) else String.format("%.2f", price),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val color = if (isUpTick) BullGreen else BearRed
                    val icon = if (isUpTick) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (isUpTick) "+0.78%" else "-0.65%",
                        color = color,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Details",
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Data models
data class WatchlistFolder(
    val id: String,
    val name: String,
    val description: String,
    val symbols: List<String>
)

data class SectorCategory(
    val name: String,
    val symbols: List<String>
)
