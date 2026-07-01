package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.TradeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TradingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = TradeRepository(db.tradingDao())

    // Selection States
    private val _selectedSymbol = MutableStateFlow("BTCUSD")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    private val _selectedTimeframe = MutableStateFlow("1D")
    val selectedTimeframe: StateFlow<String> = _selectedTimeframe.asStateFlow()

    // Live Candlestick State for the active chart
    private val _candles = MutableStateFlow<List<CandlePoint>>(emptyList())
    val candles: StateFlow<List<CandlePoint>> = _candles.asStateFlow()

    // Indicators toggles
    private val _showSMA = MutableStateFlow(true)
    val showSMA: StateFlow<Boolean> = _showSMA.asStateFlow()

    private val _showEMA = MutableStateFlow(false)
    val showEMA: StateFlow<Boolean> = _showEMA.asStateFlow()

    private val _showRSI = MutableStateFlow(false)
    val showRSI: StateFlow<Boolean> = _showRSI.asStateFlow()

    private val _showUTBot = MutableStateFlow(false)
    val showUTBot: StateFlow<Boolean> = _showUTBot.asStateFlow()

    // Active Drawing Tool: "None", "Horizontal", "TrendLine"
    private val _activeDrawingTool = MutableStateFlow("None")
    val activeDrawingTool: StateFlow<String> = _activeDrawingTool.asStateFlow()

    // Hover or Drag Crosshair Coordinate state
    private val _crosshairPoint = MutableStateFlow<CandlePoint?>(null)
    val crosshairPoint: StateFlow<CandlePoint?> = _crosshairPoint.asStateFlow()

    // Watchlist Flow combined with live price ticks
    val watchlist: StateFlow<List<WatchlistItemWithPrice>> = combine(
        repository.getWatchlist(),
        repository.livePricesFlow,
        repository.priceDirectionsFlow
    ) { items, livePrices, directions ->
        items.map { item ->
            WatchlistItemWithPrice(
                item = item,
                price = livePrices[item.symbol] ?: item.initialPrice,
                isUpTick = directions[item.symbol] ?: true
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Portfolio flows
    val portfolioState: StateFlow<PortfolioState?> = repository.getPortfolioState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Positions merged with live prices for dynamic valuations
    val positions: StateFlow<List<PositionWithValuation>> = combine(
        repository.getPositions(),
        repository.livePricesFlow
    ) { openPositions, prices ->
        openPositions.map { pos ->
            val curPrice = prices[pos.symbol] ?: pos.averageEntryPrice
            val unrealizedPL = (curPrice - pos.averageEntryPrice) * pos.shares
            val percentChange = ((curPrice - pos.averageEntryPrice) / pos.averageEntryPrice) * 100.0
            PositionWithValuation(
                position = pos,
                currentPrice = curPrice,
                unrealizedPL = unrealizedPL,
                unrealizedPLPercent = percentChange
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Order logs
    val orderHistory: StateFlow<List<DatabaseOrder>> = repository.getOrderHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saved trendlines on the active symbol
    val activeTrendlines: StateFlow<List<DatabaseTrendline>> = _selectedSymbol.flatMapLatest { symbol ->
        repository.getTrendlines(symbol)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic Portfolio Summary
    val portfolioSummary: StateFlow<PortfolioSummaryState> = combine(
        portfolioState,
        positions
    ) { profile, holdings ->
        val cash = profile?.cash ?: 100000.0
        val holdingsValue = holdings.sumOf { it.sharesValuation }
        val totalEquity = cash + holdingsValue
        val totalCost = holdings.sumOf { it.position.shares * it.position.averageEntryPrice }
        val overallPL = totalEquity - 100000.0 // Assuming initial start was 100k
        val overallPLPercent = (overallPL / 100000.0) * 100.0

        PortfolioSummaryState(
            cash = cash,
            holdingsValue = holdingsValue,
            totalEquity = totalEquity,
            overallPL = overallPL,
            overallPLPercent = overallPLPercent
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PortfolioSummaryState())

    // Active notifications/toasts
    private val _notification = MutableSharedFlow<String>()
    val notification: SharedFlow<String> = _notification.asSharedFlow()

    // Background jobs
    private var simulatorJob: Job? = null

    init {
        viewModelScope.launch {
            // First run setups
            repository.initializeDatabaseIfNeeded()
            // Pull base candles for default selection
            loadBaseCandles(_selectedSymbol.value, _selectedTimeframe.value)
            // Start background ticks
            startTickSimulation()
        }

        // Listen for price updates to update the active candle on chart and check Stop Loss/Take Profit
        viewModelScope.launch {
            repository.livePricesFlow.collect { prices ->
                val activeSym = _selectedSymbol.value
                val curPrice = prices[activeSym]
                
                // 1. Check Stop Loss and Take Profit triggers for all active positions
                val activePositions = repository.getPositionsListSync()
                activePositions.forEach { pos ->
                    val price = prices[pos.symbol]
                    if (price != null) {
                        val sl = pos.stopLoss
                        val tp = pos.takeProfit
                        if (sl != null && sl > 0.0 && price <= sl) {
                            viewModelScope.launch {
                                val msg = repository.sellAsset(pos.symbol, pos.shares)
                                _notification.emit("[Stop Loss Triggered] Sold ${pos.shares} of ${pos.symbol} at $price: $msg")
                            }
                        } else if (tp != null && tp > 0.0 && price >= tp) {
                            viewModelScope.launch {
                                val msg = repository.sellAsset(pos.symbol, pos.shares)
                                _notification.emit("[Take Profit Triggered] Sold ${pos.shares} of ${pos.symbol} at $price: $msg")
                            }
                        }
                    }
                }

                // 2. Update the active candle on chart
                if (curPrice != null && _candles.value.isNotEmpty()) {
                    val list = _candles.value.toMutableList()
                    val lastIdx = list.size - 1
                    val lastCandle = list[lastIdx]

                    val timeframe = _selectedTimeframe.value
                    val intervalMs = when (timeframe) {
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

                    val currentTime = System.currentTimeMillis()
                    if (currentTime >= lastCandle.timestamp + intervalMs) {
                        // Create a new candle
                        val newCandle = com.example.data.model.CandlePoint(
                            timestamp = lastCandle.timestamp + intervalMs,
                            open = lastCandle.close,
                            high = maxOf(lastCandle.close, curPrice),
                            low = minOf(lastCandle.close, curPrice),
                            close = curPrice,
                            volume = 0.0 // Start with 0 volume
                        )
                        list.add(newCandle)
                        if (list.size > 100) {
                            list.removeAt(0) // Keep the list size bounded
                        }
                    } else {
                        // Update last candle close with the live price
                        val updatedHigh = maxOf(lastCandle.high, curPrice)
                        val updatedLow = minOf(lastCandle.low, curPrice)
                        list[lastIdx] = lastCandle.copy(
                            close = curPrice,
                            high = updatedHigh,
                            low = updatedLow
                        )
                    }
                    _candles.value = list
                }
            }
        }
    }

    private fun loadBaseCandles(symbol: String, timeframe: String) {
        val series = repository.generateHistoricalCandles(symbol, timeframe, count = 80)
        _candles.value = series
    }

    private fun startTickSimulation() {
        simulatorJob?.cancel()
        simulatorJob = viewModelScope.launch {
            while (true) {
                try {
                    repository.fetchRealTimePrices()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5000) // Poll every 5 seconds for real-time prices
            }
        }
    }

    fun selectSymbol(symbol: String) {
        _selectedSymbol.value = symbol
        loadBaseCandles(symbol, _selectedTimeframe.value)
        _crosshairPoint.value = null
    }

    fun selectTimeframe(timeframe: String) {
        _selectedTimeframe.value = timeframe
        loadBaseCandles(_selectedSymbol.value, timeframe)
        _crosshairPoint.value = null
    }

    fun setCrosshairPoint(point: CandlePoint?) {
        _crosshairPoint.value = point
    }

    fun toggleSMA() {
        _showSMA.value = !_showSMA.value
    }

    fun toggleEMA() {
        _showEMA.value = !_showEMA.value
    }

    fun toggleRSI() {
        _showRSI.value = !_showRSI.value
    }

    fun toggleUTBot() {
        _showUTBot.value = !_showUTBot.value
    }

    fun changeDrawingTool(tool: String) {
        _activeDrawingTool.value = tool
    }

    fun addSymbolToWatchlist(symbol: String) {
        viewModelScope.launch {
            val upperSym = symbol.uppercase()
            val exists = repository.getWatchlist().firstOrNull()?.any { it.symbol == upperSym } ?: false
            if (exists) {
                _notification.emit("$upperSym is already in your Watchlist")
                return@launch
            }
            
            val defaultItem = repository.getDefaultItem(upperSym)
            val name = defaultItem?.name ?: upperSym
            val assetType = defaultItem?.assetType ?: when {
                upperSym.startsWith("NSE:") -> "Stock"
                upperSym.startsWith("MCX:") -> "Commodity"
                upperSym.contains("USD") && upperSym.length == 6 -> "Forex"
                upperSym.endsWith("USD") -> "Crypto"
                else -> "Stock"
            }
            
            val initialPrice = repository.livePricesFlow.value[upperSym] ?: 100.0
            repository.addWatchlistItem(upperSym, name, initialPrice, assetType)
            _notification.emit("Added $upperSym to Watchlist")
        }
    }

    fun setStopLossAndTakeProfit(symbol: String, stopLoss: Double?, takeProfit: Double?) {
        viewModelScope.launch {
            repository.updatePositionSLTP(symbol, stopLoss, takeProfit)
            _notification.emit("Updated SL/TP for $symbol")
        }
    }

    // Database Actions wrapped in VM launching
    fun addCustomTicker(symbol: String, name: String, initialPrice: Double, assetType: String) {
        viewModelScope.launch {
            if (symbol.isBlank() || name.isBlank()) {
                _notification.emit("Symbol and company name cannot be empty")
                return@launch
            }
            repository.addWatchlistItem(symbol.uppercase(), name, initialPrice, assetType)
            _notification.emit("Added ${symbol.uppercase()} to Watchlist")
        }
    }

    fun deleteTicker(symbol: String) {
        viewModelScope.launch {
            repository.removeWatchlistItem(symbol)
            _notification.emit("Removed $symbol from Watchlist")
            // If deleting active symbol, fall back to BTCUSD
            if (_selectedSymbol.value == symbol) {
                selectSymbol("BTCUSD")
            }
        }
    }

    fun recordTrendline(startXRatio: Float, startPrice: Double, endXRatio: Float, endPrice: Double, isHoriz: Boolean) {
        viewModelScope.launch {
            val trend = DatabaseTrendline(
                symbol = _selectedSymbol.value,
                startXRatio = startXRatio,
                startPrice = startPrice,
                endXRatio = endXRatio,
                endPrice = endPrice,
                isHorizontal = isHoriz
            )
            repository.addTrendline(trend)
            _notification.emit("Drawing preserved on ${_selectedSymbol.value}")
        }
    }

    fun clearAllTrendlines() {
        viewModelScope.launch {
            repository.clearTrendlines(_selectedSymbol.value)
            _notification.emit("Cleared all drawings on ${_selectedSymbol.value}")
        }
    }

    fun deleteTrendline(id: Int) {
        viewModelScope.launch {
            repository.removeTrendline(id)
        }
    }

    fun executePaperTrade(type: String, shares: Double) {
        viewModelScope.launch {
            val result = if (type == "BUY") {
                repository.buyAsset(_selectedSymbol.value, shares)
            } else {
                repository.sellAsset(_selectedSymbol.value, shares)
            }
            _notification.emit(result)
        }
    }

    fun executeSellDirect(symbol: String, shares: Double) {
        viewModelScope.launch {
            val result = repository.sellAsset(symbol, shares)
            _notification.emit(result)
        }
    }

    override fun onCleared() {
        super.onCleared()
        simulatorJob?.cancel()
    }
}

// Presentation Models
data class WatchlistItemWithPrice(
    val item: WatchlistItem,
    val price: Double,
    val isUpTick: Boolean
)

data class PositionWithValuation(
    val position: DatabasePosition,
    val currentPrice: Double,
    val unrealizedPL: Double,
    val unrealizedPLPercent: Double
) {
    val sharesValuation: Double
        get() = position.shares * currentPrice
}

data class PortfolioSummaryState(
    val cash: Double = 100000.0,
    val holdingsValue: Double = 0.0,
    val totalEquity: Double = 100000.0,
    val overallPL: Double = 0.0,
    val overallPLPercent: Double = 0.0
)
