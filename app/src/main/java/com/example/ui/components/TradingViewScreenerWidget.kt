package com.example.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.isDarkThemeEnabled

@Composable
fun TradingViewScreenerWidget(
    market: String = "india",
    onSymbolClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isDarkThemeEnabled.value
    val theme = if (isDark) "dark" else "light"
    val bgColor = if (isDark) "#121212" else "#FFFFFF"
    
    val actualMarket = when (market) {
        "nse", "bse", "mcx" -> "india"
        else -> market
    }
    
    val exchangeFilter = when (market) {
        "nse" -> "NSE"
        "bse" -> "BSE"
        "mcx" -> "MCX"
        else -> ""
    }

    val htmlData = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
        <style>
            body, html { margin: 0; padding: 0; width: 100%; height: 100%; background-color: $bgColor; overflow: hidden; }
            .tradingview-widget-container { height: 100%; width: 100%; }
        </style>
        </head>
        <body>
        <!-- TradingView Widget BEGIN -->
        <div class="tradingview-widget-container" style="height:100%;width:100%">
          <div class="tradingview-widget-container__widget" style="height:calc(100% - 32px);width:100%"></div>
          <script type="text/javascript" src="https://s3.tradingview.com/external-embedding/embed-widget-screener.js" async>
          {
            "width": "100%",
            "height": "100%",
            "defaultColumn": "overview",
            "defaultScreen": "general",
            "market": "${actualMarket}",
            ${if (exchangeFilter.isNotEmpty()) "\"exchange\": \"$exchangeFilter\"," else ""}
            "showToolbar": true,
            "colorTheme": "$theme",
            "locale": "en"
          }
          </script>
        </div>
        <!-- TradingView Widget END -->
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                
                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                        android.util.Log.d("WebViewWidget", message?.message() ?: "")
                        return true
                    }
                }
                
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: ""
                        if (url.contains("/symbols/")) {
                            val segments = url.split("/symbols/")
                            if (segments.size > 1) {
                                val symbolPart = segments[1].trim('/').split("/")[0]
                                // symbolPart is like NASDAQ-AAPL or CRYPTO-BTCUSD or just AAPL
                                val cleanSymbol = symbolPart.replace("-", ":")
                                onSymbolClick?.invoke(cleanSymbol)
                                return true
                            }
                        }
                        return false
                    }
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                    }
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://www.tradingview.com/", htmlData, "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}
