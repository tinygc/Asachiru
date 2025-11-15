package com.tinygc.asachiru.data.datasource.remote

import com.tinygc.asachiru.data.dto.NewsDto
import com.tinygc.asachiru.data.util.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * NHKニュースRSSからデータを取得するデータソース
 */
open class NewsRssDataSource(
    private val httpClient: OkHttpClient
) {
    companion object {
        // NHKニュースRSS
        private const val RSS_URL = "https://www3.nhk.or.jp/rss/news/cat0.xml"
    }

    /**
     * 最新ニュースを取得
     * @return NewsDtoリスト
     * @throws IOException ネットワークエラー
     */
    open
    suspend fun fetchLatestNews(): List<NewsDto> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(RSS_URL)
            .cacheControl(CacheControl.FORCE_NETWORK) // 常に最新のRSSを取得（キャッシュ無視）
            .get()
            .build()

        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("HTTP Error: ${response.code}")
        }

        val inputStream = response.body?.byteStream()
            ?: throw IOException("Empty response")

        RssParser.parse(inputStream)
    }
}
