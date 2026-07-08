package com.example.data.model

import java.util.Calendar
import java.util.TimeZone

data class MarketStatus(
    val isOpen: Boolean,
    val description: String,
    val operatingHours: String
)

fun getCurrencySymbol(symbol: String?): String {
    if (symbol == null) return "₹" // Default to Rupees for portfolio cash in Indian context
    val s = symbol.uppercase()
    return if (s.startsWith("NSE:") || s.startsWith("MCX:") || s.contains("RELIANCE") || s.contains("TCS") || s.contains("HDFCBANK") || s.contains("INFY") || s.contains("SBIN") || s.contains("TATAMOTORS") || s.endsWith(".NS") || s.endsWith(".BO")) "₹" else "$"
}

fun Double.formatPrice(symbol: String?): String {
    val currency = getCurrencySymbol(symbol)
    val format = if (currency == "$") {
        if (this < 2.0 && symbol != null && (symbol.contains("EURUSD") || symbol.contains("Forex"))) "%,.4f" else "%,.2f"
    } else {
        "%,.2f"
    }
    return "$currency${String.format(format, this)}"
}

fun getMarketStatus(symbol: String): MarketStatus {
    val sym = symbol.uppercase()
    
    // 1. Crypto is always open (24/7)
    if (sym == "BTCUSD" || sym == "ETHUSD" || sym == "SOLUSD" || sym.contains("BTC") || sym.contains("ETH") || sym.contains("SOL")) {
        return MarketStatus(
            isOpen = true,
            description = "Crypto Market - ACTIVE",
            operatingHours = "Always Open (24/7)"
        )
    }
    
    // 2. Indian markets (NSE & MCX)
    if (sym.startsWith("NSE:") || sym.startsWith("MCX:") || sym.contains("RELIANCE") || sym.contains("TCS") || sym.contains("HDFCBANK") || sym.contains("INFY") || sym.contains("SBIN") || sym.contains("TATAMOTORS")) {
        // India Timezone is GMT+5:30
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+05:30"))
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        
        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        
        if (sym.startsWith("MCX:")) {
            // MCX commodities open weekdays 9:00 AM to 11:30 PM (23:30)
            val isWeekdayHours = !isWeekend && (hour > 9 || (hour == 9 && minute >= 0)) && (hour < 23 || (hour == 23 && minute <= 30))
            return MarketStatus(
                isOpen = isWeekdayHours,
                description = if (isWeekdayHours) "MCX Commodities - ACTIVE" else "MCX - CLOSED",
                operatingHours = "Mon-Fri: 9:00 AM - 11:30 PM IST"
            )
        } else {
            // NSE stocks open weekdays 9:15 AM to 3:30 PM (15:30)
            val hourMin = hour * 100 + minute
            val isWeekdayHours = !isWeekend && (hourMin >= 915 && hourMin <= 1530)
            return MarketStatus(
                isOpen = isWeekdayHours,
                description = if (isWeekdayHours) "NSE India - ACTIVE" else "NSE - CLOSED",
                operatingHours = "Mon-Fri: 9:15 AM - 3:30 PM IST"
            )
        }
    }
    
    // 3. Forex (EURUSD)
    if (sym == "EURUSD" || sym.contains("FOREX")) {
        // Forex is open 24 hours a day on weekdays. Closes Friday 5 PM EST, opens Sunday 5 PM EST.
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-05:00"))
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        
        val isClosed = (dayOfWeek == Calendar.FRIDAY && hour >= 17) ||
                       (dayOfWeek == Calendar.SATURDAY) ||
                       (dayOfWeek == Calendar.SUNDAY && hour < 17)
        
        return MarketStatus(
            isOpen = !isClosed,
            description = if (!isClosed) "Forex - ACTIVE" else "Forex - CLOSED",
            operatingHours = "24/5 (Sun 5 PM - Fri 5 PM EST)"
        )
    }
    
    // 4. US Stocks (AAPL, TSLA, NVDA, AMZN, MSFT, GOOGL, etc.)
    // US Stocks open weekdays 9:30 AM to 4:00 PM EST (16:00)
    val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-05:00"))
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    
    val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    val hourMin = hour * 100 + minute
    val isWeekdayHours = !isWeekend && (hourMin >= 930 && hourMin <= 1600)
    
    return MarketStatus(
        isOpen = isWeekdayHours,
        description = if (isWeekdayHours) "US Stock Market - ACTIVE" else "US Market - CLOSED",
        operatingHours = "Mon-Fri: 9:30 AM - 4:00 PM EST"
    )
}
