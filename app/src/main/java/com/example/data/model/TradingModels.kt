package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist_items")
data class WatchlistItem(
    @PrimaryKey val symbol: String,
    val name: String,
    val isCustom: Boolean = false,
    val initialPrice: Double = 100.0,
    val assetType: String = "Stock" // "Stock", "Crypto", "Forex"
)

@Entity(tableName = "portfolio_state")
data class PortfolioState(
    @PrimaryKey val id: Int = 1, // Only 1 row ever
    val cash: Double = 100000.0
)

@Entity(tableName = "positions")
data class DatabasePosition(
    @PrimaryKey val symbol: String,
    val shares: Double,
    val averageEntryPrice: Double,
    val stopLoss: Double? = null,
    val takeProfit: Double? = null
)

@Entity(tableName = "order_history")
data class DatabaseOrder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val type: String, // "BUY" or "SELL"
    val price: Double,
    val shares: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chart_trendlines")
data class DatabaseTrendline(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val startXRatio: Float, // Relative X coordinate (0.0f to 1.0f) in local viewport OR relative index
    val startPrice: Double,  // Absolute price to keep it pinned to relative coordinates perfectly
    val endXRatio: Float,
    val endPrice: Double,
    val isHorizontal: Boolean = false,
    val colorHex: String = "#FF9800" // Light Orange
)

// Memory model representing full live state for charting
data class CandlePoint(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)
