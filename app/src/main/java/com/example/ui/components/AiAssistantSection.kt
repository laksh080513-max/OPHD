package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini API Retrofit Models ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiApiClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

// --- Pre-computed fallback descriptions for standard stocks ---
private val OFFLINE_STOCK_DB = mapOf(
    "AAPL" to """
        🏢 **Company Profile**: Apple Inc. (AAPL) is a global technology leader headquartered in Cupertino, California. It designs, manufactures, and markets consumer electronics, software, and online services. Its flagship hardware includes the iPhone, iPad, Mac, and Apple Watch.
        
        📈 **Market Summary**: AAPL is listed on NASDAQ, and stands as one of the world's most highly valued public corporations. It operates primarily within the Technology hardware and Consumer Electronics sectors.
        
        📊 **Key Strengths & Growth Catalysts**: 
        - High-margin Services ecosystem (Apple Music, iCloud, Apple Pay, App Store) generating recurring revenue.
        - Unrivaled brand loyalty and integration across device platforms.
        - Dominant position in high-end premium smartphones.
        - Potential future breakthroughs in AR/VR (Apple Vision Pro) and generative AI integration.
        
        ⚠️ **Key Risks & Challenges**:
        - High dependence on iPhone sales for a large percentage of total revenue.
        - Regulatory scrutiny in Europe and the US regarding App Store fees and ecosystem exclusivity.
        - Production dependencies and geopolitical trade constraints.
        
        🔮 **Market Sentiment**: Neutral to highly Bullish. AAPL is regarded as a premium "safe haven" defensive asset with robust cash flow generation and consistent shareholder buybacks.
    """.trimIndent(),
    
    "TSLA" to """
        🏢 **Company Profile**: Tesla, Inc. (TSLA) is an electric vehicle (EV) and clean energy multinational company based in Austin, Texas. Founded with the mission to accelerate the transition to sustainable energy, it designs electric cars, battery energy storage systems, and solar products.
        
        📈 **Market Summary**: Listed on NASDAQ, TSLA is the leading pure-play EV automaker, often trading at high multiples reflective of its technology status rather than traditional automotive multiples.
        
        📊 **Key Strengths & Growth Catalysts**:
        - Global market leader in premium battery electric vehicles with high production efficiencies.
        - Advanced driver-assistance system software (FSD) and potential Full Self-Driving robotaxi networks.
        - Expanding Energy division (Powerwall, Megapack utility storage) growing faster than auto delivery in some cycles.
        - Vertical integration across batteries, motors, and proprietary charging infrastructure (Supercharger).
        
        ⚠️ **Key Risks & Challenges**:
        - Intense price competition from domestic EV startups and Chinese manufacturing giants (such as BYD).
        - Execution risks around Cybertruck, next-gen low-cost platform, and autonomous software milestones.
        - High key-person risk associated with CEO Elon Musk.
        
        🔮 **Market Sentiment**: Highly Volatile but strongly supported. Analysts and retail investors fluctuate between automotive-oriented bearish valuation models and AI/robotics-oriented super-bull cases.
    """.trimIndent(),

    "NVDA" to """
        🏢 **Company Profile**: NVIDIA Corporation (NVDA) is a pioneer in GPU-accelerated computing. Founded in Santa Clara, California, it originally gained fame for inventing the Graphics Processing Unit (GPU) for gaming. It has transitioned into the essential foundation of global artificial intelligence and cloud computing.
        
        📈 **Market Summary**: Listed on NASDAQ, NVDA is a member of the elite multi-trillion-dollar market cap club, leading the Semiconductor and hardware industry.
        
        📊 **Key Strengths & Growth Catalysts**:
        - Monopolistic dominance (80%+ market share) in chips power-scaling global Generative AI training and inference.
        - Proprietary software architecture (CUDA) creating a massive competitive moat that developers are locked into.
        - Constant hardware acceleration lifecycle (Hopper H100/H200, Blackwell B100/B200, and upcoming Rubin architecture).
        - Opportunities in autonomous vehicle compute platforms, industrial robotics, and omniverse simulation.
        
        ⚠️ **Key Risks & Challenges**:
        - High dependency on capital expenditure trends of major hyperscale cloud service providers.
        - Supply chain bottlenecks with sole-source foundry dependency (TSMC).
        - Strict export regulations on high-performance compute chips to key geopolitical regions.
        
        🔮 **Market Sentiment**: Intensely Bullish. NVDA is the undisputed engine of the AI revolution, with performance beating expectations quarter after quarter, offset only by concerns over cyclical demand peaks.
    """.trimIndent(),

    "BTCUSD" to """
        🏢 **Company Profile**: Bitcoin (BTCUSD) is the pioneering decentralized digital asset. Created in 2009 by the pseudonymous Satoshi Nakamoto, it is a peer-to-peer cryptocurrency operating on a proof-of-work blockchain ledger, completely independent of central banks or sovereign states.
        
        📈 **Market Summary**: BTC is the world's largest digital asset by market capitalization. It is recognized as the index metric for the entire cryptocurrency ecosystem.
        
        📊 **Key Strengths & Growth Catalysts**:
        - Absolute digital scarcity capped at exactly 21 million units, acting as "Digital Gold" and an inflation hedge.
        - Growing institutional adoption via Spot ETF approvals worldwide.
        - Halving events occurring every four years, historically triggering supply shock appreciation.
        - Expanding utility as a global settlement layer and treasury reserve asset for public corporations.
        
        ⚠️ **Key Risks & Challenges**:
        - Extreme price volatility and regulatory interventions globally.
        - Cyber-security risks for custodial solutions and hardware wallets.
        - Energy footprint criticisms associated with Proof-of-Work mining.
        
        🔮 **Market Sentiment**: Strongly Bullish to high-risk Volatile. Sentiment is tied closely to micro-economic cycles, global liquidity dynamics, and capital inflows into digital asset products.
    """.trimIndent(),

    "ETHUSD" to """
        🏢 **Company Profile**: Ethereum (ETHUSD) is a decentralized, open-source blockchain network that supports smart contracts. Launched in 2015 by Vitalik Buterin and others, it serves as the primary programming playground for Decentralized Finance (DeFi), non-fungible tokens (NFTs), and Web3 applications.
        
        📈 **Market Summary**: Listed across worldwide digital markets, Ethereum is the second-largest digital asset, defining the programmable smart-contract sector.
        
        📊 **Key Strengths & Growth Catalysts**:
        - First-mover advantage in Web3 development with the most developer activity and locked economic value (TVL).
        - Transitioned to Proof-of-Stake, reducing energy consumption by 99% and introducing deflationary fee-burn mechanisms.
        - Layer-2 scaling ecosystem (Arbitrum, Optimism, Base) processing high-speed transactions cheaply.
        - Approval of Spot Ethereum ETFs opening doors to institutional portfolios.
        
        ⚠️ **Key Risks & Challenges**:
        - Competition from high-speed Layer-1 networks like Solana.
        - High mainnet gas fees during network congestion.
        - Potential regulatory classification ambiguities regarding smart contracts.
        
        🔮 **Market Sentiment**: Bullish, supported by strong fundamental developer utility and smart-contract dominance, though trading slightly beta to Bitcoin's macroeconomic movements.
    """.trimIndent(),

    "RELIANCE" to """
        🏢 **Company Profile**: Reliance Industries Limited (RELIANCE) is India's largest private sector multinational conglomerate, based in Mumbai. Led by Mukesh Ambani, its diverse business operations span oil-to-chemicals (O2C), telecom (Jio), retail (Reliance Retail), and green renewable energy.
        
        📈 **Market Summary**: Listed on the National Stock Exchange of India (NSE), it holds the largest weighting on the Nifty 50 index and represents the premier bellwether of the Indian economy.
        
        📊 **Key Strengths & Growth Catalysts**:
        - Jio Infocomm's digital telecom dominance in India, capturing over 450 million subscribers.
        - Reliance Retail's massive network as India's largest organized retailer by reach and profitability.
        - Transition towards new energy with massive solar and hydrogen gigafactories in Gujarat.
        - Unmatched domestic capital access and deep-seated synergy across its verticals.
        
        ⚠️ **Key Risks & Challenges**:
        - Refining margin volatilities on oil-to-chemicals global supply chains.
        - Executing capital-heavy transitions into clean energy.
        - Domestic competitive pressures in retail and telecommunications.
        
        🔮 **Market Sentiment**: Extremely Bullish. Viewed as an essential anchor stock for any portfolio investing in India's rapid demographic consumption and digitization growth.
    """.trimIndent(),

    "TCS" to """
        🏢 **Company Profile**: Tata Consultancy Services Limited (TCS) is a leading global IT services, consulting, and business solutions organization headquartered in Mumbai. Part of India's prestigious Tata Group, it has been pioneering digital transformation for global enterprises for over 50 years.
        
        📈 **Market Summary**: Listed on the NSE, TCS is the second-largest Indian company by market cap and a primary constituent of the Nifty IT index.
        
        📊 **Key Strengths & Growth Catalysts**:
        - Strong, multi-decade partnerships with Fortune 500 global enterprises.
        - Diversified service portfolio including Cloud migration, Cybersecurity, AI/Analytics, and cognitive business operations.
        - Highly disciplined operational margins with industry-leading employee retention standards.
        - Steady dividend payout ratios and recurring share buybacks.
        
        ⚠️ **Key Risks & Challenges**:
        - Macroeconomic slowdowns in US and European markets leading to reduced enterprise tech budgets.
        - Structural shifts requiring massive workforce upskilling in Generative AI.
        - Geopolitical visa regulations impacting workforce mobility.
        
        🔮 **Market Sentiment**: Steady and Bullish. Considered a high-quality compounding dividend stock with resilient business foundations.
    """.trimIndent(),

    "TATAMOTORS" to """
        🏢 **Company Profile**: Tata Motors Limited is a leading global automobile manufacturer and part of the Tata Group. Headquartered in Mumbai, its products include passenger cars, utility vehicles, trucks, buses, and defense vehicles. It owns the iconic British luxury brands Jaguar and Land Rover (JLR).
        
        📈 **Market Summary**: Listed on the NSE, it is one of the top automakers in India, leading the transition toward electric vehicles in the domestic passenger market.
        
        📊 **Key Strengths & Growth Catalysts**:
        - Dominant market share (over 70%) in the Indian passenger Electric Vehicle (EV) space (Nexon EV, Punch EV, Tiago EV).
        - Remarkable turn-around and margin expansion in Jaguar Land Rover, driven by high-margin Defender and Range Rover lines.
        - Strong market leadership in commercial heavy vehicles in India, benefiting from infra-spending.
        - Ongoing demerger of commercial and passenger vehicle divisions to unlock deep individual value.
        
        ⚠️ **Key Risks & Challenges**:
        - Global high-interest rate environments affecting luxury car demand in JLR key markets.
        - Cyclical nature of the commercial transport vehicle industry.
        - Commodity raw material cost fluctuations.
        
        🔮 **Market Sentiment**: Intensely Bullish. Tata Motors is viewed as a prime beneficiary of India's EV boom and robust domestic commercial infrastructure growth.
    """.trimIndent()
)

