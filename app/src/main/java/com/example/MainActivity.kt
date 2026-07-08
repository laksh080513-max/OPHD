package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
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
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradingViewModel
import com.example.data.model.formatPrice
import com.example.data.model.getMarketStatus
import com.example.data.model.getCurrencySymbol
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.flow.collectLatest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

fun handleGoogleSignIn(context: android.content.Context, scope: kotlinx.coroutines.CoroutineScope, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val credentialManager = CredentialManager.create(context)
    
    // Replace with your Web Client ID
    val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID_HERE.apps.googleusercontent.com"
    
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(WEB_CLIENT_ID)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )
            onSuccess()
        } catch (e: GetCredentialException) {
            onError("Google Sign In failed. Please configure your Web Client ID.")
        } catch (e: Exception) {
            onError("Error: ${e.message}")
        }
    }
}

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

data class NewsItem(val title: String, val source: String, val symbol: String, val time: String)

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
    val smaPeriod by viewModel.smaPeriod.collectAsState()
    val emaPeriod by viewModel.emaPeriod.collectAsState()
    val rsiPeriod by viewModel.rsiPeriod.collectAsState()
    val utBotSensitivity by viewModel.utBotSensitivity.collectAsState()
    val activeDrawingTool by viewModel.activeDrawingTool.collectAsState()
    val crosshairPoint by viewModel.crosshairPoint.collectAsState()
    val activeTrendlines by viewModel.activeTrendlines.collectAsState()
    val portfolioSummary by viewModel.portfolioSummary.collectAsState()
    val positions by viewModel.positions.collectAsState()
    val orderHistory by viewModel.orderHistory.collectAsState()
    val livePrices by viewModel.livePrices.collectAsState()

    // Pine editor state
    var showPineEditor by remember { mutableStateOf(false) }
    var aiFilterSelected by remember { mutableStateOf(false) }
    var filterSaved by remember { mutableStateOf(false) }
    var isChartFullscreen by remember { mutableStateOf(false) }
    var chartType by remember { mutableStateOf("NATIVE") } // "NATIVE" or "TRADINGVIEW"

    // Find current selected price
    val activePrice = remember(livePrices, watchlist, selectedSymbol) {
        livePrices[selectedSymbol] ?: watchlist.find { it.item.symbol == selectedSymbol }?.price ?: 100.0
    }

    var backStack by remember { mutableStateOf(listOf("HOME")) }
    val currentScreen = backStack.last()
    
    val navigateTo: (String) -> Unit = { screen ->
        if (backStack.last() != screen) {
            val newStack = backStack.toMutableList()
            newStack.remove(screen)
            newStack.add(screen)
            backStack = newStack
        }
    }
    
    val navigateBack: () -> Unit = {
        if (backStack.size > 1) {
            backStack = backStack.dropLast(1)
        }
    }
    var isBooting by remember { mutableStateOf(true) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var showSignInDialog by remember { mutableStateOf(false) }
    var showProDialog by remember { mutableStateOf(false) }
    var showIndicatorSettingsDialog by remember { mutableStateOf(false) }
    var proDialogFeatureName by remember { mutableStateOf("") }
    
    // Dynamic News & Refresh Timer based on Room/Pro membership status
    val newsPools = remember {
        listOf(
            listOf(
                NewsItem("Apple's new product launch creates buzz", "Financial Times", "AAPL", "2h ago"),
                NewsItem("Tesla deliveries beat expectations in Q3", "Bloomberg", "TSLA", "4h ago"),
                NewsItem("Microsoft announces new AI integrations", "Reuters", "MSFT", "5h ago"),
                NewsItem("Reliance shares hit new 52-week high", "Economic Times", "NSE:RELIANCE", "6h ago"),
                NewsItem("Nvidia earnings report scheduled for next week", "CNBC", "NVDA", "8h ago")
            ),
            listOf(
                NewsItem("Federal Reserve holds interest rates steady", "Wall Street Journal", "SPY", "15m ago"),
                NewsItem("Google unveils breakthrough quantum computing processor", "Wired", "GOOGL", "1h ago"),
                NewsItem("Bitcoin surges past key resistance level above $70k", "CoinDesk", "BTCUSD", "2h ago"),
                NewsItem("Crude oil prices drop as global supplies hit record high", "Reuters", "USO", "3h ago"),
                NewsItem("Amazon rolls out multi-billion dollar logistics expansion", "Bloomberg", "AMZN", "5h ago")
            ),
            listOf(
                NewsItem("Meta stock climbs to all-time high on advertising surge", "CNBC", "META", "5m ago"),
                NewsItem("NIFTY 50 consolidated near 24,000 amid banking support", "Moneycontrol", "NIFTY_50", "45m ago"),
                NewsItem("AMD launches next-generation Ryzen AI laptop chips", "TechCrunch", "AMD", "2h ago"),
                NewsItem("Berkshire Hathaway builds cash pile to record $189B", "Omaha Herald", "BRK.B", "4h ago"),
                NewsItem("Ethereum gas fees drop to multi-year lows", "Cointelegraph", "ETHUSD", "6h ago")
            )
        )
    }
    
    val isProUserActive = com.example.ui.theme.isProUserEnabled.value
    var currentNewsPoolIndex by remember { mutableStateOf(0) }
    var secondsRemaining by remember { mutableStateOf(if (isProUserActive) 1800 else 3600) }
    
    // Automatically cap or adapt timer when user's membership status changes
    LaunchedEffect(isProUserActive) {
        val maxSec = if (isProUserActive) 1800 else 3600
        if (secondsRemaining > maxSec) {
            secondsRemaining = maxSec
        }
    }
    
    // Reactive countdown timer that ticks every second and rotates the news pool at 0
    LaunchedEffect(secondsRemaining) {
        kotlinx.coroutines.delay(1000L)
        val maxSec = if (com.example.ui.theme.isProUserEnabled.value) 1800 else 3600
        if (secondsRemaining > maxSec) {
            secondsRemaining = maxSec
        }
        if (secondsRemaining > 0) {
            secondsRemaining--
        } else {
            currentNewsPoolIndex = (currentNewsPoolIndex + 1) % newsPools.size
            secondsRemaining = maxSec
        }
    }
    var showEmailForm by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    
    androidx.activity.compose.BackHandler(enabled = backStack.size > 1) {
        navigateBack()
    }

    // Boot screen delay
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        isBooting = false
    }

    // Capture Toast events
    LaunchedEffect(Unit) {
        viewModel.notification.collectLatest { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    if (isBooting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgMidnight),
            contentAlignment = Alignment.Center
        ) {
            // Backdrop logo
            Text(
                "π",
                color = Color.White.copy(alpha = 0.05f),
                fontWeight = FontWeight.Bold,
                fontSize = 400.sp
            )
            
            // Foreground content
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top placeholder to balance the layout
                Spacer(modifier = Modifier.height(40.dp))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "π",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 100.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "one charge is the only thing",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    androidx.compose.material3.CircularProgressIndicator(
                        color = AccentOrange,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "DESIGNED & DEVELOPED BY",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "lakshophd",
                        color = AccentOrange,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    } else {
    androidx.compose.material3.ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentScreen == "HOME",
        drawerContent = {
            androidx.compose.material3.ModalDrawerSheet(
                drawerContainerColor = SurfaceCard,
                drawerContentColor = TextWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        if (isLoggedIn) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "User Profile",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            androidx.compose.material3.HorizontalDivider(color = BorderNavy)
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                IconButton(onClick = {
                                    isLoggedIn = false
                                    scope.launch { drawerState.close() }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Log out", tint = Color.Red)
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                IconButton(onClick = {
                                    showSignInDialog = true
                                    scope.launch { drawerState.close() }
                                }) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = "Sign in", tint = TextWhite)
                                }
                            }
                        }
                        androidx.compose.material3.HorizontalDivider(color = BorderNavy)
                        Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IconButton(onClick = {
                                navigateTo("SETTINGS")
                                scope.launch { drawerState.close() }
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextWhite)
                            }
                        }
                    }
                    
                    // Made by lakshophd inside Drawer Menu
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.HorizontalDivider(color = BorderNavy)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Made by",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            "lakshophd",
                            color = AccentOrange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    ) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!isChartFullscreen) {
                CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "π",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                        Text(
                            "one charge is the only thing",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                },
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (currentScreen == "HOME") {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Pro Feature Menu",
                                    tint = Color.White
                                )
                            }
                        } else if (backStack.size > 1) {
                            IconButton(onClick = { navigateBack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = { navigateTo("HOME") },
                            modifier = Modifier.size(36.dp).testTag("home_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                tint = if (currentScreen == "HOME") AccentOrange else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { navigateTo("EXPLORE") },
                            modifier = Modifier.size(36.dp).testTag("explore_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Explore Markets",
                                tint = if (currentScreen == "EXPLORE") AccentOrange else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BgMidnight
                ),
                actions = {
                    // Search Button
                    IconButton(
                        onClick = { navigateTo("WATCHLIST") },
                        modifier = Modifier.testTag("search_btn")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = TextWhite)
                    }
                }
            )
            }
        },
        bottomBar = {
            if (!isChartFullscreen) {
                NavigationBar(
                containerColor = BgMidnight
            ) {
                NavigationBarItem(
                    selected = currentScreen == "EXPLORE",
                    onClick = { navigateTo("EXPLORE") },
                    icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                    label = { Text("Explore") },
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
                    onClick = { navigateTo("WATCHLIST") },
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
                    onClick = { navigateTo("CHART") },
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
                    onClick = { navigateTo("PAPER_TRADING") },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Max View") },
                    label = { Text("Max View") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentOrange,
                        selectedTextColor = AccentOrange,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = SurfaceCard
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == "AI",
                    onClick = { navigateTo("AI") },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant") },
                    label = { Text("AI") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentOrange,
                        selectedTextColor = AccentOrange,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = SurfaceCard
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == "ANALYTICS",
                    onClick = { navigateTo("ANALYTICS") },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Analytics") },
                    label = { Text("Analytics") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentOrange,
                        selectedTextColor = AccentOrange,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = SurfaceCard
                    )
                )
            }
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
                val activeNewsList = newsPools[currentNewsPoolIndex]

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Recent Market News", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextWhite)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val isProActive = com.example.ui.theme.isProUserEnabled.value
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isProActive) AccentOrange.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isProActive) "✨ ROOM PRO MEMBER" else "STANDARD PLAN",
                                            color = if (isProActive) AccentOrange else Color.Gray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "• Updates: " + (if (isProActive) "30m" else "1h"),
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            
                            // Visual Timer and Fast-Forward helper
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val minutes = secondsRemaining / 60
                                val seconds = secondsRemaining % 60
                                val timeStr = String.format("%02d:%02d", minutes, seconds)
                                
                                Text(
                                    text = "🔄 Refresh in: $timeStr",
                                    color = AccentOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Simulation / Fast Forward Button
                                    Box(
                                        modifier = Modifier
                                            .background(BorderNavy, RoundedCornerShape(4.dp))
                                            .clickable {
                                                if (secondsRemaining > 600) {
                                                    secondsRemaining -= 600
                                                } else {
                                                    secondsRemaining = 0
                                                }
                                            }
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text("⏩ +10m", color = TextWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    // Force Refresh Button
                                    Box(
                                        modifier = Modifier
                                            .background(BorderNavy, RoundedCornerShape(4.dp))
                                            .clickable {
                                                secondsRemaining = 0
                                            }
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text("🔄 Force", color = TextWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(activeNewsList) { news ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderNavy),
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.selectSymbol(news.symbol)
                                navigateTo("CHART")
                            }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(news.source + " • " + news.time, color = TextMuted, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(news.title, color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(AccentOrange.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(news.symbol, color = AccentOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else if (currentScreen == "WATCHLIST") {
                val context = androidx.compose.ui.platform.LocalContext.current
                val sharedPrefs = androidx.compose.runtime.remember { context.getSharedPreferences("market_prefs", android.content.Context.MODE_PRIVATE) }
                var selectedMarket by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(sharedPrefs.getString("default_market", "india") ?: "india") }
                var selectedFilter by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(sharedPrefs.getString("default_filter", "general") ?: "general") }
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
                                navigateTo("CHART")
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

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                            edgePadding = 8.dp,
                            modifier = Modifier.weight(1f)
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
                        IconButton(onClick = {
                            sharedPrefs.edit()
                                .putString("default_market", selectedMarket)
                                .putString("default_filter", selectedFilter)
                                .apply()
                            android.widget.Toast.makeText(context, "Saved market and filter as default", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Star, contentDescription = "Save Default Filter", tint = AccentOrange)
                        }
                    }
                    
                    androidx.compose.material3.ScrollableTabRow(
                        selectedTabIndex = when (selectedFilter) {
                            "general" -> 0
                            "top_gainers" -> 1
                            "top_losers" -> 2
                            "ath" -> 3
                            "atl" -> 4
                            "above_52wk_high" -> 5
                            "below_52wk_low" -> 6
                            else -> 0
                        },
                        containerColor = BgMidnight,
                        contentColor = Color.White,
                        edgePadding = 8.dp,
                        indicator = { },
                        divider = { }
                    ) {
                        val filters = listOf(
                            "general" to "General",
                            "top_gainers" to "Top Gainers",
                            "top_losers" to "Top Losers",
                            "ath" to "All Time High",
                            "atl" to "All Time Low",
                            "above_52wk_high" to "52W High",
                            "below_52wk_low" to "52W Low"
                        )
                        filters.forEach { (filterId, filterName) ->
                            androidx.compose.material3.FilterChip(
                                selected = selectedFilter == filterId,
                                onClick = { selectedFilter = filterId },
                                label = { androidx.compose.material3.Text(filterName) },
                                modifier = Modifier.padding(horizontal = 4.dp),
                                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentOrange.copy(alpha = 0.2f),
                                    selectedLabelColor = AccentOrange,
                                    labelColor = Color.LightGray
                                )
                            )
                        }
                    }

                    com.example.ui.components.TradingViewScreenerWidget(
                        market = selectedMarket,
                        defaultScreen = selectedFilter,
                        onSymbolClick = { sym -> 
                            viewModel.selectSymbol(sym)
                            navigateTo("CHART")
                        },
                        modifier = Modifier.fillMaxSize().weight(1f)
                    )
                }
            } else if (currentScreen == "CHART") {
                val lastCandle = candles.lastOrNull()
                var countdownText by remember(lastCandle, selectedTimeframe) { mutableStateOf("") }
                var showOrderPanel by remember { mutableStateOf(false) }
                var isPositionCardExpanded by remember { mutableStateOf(false) }
                LaunchedEffect(lastCandle, selectedTimeframe) {
                    if (lastCandle != null) {
                        val intervalMs = when (selectedTimeframe) {
                            "5S" -> 5_000L
                            "15S" -> 15_000L
                            "30S" -> 30_000L
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
                    val currentPrice = livePrices[selectedSymbol] ?: watchlist.find { it.item.symbol == selectedSymbol }?.price ?: 100.0
                    
                    if (!isChartFullscreen) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(com.example.ui.theme.BgMidnight)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
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
                                fontSize = 18.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            
                            androidx.compose.material3.IconButton(
                                onClick = {
                                    if (isInWatchlist) {
                                        viewModel.deleteTicker(selectedSymbol)
                                    } else {
                                        viewModel.addSymbolToWatchlist(selectedSymbol)
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = if (isInWatchlist) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = if (isInWatchlist) "Remove from Watchlist" else "Add to Watchlist",
                                    tint = if (isInWatchlist) com.example.ui.theme.BullGreen else com.example.ui.theme.AccentOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            val marketStatus = remember(selectedSymbol) { getMarketStatus(selectedSymbol) }
                            androidx.compose.material3.Card(
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = if (marketStatus.isOpen) com.example.ui.theme.BullGreen.copy(alpha = 0.15f) else com.example.ui.theme.AccentOrange.copy(alpha = 0.15f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (marketStatus.isOpen) com.example.ui.theme.BullGreen.copy(alpha = 0.3f) else com.example.ui.theme.AccentOrange.copy(alpha = 0.3f)),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                androidx.compose.foundation.layout.Row(
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                                ) {
                                    androidx.compose.foundation.layout.Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .background(if (marketStatus.isOpen) com.example.ui.theme.BullGreen else com.example.ui.theme.AccentOrange, androidx.compose.foundation.shape.CircleShape)
                                    )
                                    androidx.compose.material3.Text(
                                        text = if (marketStatus.isOpen) "Live" else "After-Hours",
                                        color = if (marketStatus.isOpen) com.example.ui.theme.BullGreen else com.example.ui.theme.AccentOrange,
                                        fontSize = 8.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        androidx.compose.foundation.layout.Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.End
                        ) {
                            androidx.compose.material3.Text(
                                text = currentPrice.formatPrice(selectedSymbol),
                                color = com.example.ui.theme.AccentOrange,
                                fontSize = 18.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
                    
                    if (position != null) {
                        val pl = (currentPrice - position.position.averageEntryPrice) * position.position.shares
                        val isProfit = pl >= 0
                        
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(com.example.ui.theme.SurfaceDark)
                        ) {
                            // Expandable header row
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isPositionCardExpanded = !isPositionCardExpanded }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                androidx.compose.foundation.layout.Row(
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.Icon(
                                        imageVector = if (isPositionCardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isPositionCardExpanded) "Collapse Position Details" else "Expand Position Details",
                                        tint = com.example.ui.theme.AccentOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    androidx.compose.material3.Text(
                                        text = "Position: ${position.position.shares} shares",
                                        color = com.example.ui.theme.TextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                                androidx.compose.material3.Text(
                                    text = "P&L: ${if (isProfit) "+" else ""}${pl.formatPrice(selectedSymbol)} (${String.format("%.2f", position.unrealizedPLPercent)}%)",
                                    color = if (isProfit) com.example.ui.theme.BullGreen else com.example.ui.theme.BearRed,
                                    fontSize = 13.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }

                            if (isPositionCardExpanded) {
                                androidx.compose.foundation.layout.Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
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
                                                fontSize = 13.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                            androidx.compose.material3.Text(
                                                "Avg Buy: ${position.position.averageEntryPrice.formatPrice(selectedSymbol)}", 
                                                color = com.example.ui.theme.TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                        androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                            androidx.compose.material3.Text(
                                                "Current: ${currentPrice.formatPrice(selectedSymbol)}", 
                                                color = com.example.ui.theme.TextWhite,
                                                fontSize = 13.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                            androidx.compose.material3.Text(
                                                "P&L: ${if (isProfit) "+" else ""}${pl.formatPrice(selectedSymbol)}", 
                                                color = if (isProfit) com.example.ui.theme.BullGreen else com.example.ui.theme.BearRed,
                                                fontSize = 11.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                        }
                                    }
                                    
                                    androidx.compose.material3.HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = Color.DarkGray,
                                        thickness = 0.5.dp
                                    )
                                    
                                    var showSLTPPanel by remember(position.position.stopLoss, position.position.takeProfit) { 
                                        mutableStateOf(position.position.stopLoss == null && position.position.takeProfit == null) 
                                    }
                                    
                                    if (!showSLTPPanel) {
                                        androidx.compose.foundation.layout.Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                            androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
                                                androidx.compose.material3.Text("SL: ${position.position.stopLoss?.let { it.formatPrice(selectedSymbol) } ?: "None"}", color = com.example.ui.theme.TextMuted, fontSize = 12.sp)
                                                androidx.compose.material3.Text("TP: ${position.position.takeProfit?.let { it.formatPrice(selectedSymbol) } ?: "None"}", color = com.example.ui.theme.TextMuted, fontSize = 12.sp)
                                            }
                                            androidx.compose.material3.TextButton(onClick = { showSLTPPanel = true }) {
                                                androidx.compose.material3.Text("Edit", color = com.example.ui.theme.AccentOrange, fontSize = 12.sp)
                                            }
                                        }
                                    } else {
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
                                                    showSLTPPanel = false
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
                            }
                        }
                    }
                    }
                    
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(com.example.ui.theme.SurfaceDark)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        // Engine selection merged here to save vertical space
                        androidx.compose.material3.FilterChip(
                            selected = chartType == "NATIVE",
                            onClick = { chartType = "NATIVE" },
                            label = { androidx.compose.material3.Text("NATIVE PRO", fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = com.example.ui.theme.AccentOrange,
                                selectedLabelColor = Color.Black
                            )
                        )
                        androidx.compose.material3.FilterChip(
                            selected = chartType == "TRADINGVIEW",
                            onClick = { chartType = "TRADINGVIEW" },
                            label = { androidx.compose.material3.Text("TRADINGVIEW WEB", fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = com.example.ui.theme.AccentOrange,
                                selectedLabelColor = Color.Black
                            )
                        )

                        androidx.compose.material3.VerticalDivider(
                            modifier = Modifier.height(20.dp),
                            color = Color.Gray.copy(alpha = 0.5f)
                        )

                        listOf("5S", "15S", "30S", "1M", "5M", "15M", "1H", "4H", "1D", "1W").forEach { tf ->
                            val isTfPro = tf in listOf("5S", "15S", "30S")
                            androidx.compose.material3.FilterChip(
                                selected = selectedTimeframe == tf,
                                onClick = {
                                    if (isTfPro && !com.example.ui.theme.isProUserEnabled.value) {
                                        proDialogFeatureName = when (tf) {
                                            "5S" -> "5 Second Real-time Candles"
                                            "15S" -> "15 Second Real-time Candles"
                                            "30S" -> "30 Second Real-time Candles"
                                            else -> "Second Intervals"
                                        }
                                        showProDialog = true
                                    } else {
                                        viewModel.selectTimeframe(tf)
                                    }
                                },
                                label = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            when (tf) {
                                                "5S" -> "5 Sec"
                                                "15S" -> "15 Sec"
                                                "30S" -> "30 Sec"
                                                "1M" -> "1 Min"
                                                "5M" -> "5 Min"
                                                "15M" -> "15 Min"
                                                "1H" -> "1 Hr"
                                                "4H" -> "4 Hr"
                                                "1D" -> "1 Day"
                                                "1W" -> "1 Wk"
                                                else -> tf
                                            }
                                        )
                                        if (isTfPro && !com.example.ui.theme.isProUserEnabled.value) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Pro Feature",
                                                modifier = Modifier.size(10.dp),
                                                tint = Color.Gray
                                            )
                                        }
                                    }
                                },
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
                        androidx.compose.material3.FilterChip(
                            selected = aiFilterSelected,
                            onClick = { aiFilterSelected = !aiFilterSelected },
                            label = { androidx.compose.material3.Text("AI Filter") },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = com.example.ui.theme.AccentOrange,
                                selectedLabelColor = Color.White
                            )
                        )
                        androidx.compose.material3.IconButton(
                            onClick = { filterSaved = true }
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Save Filter",
                                tint = if (filterSaved) com.example.ui.theme.AccentOrange else Color.White
                            )
                        }
                        androidx.compose.material3.TextButton(
                            onClick = { showPineEditor = true }
                        ) {
                            androidx.compose.material3.Text(
                                "Pine Editor { }",
                                color = com.example.ui.theme.AccentOrange,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                        androidx.compose.material3.TextButton(
                            onClick = { showIndicatorSettingsDialog = true }
                        ) {
                            androidx.compose.material3.Text(
                                "Indicators ⚙️",
                                color = com.example.ui.theme.AccentOrange,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                        androidx.compose.material3.TextButton(
                            onClick = { isChartFullscreen = !isChartFullscreen }
                        ) {
                            androidx.compose.material3.Text(
                                if (isChartFullscreen) "Exit Fullscreen" else "Fullscreen",
                                color = Color.White,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }

                    // Removed Time Interval Slider per user request

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (chartType == "NATIVE") {
                            com.example.ui.components.NativeProChart(
                                symbol = selectedSymbol,
                                timeframe = selectedTimeframe,
                                candles = candles,
                                showSMA = showSMA,
                                showEMA = showEMA,
                                showRSI = showRSI,
                                showUTBot = showUTBot,
                                countdownText = countdownText,
                                averageEntryPrice = position?.position?.averageEntryPrice,
                                smaPeriod = smaPeriod,
                                emaPeriod = emaPeriod,
                                rsiPeriod = rsiPeriod,
                                utBotSensitivity = utBotSensitivity,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            com.example.ui.components.TradingViewWidget(
                                symbol = selectedSymbol,
                                interval = selectedTimeframe,
                                showSMA = showSMA,
                                showEMA = showEMA,
                                showRSI = showRSI,
                                showUTBot = showUTBot,
                                averageEntryPrice = position?.position?.averageEntryPrice,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Float a high-visibility native HUD if user has a position on this symbol
                        position?.let { pos ->
                            androidx.compose.material3.Card(
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = com.example.ui.theme.SurfaceDark.copy(alpha = 0.9f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    com.example.ui.theme.BorderNavy.copy(alpha = 0.8f)
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(androidx.compose.ui.Alignment.TopStart)
                                    .padding(8.dp)
                            ) {
                                androidx.compose.foundation.layout.Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                                ) {
                                    androidx.compose.foundation.layout.Row(
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                                    ) {
                                        androidx.compose.foundation.layout.Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(com.example.ui.theme.BullGreen, androidx.compose.foundation.shape.CircleShape)
                                        )
                                        androidx.compose.material3.Text(
                                            text = "YOUR ACTIVE POSITION",
                                            color = com.example.ui.theme.TextMuted,
                                            fontSize = 9.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                    }
                                    androidx.compose.material3.Text(
                                        text = "Qty: ${String.format("%.4f", pos.position.shares)} units",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                    )
                                    androidx.compose.material3.Text(
                                        text = "Avg Entry Price: ${pos.position.averageEntryPrice.formatPrice(selectedSymbol)}",
                                        color = com.example.ui.theme.AccentOrange,
                                        fontSize = 12.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    
                                    val isProfit = pos.unrealizedPL >= 0
                                    val plColor = if (isProfit) com.example.ui.theme.BullGreen else com.example.ui.theme.BearRed
                                    androidx.compose.material3.Text(
                                        text = "Current P&L: ${if (isProfit) "+" else ""}${pos.unrealizedPL.formatPrice(selectedSymbol)} (${String.format("%.2f", pos.unrealizedPLPercent)}%)",
                                        color = plColor,
                                        fontSize = 11.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    
                                    if (pos.position.stopLoss != null || pos.position.takeProfit != null) {
                                        androidx.compose.foundation.layout.Row(
                                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                                        ) {
                                            pos.position.stopLoss?.let {
                                                androidx.compose.material3.Text(
                                                    "SL: ${it.formatPrice(selectedSymbol)}",
                                                    color = com.example.ui.theme.BearRed,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            pos.position.takeProfit?.let {
                                                androidx.compose.material3.Text(
                                                    "TP: ${it.formatPrice(selectedSymbol)}",
                                                    color = com.example.ui.theme.BullGreen,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (!isChartFullscreen) {
                        if (showOrderPanel) {
                            com.example.ui.components.OrderPlacementSection(
                                selectedSymbol = selectedSymbol,
                                currentPrice = currentPrice,
                                cash = portfolioSummary.cash,
                                onExecuteOrder = { type, shares -> 
                                    viewModel.executePaperTrade(type, shares)
                                    showOrderPanel = false
                                },
                                modifier = Modifier.padding(16.dp).fillMaxWidth()
                            )
                        } else {
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(com.example.ui.theme.SurfaceDark)
                                    .padding(16.dp),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                            ) {
                                androidx.compose.material3.Button(
                                    onClick = { showOrderPanel = true },
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
                                    onClick = { showOrderPanel = true },
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
                    }
                }
            } else if (currentScreen == "PAPER_TRADING") {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                ) {
                    val currentPrice = livePrices[selectedSymbol] ?: watchlist.find { it.item.symbol == selectedSymbol }?.price ?: 100.0
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
                        selectedSymbol = selectedSymbol,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }
            } else if (currentScreen == "EXPLORE") {
                com.example.ui.components.ExploreSection(
                    watchlistItems = watchlist,
                    livePrices = livePrices,
                    selectedSymbol = selectedSymbol,
                    onSymbolSelect = { sym -> viewModel.selectSymbol(sym) },
                    onNavigateToChart = { navigateTo("CHART") },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (currentScreen == "AI") {
                com.example.ui.components.AiAssistantSection(
                    modifier = Modifier.fillMaxSize()
                )
            } else if (currentScreen == "ANALYTICS") {
                com.example.ui.components.AnalyticsSection(
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                com.example.ui.components.SettingsSection(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    }
    
    if (showPineEditor) {
        PineEditorDialog(
            onDismiss = { showPineEditor = false },
            onApply = { /* Apply logic */ showPineEditor = false }
        )
    }

    if (showSignInDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { 
                showSignInDialog = false 
                showEmailForm = false 
            },
            containerColor = SurfaceDark,
            title = {
                Text(if (showEmailForm) "Sign In with Email" else "Sign In", color = TextWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                if (showEmailForm) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        androidx.compose.material3.OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email", color = Color.Gray) },
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentOrange,
                                unfocusedBorderColor = BorderNavy,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        androidx.compose.material3.OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", color = Color.Gray) },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentOrange,
                                unfocusedBorderColor = BorderNavy,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        androidx.compose.material3.Button(
                            onClick = {
                                showEmailForm = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AccentOrange)
                        ) {
                            Text("Sign in with Email", color = Color.White)
                        }
                        androidx.compose.material3.Button(
                            onClick = {
                                handleGoogleSignIn(
                                    context = context,
                                    scope = scope,
                                    onSuccess = {
                                        isLoggedIn = true
                                        showSignInDialog = false
                                    },
                                    onError = { error ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(error)
                                        }
                                        showSignInDialog = false
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("Sign in with Google", color = Color.Black)
                        }
                    }
                }
            },
            confirmButton = {
                if (showEmailForm) {
                    androidx.compose.material3.Button(
                        onClick = {
                            isLoggedIn = true
                            showSignInDialog = false
                            showEmailForm = false
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AccentOrange)
                    ) {
                        Text("Login", color = Color.White)
                    }
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { 
                    if (showEmailForm) {
                        showEmailForm = false
                    } else {
                        showSignInDialog = false 
                    }
                }) {
                    Text(if (showEmailForm) "Back" else "Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showProDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showProDialog = false },
            containerColor = SurfaceDark,
            icon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Premium",
                    tint = AccentOrange,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Unlock Groww Pro",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "The premium feature ($proDialogFeatureName) is part of Groww Pro Gold Tier.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    HorizontalDivider(color = BorderNavy)
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Fast Intervals Enabled", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Study price action on 5s, 15s, and 30s candles.", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Gemini Stock Intelligence", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Get full analysis reports directly from Google AI.", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        com.example.ui.theme.isProUserEnabled.value = true
                        showProDialog = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("Unlock Free Trial Now", color = BgMidnight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showProDialog = false }) {
                    Text("Maybe Later", color = Color.Gray)
                }
            }
        )
    }

    if (showIndicatorSettingsDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showIndicatorSettingsDialog = false },
            containerColor = SurfaceDark,
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Indicators Settings",
                    tint = AccentOrange,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Indicator Settings",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)
                ) {
                    item {
                        Text(
                            text = "Toggle active indicators on the chart and fine-tune their parameters below.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        HorizontalDivider(color = BorderNavy)
                    }

                    // SMA Section
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    androidx.compose.material3.Checkbox(
                                        checked = showSMA,
                                        onCheckedChange = { viewModel.toggleSMA() },
                                        colors = androidx.compose.material3.CheckboxDefaults.colors(
                                            checkedColor = AccentOrange,
                                            uncheckedColor = Color.Gray
                                        )
                                    )
                                    Text("Simple Moving Average (SMA)", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                            if (showSMA) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("SMA Length: $smaPeriod", color = TextWhite, fontSize = 11.sp)
                                }
                                androidx.compose.material3.Slider(
                                    value = smaPeriod.toFloat(),
                                    onValueChange = { viewModel.updateSmaPeriod(it.toInt()) },
                                    valueRange = 5f..100f,
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        thumbColor = AccentOrange,
                                        activeTrackColor = AccentOrange,
                                        inactiveTrackColor = Color.DarkGray
                                    )
                                )
                            }
                        }
                    }

                    // EMA Section
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    androidx.compose.material3.Checkbox(
                                        checked = showEMA,
                                        onCheckedChange = { viewModel.toggleEMA() },
                                        colors = androidx.compose.material3.CheckboxDefaults.colors(
                                            checkedColor = AccentOrange,
                                            uncheckedColor = Color.Gray
                                        )
                                    )
                                    Text("Exponential Moving Average (EMA)", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                            if (showEMA) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("EMA Length: $emaPeriod", color = TextWhite, fontSize = 11.sp)
                                }
                                androidx.compose.material3.Slider(
                                    value = emaPeriod.toFloat(),
                                    onValueChange = { viewModel.updateEmaPeriod(it.toInt()) },
                                    valueRange = 5f..100f,
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        thumbColor = AccentOrange,
                                        activeTrackColor = AccentOrange,
                                        inactiveTrackColor = Color.DarkGray
                                    )
                                )
                            }
                        }
                    }

                    // RSI Section
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    androidx.compose.material3.Checkbox(
                                        checked = showRSI,
                                        onCheckedChange = { viewModel.toggleRSI() },
                                        colors = androidx.compose.material3.CheckboxDefaults.colors(
                                            checkedColor = AccentOrange,
                                            uncheckedColor = Color.Gray
                                        )
                                    )
                                    Text("Relative Strength Index (RSI)", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                            if (showRSI) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("RSI Length: $rsiPeriod", color = TextWhite, fontSize = 11.sp)
                                }
                                androidx.compose.material3.Slider(
                                    value = rsiPeriod.toFloat(),
                                    onValueChange = { viewModel.updateRsiPeriod(it.toInt()) },
                                    valueRange = 2f..30f,
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        thumbColor = AccentOrange,
                                        activeTrackColor = AccentOrange,
                                        inactiveTrackColor = Color.DarkGray
                                    )
                                )
                            }
                        }
                    }

                    // UT Bot Section
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    androidx.compose.material3.Checkbox(
                                        checked = showUTBot,
                                        onCheckedChange = { viewModel.toggleUTBot() },
                                        colors = androidx.compose.material3.CheckboxDefaults.colors(
                                            checkedColor = AccentOrange,
                                            uncheckedColor = Color.Gray
                                        )
                                    )
                                    Text("UT Bot Signal", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                            if (showUTBot) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("UT Bot Sensitivity: ${String.format("%.1f", utBotSensitivity)}", color = TextWhite, fontSize = 11.sp)
                                }
                                androidx.compose.material3.Slider(
                                    value = utBotSensitivity,
                                    onValueChange = { viewModel.updateUtBotSensitivity(it) },
                                    valueRange = 1.0f..10.0f,
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        thumbColor = AccentOrange,
                                        activeTrackColor = AccentOrange,
                                        inactiveTrackColor = Color.DarkGray
                                    )
                                )
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            HorizontalDivider(color = BorderNavy)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Made with ❤️ by lakshophd",
                                color = AccentOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { showIndicatorSettingsDialog = false },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("Apply Settings", color = BgMidnight, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
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

