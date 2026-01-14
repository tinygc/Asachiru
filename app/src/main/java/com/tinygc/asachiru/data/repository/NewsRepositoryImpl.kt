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
            // 設定からRSSフィードリストを取得
            val settings = settingsRepository.getSettings()
            val rssFeeds = settings.getActiveRssFeeds()

            // RSSフィードが未設定の場合はエラー
            if (rssFeeds.isEmpty()) {
                return Result.Error(AppException.ParseException("RSSフィードが設定されていません"))
            }

            Log.d(TAG, "getLatestNews: Fetching from ${rssFeeds.size} RSS feeds")

            // 複数のRSSフィードから並列取得
            val allNews = mutableListOf<News>()
            rssFeeds.forEach { feed ->
                try {
                    val result = fetchNewsFromUrl(feed.url, feed.name)
                    if (result is Result.Success) {
                        allNews.addAll(result.data)
                    } else {
                        Log.w(TAG, "getLatestNews: Failed to fetch from ${feed.name}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "getLatestNews: Error fetching from ${feed.name}", e)
                }
            }

            // 公開日時でソート（新しい順）
            val sortedNews = allNews.sortedByDescending { it.publishedAt }

            // 重複削除（タイトルとIDで判定）
            val uniqueNews = sortedNews.distinctBy { "${it.id}_${it.title}" }

            // 件数制限
            val limitedNews = uniqueNews.take(maxOf(0, count))

            Log.d(TAG, "getLatestNews: Total ${limitedNews.size} news items after merge")

            Result.Success(limitedNews)
        } catch (e: IOException) {
            Log.e(TAG, "getLatestNews: Network error", e)
            Result.Error(AppException.NetworkException(e.message ?: "Network error"))
        } catch (e: Exception) {
            Log.e(TAG, "getLatestNews: Parse error", e)
            Result.Error(AppException.ParseException(e.message ?: "Failed to parse RSS"))
        }
    }

    override suspend fun fetchNewsFromUrl(rssUrl: String, sourceName: String): Result<List<News>> {
        return try {
            Log.d(TAG, "fetchNewsFromUrl: Fetching from $sourceName ($rssUrl)")

            // RSS URLを使ってニュースを取得
            val newsDtoList = newsRssDataSource.fetchLatestNews(rssUrl)

            Log.d(TAG, "fetchNewsFromUrl: Fetched ${newsDtoList.size} news items from $sourceName")

            // DTO → Entity変換（配信元名を設定）
            val newsList = newsDtoList.map { dto ->
                val news = dto.copy(sourceName = sourceName).toEntity()
                Log.d(TAG, "fetchNewsFromUrl: News[${news.id}] from ${news.sourceName}")
                Log.d(TAG, "  Title: ${news.title}")
                news
            }

            Result.Success(newsList)
        } catch (e: IOException) {
            Log.e(TAG, "fetchNewsFromUrl: Network error from $sourceName", e)
            Result.Error(AppException.NetworkException(e.message ?: "Network error"))
        } catch (e: Exception) {
            Log.e(TAG, "fetchNewsFromUrl: Parse error from $sourceName", e)
            Result.Error(AppException.ParseException(e.message ?: "Failed to parse RSS"))
        }
    }
}