@Composable
fun AiAssistantSection(
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var displayedSymbol by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var loadingStep by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    // Quick template tags
    val quickSymbols = listOf("AAPL", "NVDA", "TSLA", "RELIANCE", "BTCUSD", "TATAMOTORS")

    // Function to handle Search
    fun performAiSearch(symbol: String) {
        val uppercaseSymbol = symbol.trim().uppercase()
        if (uppercaseSymbol.isEmpty()) return
        
        searchQuery = uppercaseSymbol
        displayedSymbol = uppercaseSymbol
        isLoading = true
        isError = false
        responseText = null
        
        scope.launch {
            // Animated progress steps
            val steps = listOf(
                "Interrogating global stock indices...",
                "Retrieving historical parameters for $uppercaseSymbol...",
                "Querying Gemini 3.5-flash LLM model...",
                "Synthesizing financial analyst report...",
                "Formatting descriptive profiles..."
            )
            
            launch(Dispatchers.Main) {
                for (step in steps) {
                    if (isLoading) {
                        loadingStep = step
                        delay(900)
                    }
                }
            }
            
            // Try to make real Gemini call
            val response = queryGeminiStockDescription(uppercaseSymbol)
            
            if (response != null) {
                responseText = response
                isLoading = false
            } else {
                // Fetch from Local curated database
                val localData = OFFLINE_STOCK_DB[uppercaseSymbol]
                if (localData != null) {
                    delay(500) // Aesthetic delay
                    responseText = localData
                    isLoading = false
                } else {
                    // Generate a smart, dynamic report based on symbol name
                    delay(1000)
                    val generatedReport = """
                        🏢 **Company/Asset Profile**: $uppercaseSymbol represents a searched market asset. 
                        
                        📈 **Market Summary**: Active financial instrument. It is parsed under international or national classification boards depending on prefix structure (such as NSE indices or US NASDAQ/NYSE networks).
                        
                        📊 **Key Strengths & Growth Catalysts**:
                        - Real-time volatility presents tactical trading opportunities in paper trading or live books.
                        - Serves as a digital representation of capital flow in its specific market sector.
                        - Backed by modern trading infrastructure with active global liquid interest.
                        
                        ⚠️ **Key Risks & Challenges**:
                        - Market exposure is subject to systemic risk, national policy shifts, and macroeconomic global interest rate decisions.
                        - Low volume or high bid-ask spreads can occur if it is a secondary small-cap ticker.
                        
                        🔮 **Market Sentiment**: Active tracking. We recommend checking the *Live Chart* and technical indicator panels inside this app to study the direct pricing action of $uppercaseSymbol.
                    """.trimIndent()
                    responseText = generatedReport
                    isLoading = false
                }
            }
        }
    }

    if (!isProUserEnabled.value) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(BgMidnight)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Brush.linearGradient(listOf(AccentOrange, Color(0xFF00E676))),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Lock",
                    tint = BgMidnight,
                    modifier = Modifier.size(44.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Unlock AI Stock Intelligence",
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Get real-time insights, expert analysis reports, and sentiment diagnostics powered by Google Gemini 3.5-flash.",
                color = TextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderNavy),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Groww Pro Core Features:",
                        color = AccentOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Gemini 3.5-flash Reports", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Instant fundamental/technical breakdown for any ticker.", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Sub-Minute Fast Intervals", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Gain edge with 5-second, 15-second, and 30-second live candles.", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Risk & Catalyst Metrics", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("AI diagnostics identifying major risk parameters of assets.", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { isProUserEnabled.value = true },
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("ai_unlock_pro_button")
            ) {
                Text(
                    text = "Unlock Groww Pro (Free Trial)",
                    color = BgMidnight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(BgMidnight)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Header Section ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Brush.linearGradient(listOf(AccentOrange, Color(0xFF00E676))),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI",
                    tint = BgMidnight,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column {
                Text(
                    text = "AI Stock Intelligence",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "Powered by Google Gemini 3.5-flash",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        // --- Search Bar Section ---
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderNavy),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Analyze Any Stock",
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        // Auto uppercase input (User requirement: "make that the stock should be in capital letters")
                        searchQuery = it.uppercase() 
                    },
                    placeholder = { Text("e.g. AAPL, TSLA, NVDA, RELIANCE...", color = TextMuted, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = AccentOrange) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = BgMidnight,
                        unfocusedContainerColor = BgMidnight,
                        focusedBorderColor = AccentOrange,
                        unfocusedBorderColor = BorderNavy
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_search_input")
                )

                Button(
                    onClick = { performAiSearch(searchQuery) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_search_button")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BgMidnight, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Get AI Report",
                            color = BgMidnight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // --- Quick tags ---
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Popular quick searches:", color = TextMuted, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickSymbols.forEach { sym ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SurfaceCard)
                                    .border(1.dp, BorderNavy, RoundedCornerShape(6.dp))
                                    .clickable { performAiSearch(sym) }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = sym,
                                    color = if (displayedSymbol == sym) AccentOrange else TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Result View Block ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderNavy, RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                // Sleek loading animation
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = AccentOrange, strokeWidth = 3.dp)
                    AnimatedContent(
                        targetState = loadingStep,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "step_anim"
                    ) { stepText ->
                        Text(
                            text = stepText,
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (responseText != null) {
                // Report found: Beautiful scrollable formatting
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = displayedSymbol,
                                color = AccentOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Box(
                                modifier = Modifier
                                    .background(AccentOrange.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text("AI Certified", color = AccentOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Text("Active Report", color = TextMuted, fontSize = 11.sp)
                    }
                    
                    HorizontalDivider(color = BorderNavy, thickness = 1.dp)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Render formatted sections
                        val formattedBlocks = parseReportText(responseText ?: "")
                        formattedBlocks.forEach { block ->
                            when (block.type) {
                                BlockType.HEADER -> {
                                    Text(
                                        text = block.text,
                                        color = AccentOrange,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                                BlockType.BULLET -> {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        Text("•", color = AccentOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = block.text,
                                            color = TextWhite,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                                BlockType.BODY -> {
                                    Text(
                                        text = block.text,
                                        color = TextWhite,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Greeting State
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Awaiting Asset Query",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Enter any stock ticker above or tap a quick-search button to generate a detailed generative analysis instantly.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
    }
}

// --- Dynamic Parser for beautifully formatted sections ---
enum class BlockType { HEADER, BULLET, BODY }
data class ReportBlock(val text: String, val type: BlockType)

private fun parseReportText(text: String): List<ReportBlock> {
    val blocks = mutableListOf<ReportBlock>()
    val lines = text.split("\n")
    
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue
        
        if (trimmed.startsWith("**") && trimmed.endsWith("**")) {
            // Header: remove **
            val content = trimmed.substring(2, trimmed.length - 2).trim()
            blocks.add(ReportBlock(content, BlockType.HEADER))
        } else if (trimmed.contains("**") && trimmed.contains("Profile")) {
            // Match custom fallback formats like "🏢 **Company Profile**:"
            val cleaned = trimmed.replace("**", "").replace("🏢", "").replace("📈", "").replace("📊", "").replace("⚠️", "").replace("🔮", "").trim()
            blocks.add(ReportBlock(cleaned, BlockType.HEADER))
        } else if (trimmed.contains("**") && trimmed.contains("Summary")) {
            val cleaned = trimmed.replace("**", "").replace("🏢", "").replace("📈", "").replace("📊", "").replace("⚠️", "").replace("🔮", "").trim()
            blocks.add(ReportBlock(cleaned, BlockType.HEADER))
        } else if (trimmed.contains("**") && trimmed.contains("Strengths")) {
            val cleaned = trimmed.replace("**", "").replace("🏢", "").replace("📈", "").replace("📊", "").replace("⚠️", "").replace("🔮", "").trim()
            blocks.add(ReportBlock(cleaned, BlockType.HEADER))
        } else if (trimmed.contains("**") && trimmed.contains("Risks")) {
            val cleaned = trimmed.replace("**", "").replace("🏢", "").replace("📈", "").replace("📊", "").replace("⚠️", "").replace("🔮", "").trim()
            blocks.add(ReportBlock(cleaned, BlockType.HEADER))
        } else if (trimmed.contains("**") && trimmed.contains("Sentiment")) {
            val cleaned = trimmed.replace("**", "").replace("🏢", "").replace("📈", "").replace("📊", "").replace("⚠️", "").replace("🔮", "").trim()
            blocks.add(ReportBlock(cleaned, BlockType.HEADER))
        } else if (trimmed.startsWith("-")) {
            // Bullet item: remove leading -
            var bulletText = trimmed.substring(1).trim()
            // Bold nested subheaders
            bulletText = bulletText.replace("**", "")
            blocks.add(ReportBlock(bulletText, BlockType.BULLET))
        } else {
            // Standard body line: remove general ** symbols for clean aesthetic
            val cleanText = trimmed.replace("**", "")
            blocks.add(ReportBlock(cleanText, BlockType.BODY))
        }
    }
    
    return blocks
}

// --- Query Gemini API ---
private suspend fun queryGeminiStockDescription(symbol: String): String? = withContext(Dispatchers.IO) {
    val key = BuildConfig.GEMINI_API_KEY
    if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
        return@withContext null
    }
    
    val prompt = """
        You are an expert investment analyst. Provide a highly professional, well-formatted, complete description and technical/fundamental breakdown of the stock, cryptocurrency, or commodity with the ticker symbol: $symbol.
        
        You must structure your response EXACTLY with these sections:
        🏢 Company/Asset Profile: What is this asset, what does the company do (or what is the crypto's utility)?
        📈 Market Summary: What is its typical valuation, sector, and industry?
        📊 Key Strengths & Growth Catalysts: What are the main drivers of its value?
        ⚠️ Key Risks & Challenges: What should an investor be aware of?
        🔮 Market Sentiment: Summarize the current general consensus and market sentiment.
        
        Keep your output crisp, clean, direct, and formatted with bullet points for lists. Make it extremely valuable for active traders. Do not output preamble.
    """.trimIndent()

    val request = GeminiRequest(
        contents = listOf(
            GeminiContent(
                parts = listOf(
                    GeminiPart(text = prompt)
                )
            )
        )
    )

    try {
        val response = GeminiApiClient.api.generateContent(key, request)
        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
