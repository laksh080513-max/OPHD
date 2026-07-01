package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TradingDao {
    // Watchlist
    @Query("SELECT * FROM watchlist_items")
    fun getWatchlistFlow(): Flow<List<WatchlistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlistItem(item: WatchlistItem)

    @Query("DELETE FROM watchlist_items WHERE symbol = :symbol")
    suspend fun deleteWatchlistItem(symbol: String)

    // Portfolio State
    @Query("SELECT * FROM portfolio_state WHERE id = 1")
    fun getPortfolioStateFlow(): Flow<PortfolioState?>

    @Query("SELECT * FROM portfolio_state WHERE id = 1")
    suspend fun getPortfolioStateSync(): PortfolioState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortfolioState(state: PortfolioState)

    // Positions (Holdings)
    @Query("SELECT * FROM positions")
    fun getPositionsFlow(): Flow<List<DatabasePosition>>

    @Query("SELECT * FROM positions WHERE symbol = :symbol")
    suspend fun getPositionSync(symbol: String): DatabasePosition?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: DatabasePosition)

    @Query("DELETE FROM positions WHERE symbol = :symbol")
    suspend fun deletePosition(symbol: String)

    // Orders
    @Query("SELECT * FROM order_history ORDER BY timestamp DESC")
    fun getOrderHistoryFlow(): Flow<List<DatabaseOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: DatabaseOrder)

    // Trendlines
    @Query("SELECT * FROM chart_trendlines WHERE symbol = :symbol")
    fun getTrendlinesFlow(symbol: String): Flow<List<DatabaseTrendline>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrendline(trendline: DatabaseTrendline)

    @Query("DELETE FROM chart_trendlines WHERE id = :id")
    suspend fun deleteTrendline(id: Int)

    @Query("DELETE FROM chart_trendlines WHERE symbol = :symbol")
    suspend fun clearTrendlines(symbol: String)
}
