package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TradingViewWidget(
    symbol: String, 
    interval: String = "D", 
    showSMA: Boolean = false,
    showEMA: Boolean = false,
    showRSI: Boolean = false,
    showUTBot: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = com.example.ui.theme.isDarkThemeEnabled.value
    val themeStr = if (isDark) "dark" else "light"
    val bgColor = if (isDark) "#000000" else "#FAFAFA"
    
    // Map common timeframes to TradingView intervals
    val tvInterval = when (interval) {
        "30S" -> "30S"
        "1M" -> "1"
        "5M" -> "5"
        "15M" -> "15"
        "1H" -> "60"
        "4H" -> "240"
        "1D" -> "D"
        "1W" -> "W"
        "1Mo" -> "M"
        else -> "D"
    }
    
    val studiesList = buildList {
        if (showSMA) add("\"MASimple@tv-basicstudies\"")
        if (showEMA) add("\"MAExp@tv-basicstudies\"")
        if (showRSI) add("\"RSI@tv-basicstudies\"")
        if (showUTBot) add("\"SuperTrend@tv-basicstudies\"")
    }.joinToString(",")
    
    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
        <style>
            html, body { margin: 0; padding: 0; height: 100vh; width: 100vw; background-color: $bgColor; overflow: hidden; }
            .tradingview-widget-container { width: 100vw; height: 100vh; }
            #tradingview_chart { position: relative; top: -65px; height: calc(100vh + 65px) !important; width: 100vw; }
            iframe { height: calc(100vh + 65px) !important; }
        </style>
        </head>
        <body>
        <!-- TradingView Widget BEGIN -->
        <div class="tradingview-widget-container" style="height:100vh;width:100vw;overflow:hidden;">
          <div id="tradingview_chart"></div>
          <script type="text/javascript" src="https://s3.tradingview.com/tv.js"></script>
          <script type="text/javascript">
          new TradingView.widget(
          {
          "autosize": true,
          "symbol": "$symbol",
          "interval": "$tvInterval",
          "timezone": "Etc/UTC",
          "theme": "$themeStr",
          "style": "1",
          "locale": "en",
          "enable_publishing": false,
          "backgroundColor": "$bgColor",
          "gridColor": "${if (isDark) "#333333" else "#E0E0E0"}",
          "withdateranges": true,
          "hide_side_toolbar": true,
          "allow_symbol_change": true,
          "details": false,
          "hotlist": false,
          "calendar": false,
          "hide_top_toolbar": true,
          "hide_legend": true,
          "save_image": false,
          "session": "extended",
          "show_countdown": true,
          "range": "${if (interval in listOf("1M", "5M", "15M", "1H", "4H")) "5D" else "12M"}",
          "studies": [$studiesList],
          "show_popup_button": false,
          "container_id": "tradingview_chart"
        }
          );
          </script>
        </div>
        <!-- TradingView Widget END -->
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                loadDataWithBaseURL("https://www.tradingview.com/", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://www.tradingview.com/", htmlContent, "text/html", "UTF-8", null)
        }
    )
}
