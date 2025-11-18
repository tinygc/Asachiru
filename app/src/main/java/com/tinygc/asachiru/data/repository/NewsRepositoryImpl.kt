package com.tinygc.asachiru.data.repository

import android.util.Log
import com.tinygc.asachiru.data.datasource.remote.NewsRssDataSource
import com.tinygc.asachiru.domain.common.AppException
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.News
import com.tinygc.asachiru.domain.repository.NewsRepository
import com.tinygc.asachiru.domain.repository.SettingsRepository
import java.io.IOException

/**
 * NewsRepositoryの実装
 */
class NewsRepositoryImpl(
    private val newsRssDataSource: NewsRssDataSource,
    private val settingsRepository: SettingsRepository
) : NewsRepository {

    companion object {
        private const val TAG = "NewsRepository"
    }

    override suspend fun getLatestNews(count: Int): Result<List<News>> {
        return try {
            // 設定からRSS URLを取得
            val settings = settingsRepository.getSettings()
            val rssUrl = settings.rssUrl

            // RSS URLが未設定の場合はエラー
            if (rssUrl.isNullOrBlank()) {
                return Result.Error(AppException.ParseException("RSS URLが設定されていません"))
            }

            Log.d(TAG, "getLatestNews: Fetching from RSS URL: $rssUrl")

            // RSS URLを使ってニュースを取得
            val newsDtoList = newsRssDataSource.fetchLatestNews(rssUrl)

            Log.d(TAG, "getLatestNews: Fetched ${newsDtoList.size} news items")

            // DTO → Entity変換 & 件数制限 (negative countは0として扱う)
            val newsList = newsDtoList
                .take(maxOf(0, count))
                .map { dto ->
                    val news = dto.toEntity()
                    Log.d(TAG, "getLatestNews: News[${news.id}]")
                    Log.d(TAG, "  Title: ${news.title}")
                    Log.d(TAG, "  Description length: ${news.description.length} chars")
                    Log.d(TAG, "  Description: ${news.description.take(100)}...") // 最初の100文字だけログ出力
                    news
                }

            Result.Success(newsList)
        } catch (e: IOException) {
            Log.e(TAG, "getLatestNews: Network error", e)
            Result.Error(AppException.NetworkException(e.message ?: "Network error"))
        } catch (e: Exception) {
            Log.e(TAG, "getLatestNews: Parse error", e)
            Result.Error(AppException.ParseException(e.message ?: "Failed to parse RSS"))
        }
    }
}
