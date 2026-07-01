package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class YahooQuoteResponse(
    @Json(name = "quoteResponse") val quoteResponse: YahooQuoteResponseData?
)

@JsonClass(generateAdapter = true)
data class YahooQuoteResponseData(
    @Json(name = "result") val result: List<YahooQuoteResult>?,
    @Json(name = "error") val error: Any?
)

@JsonClass(generateAdapter = true)
data class YahooQuoteResult(
    @Json(name = "symbol") val symbol: String,
    @Json(name = "regularMarketPrice") val regularMarketPrice: Double?
)

interface MarketDataApi {
    @GET("v7/finance/quote")
    suspend fun getYahooQuotes(@Query("symbols") symbols: String): YahooQuoteResponse
}

object MarketDataClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val yahooApi: MarketDataApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://query1.finance.yahoo.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MarketDataApi::class.java)
    }
}
