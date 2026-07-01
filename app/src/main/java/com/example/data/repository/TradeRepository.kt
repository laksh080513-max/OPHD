package com.example.data.repository

import com.example.data.db.TradingDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Random
import com.example.data.api.MarketDataClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TradeRepository(private val dao: TradingDao) {

    // Default watchlists
    private val defaultWatchlist = listOf(
        WatchlistItem("BTCUSD", "Bitcoin / US Dollar", false, 68500.0, "Crypto"),
        WatchlistItem("ETHUSD", "Ethereum / US Dollar", false, 3450.0, "Crypto"),
        WatchlistItem("SOLUSD", "Solana / US Dollar", false, 145.5, "Crypto"),
        WatchlistItem("AAPL", "Apple Inc.", false, 180.25, "Stock"),
        WatchlistItem("TSLA", "Tesla Inc.", false, 175.8, "Stock"),
        WatchlistItem("NVDA", "NVIDIA Corporation", false, 950.4, "Stock"),
        WatchlistItem("AMZN", "Amazon.com Inc.", false, 182.1, "Stock"),
        WatchlistItem("MSFT", "Microsoft Corporation", false, 420.5, "Stock"),
        WatchlistItem("GOOGL", "Alphabet Inc.", false, 175.2, "Stock"),
        WatchlistItem("NSE:RELIANCE", "Reliance Industries", false, 2900.0, "Stock"),
        WatchlistItem("NSE:TCS", "Tata Consultancy Services", false, 3800.0, "Stock"),
        WatchlistItem("NSE:HDFCBANK", "HDFC Bank", false, 1500.0, "Stock"),
        WatchlistItem("NSE:INFY", "Infosys", false, 1400.0, "Stock"),
        WatchlistItem("NSE:SBIN", "State Bank of India", false, 800.0, "Stock"),
        WatchlistItem("NSE:TATAMOTORS", "Tata Motors", false, 1000.0, "Stock"),
        WatchlistItem("MCX:GOLD1!", "Gold Futures", false, 71500.0, "Commodity"),
        WatchlistItem("MCX:SILVER1!", "Silver Futures", false, 90000.0, "Commodity"),
        WatchlistItem("MCX:CRUDEOIL1!", "Crude Oil Futures", false, 6500.0, "Commodity"),
        WatchlistItem("MCX:NATURALGAS1!", "Natural Gas Futures", false, 250.0, "Commodity"),
        WatchlistItem("MCX:COPPER1!", "Copper Futures", false, 850.0, "Commodity"),
        WatchlistItem("MCX:ZINC1!", "Zinc Futures", false, 250.0, "Commodity"),
        WatchlistItem("MCX:ALUMINIUM1!", "Aluminium Futures", false, 240.0, "Commodity"),
        WatchlistItem("MCX:LEAD1!", "Lead Futures", false, 190.0, "Commodity"),
        WatchlistItem("EURUSD", "Euro / US Dollar", false, 1.0850, "Forex")
    )

    // Keep track of live prices in-memory for live ticks
    private val _livePrices = MutableStateFlow<Map<String, Double>>(emptyMap())
    val livePricesFlow: StateFlow<Map<String, Double>> = _livePrices.asStateFlow()

    // Keep track of secondary price changes (green / red glow indicators)
    private val _priceDirections = MutableStateFlow<Map<String, Boolean>>(emptyMap()) // true = up, false = down
    val priceDirectionsFlow: StateFlow<Map<String, Boolean>> = _priceDirections.asStateFlow()

    init {
        // Initialize default prices based on watchlist
        val initialMap = defaultWatchlist.associate { it.symbol to it.initialPrice }
        _livePrices.value = initialMap
    }

    suspend fun initializeDatabaseIfNeeded() {
        // Ensure default cash exists
        val currentProfile = dao.getPortfolioStateSync()
        if (currentProfile == null) {
            dao.insertPortfolioState(PortfolioState(id = 1, cash = 100000.0))
        }

        // Add missing default watchlists
        val currentWatchlist = dao.getWatchlistFlow().firstOrNull()?.map { it.symbol } ?: emptyList()
        for (item in defaultWatchlist) {
            if (item.symbol !in currentWatchlist) {
                dao.insertWatchlistItem(item)
            }
        }
    }

    // Maps our internal symbols to Yahoo Finance symbols
    private val yahooSymbolMap = mapOf(
        "BTCUSD" to "BTC-USD",
        "ETHUSD" to "ETH-USD",
        "SOLUSD" to "SOL-USD",
        "AAPL" to "AAPL",
        "TSLA" to "TSLA",
        "NVDA" to "NVDA",
        "AMZN" to "AMZN",
        "MSFT" to "MSFT",
        "GOOGL" to "GOOGL",
        "NSE:RELIANCE" to "RELIANCE.NS",
        "NSE:TCS" to "TCS.NS",
        "NSE:HDFCBANK" to "HDFCBANK.NS",
        "NSE:INFY" to "INFY.NS",
        "NSE:SBIN" to "SBIN.NS",
        "NSE:TATAMOTORS" to "TATAMOTORS.NS",
        "EURUSD" to "EURUSD=X"
    )

    private val reverseYahooSymbolMap = yahooSymbolMap.entries.associate { (k, v) -> v to k }

    private fun toYahooSymbol(symbol: String): String {
        return yahooSymbolMap[symbol] ?: if (symbol.startsWith("NSE:")) symbol.removePrefix("NSE:") + ".NS" else symbol
    }

    private fun fromYahooSymbol(yahoo: String): String {
        return reverseYahooSymbolMap[yahoo] ?: if (yahoo.endsWith(".NS")) "NSE:" + yahoo.removeSuffix(".NS") else yahoo
    }

    // Live Feed API Fetch Trigger
    suspend fun fetchRealTimePrices() {
        try {
            val symbolsToFetch = _livePrices.value.keys.map { toYahooSymbol(it) }.joinToString(",")
            val response = withContext(Dispatchers.IO) {
                MarketDataClient.yahooApi.getYahooQuotes(symbolsToFetch)
            }
            
            val current = _livePrices.value.toMutableMap()
            val directions = _priceDirections.value.toMutableMap()
            
            response.quoteResponse?.result?.forEach { quote ->
                val internalSymbol = fromYahooSymbol(quote.symbol)
                val newPrice = quote.regularMarketPrice
                
                if (newPrice != null) {
                    val oldPrice = current[internalSymbol] ?: newPrice
                    
                    // Determine up or down
                    if (newPrice != oldPrice) {
                        directions[internalSymbol] = newPrice > oldPrice
                    }
                    current[internalSymbol] = newPrice
                }
            }
            
            _livePrices.value = current
            _priceDirections.value = directions
            
        } catch (e: Exception) {
            e.printStackTrace()
            // On failure, maybe do nothing and retain old prices
        }
    }

    // Live Feed Simulation Trigger (Fallback)
    fun simulatePriceTicks() {
        val current = _livePrices.value.toMutableMap()
        val directions = _priceDirections.value.toMutableMap()
        val random = Random()

        current.forEach { (symbol, price) ->
            val volatility = getVolatilityForSymbol(symbol)
            // Determine up or down
            val isUp = random.nextBoolean()
            val changePercent = random.nextDouble() * volatility * 0.2 // Small tick increments
            val changeValue = price * changePercent
            
            val newPrice = if (isUp) {
                price + changeValue
            } else {
                price - changeValue
            }

            // Format appropriately to keep decimal places looking clean (especially Forex)
            val roundedPrice = if (symbol == "EURUSD") {
                Math.round(newPrice * 10000.0) / 10000.0
            } else {
                Math.round(newPrice * 100.0) / 100.0
            }

            current[symbol] = roundedPrice
            directions[symbol] = isUp
        }

        _livePrices.value = current
        _priceDirections.value = directions
    }

    private fun getVolatilityForSymbol(symbol: String): Double {
        return when (symbol.uppercase()) {
            "BTCUSD", "ETHUSD", "SOLUSD" -> 0.015 // Crypto high
            "TSLA", "NVDA" -> 0.012               // Volatile tech stocks
            "RELIANCE", "TCS", "HDFCBANK", "INFY" -> 0.008 // Indian stocks
            "EURUSD" -> 0.001                     // Forex tiny volatility
            else -> 0.006                         // Typical stock stability
        }
    }

    // Flow Accessors
    fun getWatchlist(): Flow<List<WatchlistItem>> = dao.getWatchlistFlow()
    fun getPortfolioState(): Flow<PortfolioState?> = dao.getPortfolioStateFlow()
    fun getPositions(): Flow<List<DatabasePosition>> = dao.getPositionsFlow()
    fun getOrderHistory(): Flow<List<DatabaseOrder>> = dao.getOrderHistoryFlow()
    fun getTrendlines(symbol: String): Flow<List<DatabaseTrendline>> = dao.getTrendlinesFlow(symbol)

    // DB Modifications
    suspend fun addWatchlistItem(symbol: String, name: String, initialPrice: Double, assetType: String) {
        dao.insertWatchlistItem(WatchlistItem(symbol, name, true, initialPrice, assetType))
        
        // Register in-memory live price too
        val current = _livePrices.value.toMutableMap()
        current[symbol] = initialPrice
        _livePrices.value = current
    }

    suspend fun removeWatchlistItem(symbol: String) {
        dao.deleteWatchlistItem(symbol)
    }

    suspend fun getPosition(symbol: String): DatabasePosition? {
        return dao.getPositionSync(symbol)
    }

    suspend fun updatePositionSLTP(symbol: String, stopLoss: Double?, takeProfit: Double?) {
        val existing = dao.getPositionSync(symbol)
        if (existing != null) {
            dao.insertPosition(existing.copy(stopLoss = stopLoss, takeProfit = takeProfit))
        }
    }

    suspend fun getPositionsListSync(): List<DatabasePosition> {
        return dao.getPositionsFlow().firstOrNull() ?: emptyList()
    }

    fun getDefaultItem(symbol: String): WatchlistItem? {
        return defaultWatchlist.find { it.symbol.equals(symbol, ignoreCase = true) }
    }

    suspend fun addTrendline(trendline: DatabaseTrendline) {
        dao.insertTrendline(trendline)
    }

    suspend fun removeTrendline(id: Int) {
        dao.deleteTrendline(id)
    }

    suspend fun clearTrendlines(symbol: String) {
        dao.clearTrendlines(symbol)
    }

    // Trade operations
    suspend fun buyAsset(symbol: String, shares: Double): String {
        if (shares <= 0) return "Invalid share quantity"
        val price = _livePrices.value[symbol] ?: return "Symbol price not available"
        val totalCost = price * shares

        val portfolio = dao.getPortfolioStateSync() ?: return "Portfolio balance missing"
        if (portfolio.cash < totalCost) {
            return "Insufficient funds. Required: $${String.format("%,.2f", totalCost)}, Available: $${String.format("%,.2f", portfolio.cash)}"
        }

        // Deduct Cash
        val updatedCash = portfolio.cash - totalCost
        dao.insertPortfolioState(PortfolioState(id = 1, cash = updatedCash))

        // Update Position
        val existingPosition = dao.getPositionSync(symbol)
        if (existingPosition == null) {
            dao.insertPosition(DatabasePosition(symbol, shares, price))
        } else {
            val totalShares = existingPosition.shares + shares
            val newAvgPrice = ((existingPosition.shares * existingPosition.averageEntryPrice) + totalCost) / totalShares
            dao.insertPosition(DatabasePosition(symbol, totalShares, newAvgPrice))
        }

        // Add transaction log
        dao.insertOrder(DatabaseOrder(symbol = symbol, type = "BUY", price = price, shares = shares))

        return "Successfully purchased $shares shares of $symbol at $${String.format("%,.4f" , price)}"
    }

    suspend fun sellAsset(symbol: String, shares: Double): String {
        if (shares <= 0) return "Invalid share quantity"
        val price = _livePrices.value[symbol] ?: return "Symbol price not available"
        val totalProceeds = price * shares

        val existingPosition = dao.getPositionSync(symbol) ?: return "You don't own any shares of $symbol"
        if (existingPosition.shares < shares) {
            return "Insufficient shares. You own ${existingPosition.shares}, requested $shares"
        }

        // Update Portfolio State
        val portfolio = dao.getPortfolioStateSync() ?: return "Portfolio balance missing"
        val updatedCash = portfolio.cash + totalProceeds
        dao.insertPortfolioState(PortfolioState(id = 1, cash = updatedCash))

        // Update Position
        val remainingShares = existingPosition.shares - shares
        if (remainingShares <= 1e-6) {
            dao.deletePosition(symbol)
        } else {
            dao.insertPosition(DatabasePosition(symbol, remainingShares, existingPosition.averageEntryPrice))
        }

        // Add transaction log
        dao.insertOrder(DatabaseOrder(symbol = symbol, type = "SELL", price = price, shares = shares))

        return "Successfully sold $shares shares of $symbol at $${String.format("%,.4f" , price)}"
    }

    // Historical Candlestick Generation
    fun generateHistoricalCandles(symbol: String, timeframe: String, count: Int = 100): List<CandlePoint> {
        val seed = (symbol + timeframe).hashCode().toLong()
        val random = Random(seed)

        var price = when (symbol.uppercase()) {
            "BTCUSD" -> 68500.0
            "ETHUSD" -> 3450.0
            "SOLUSD" -> 145.5
            "AAPL" -> 180.25
            "TSLA" -> 175.8
            "NVDA" -> 950.4
            "AMZN" -> 182.1
            "MSFT" -> 420.5
            "GOOGL" -> 175.2
            "RELIANCE" -> 2900.0
            "TCS" -> 3800.0
            "HDFCBANK" -> 1500.0
            "INFY" -> 1400.0
            "EURUSD" -> 1.0850
            else -> 100.0
        }

        val intervalMs = when (timeframe) {
            "1M" -> 60_000L
            "5M" -> 300_000L
            "15M" -> 900_000L
            "1H" -> 3_600_000L
            "4H" -> 14_400_000L
            "1D" -> 86_400_000L
            "1W" -> 604_800_000L
            "1Mo" -> 2_592_000_000L // 30 days
            else -> 86_400_000L
        }

        var time = System.currentTimeMillis() - (count * intervalMs)
        val candles = mutableListOf<CandlePoint>()
        val volatility = getVolatilityForSymbol(symbol)

        for (i in 0 until count) {
            // Drift slightly up based on index to simulate positive market trend
            val trendDrift = (random.nextDouble() - 0.485) * volatility
            val open = price
            val close = open * (1 + trendDrift)
            val high = maxOf(open, close) * (1 + random.nextDouble() * volatility * 0.4)
            val low = minOf(open, close) * (1 - random.nextDouble() * volatility * 0.4)
            val volume = 1000 + random.nextInt(9000).toDouble() * (price / 50.0)

            candles.add(CandlePoint(time, open, high, low, close, volume))
            price = close
            time += intervalMs
        }
        return candles
    }
}
