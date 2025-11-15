package com.tinygc.asachiru.data.datasource.remote

import com.google.gson.Gson
import com.tinygc.asachiru.data.dto.WeatherApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * 天気APIからデータを取得するデータソース
 */
open class WeatherApiDataSource(
    private val httpClient: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        // 天気予報API（livedoor天気互換）
        private const val BASE_URL = "https://weather.tsukumijima.net/api/forecast"
    }

    /**
     * 天気情報を取得
     * @param areaCode 地域コード
     * @return WeatherApiResponse
     * @throws IOException ネットワークエラー
     */
    open suspend fun fetchWeather(areaCode: String): WeatherApiResponse = withContext(Dispatchers.IO) {
        val url = "$BASE_URL?city=$areaCode"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("HTTP Error: ${response.code}")
        }

        val json = response.body?.string() ?: throw IOException("Empty response")
        if (json.isBlank()) {
            throw IOException("Empty response")
        }
        gson.fromJson(json, WeatherApiResponse::class.java) ?: throw IOException("Empty response")
    }
}
