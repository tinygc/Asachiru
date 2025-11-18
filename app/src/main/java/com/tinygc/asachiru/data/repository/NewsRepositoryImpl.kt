package com.tinygc.asachiru.data.repository

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

    override suspend fun getLatestNews(count: Int): Result<List<News>> {
        return try {
            // 設定からRSS URLを取得
            val settings = settingsRepository.getSettings()
            val rssUrl = settings.rssUrl

            // RSS URLが未設定の場合はエラー
            if (rssUrl.isNullOrBlank()) {
                return Result.Error(AppException.ParseException("RSS URLが設定されていません"))
            }

            // RSS URLを使ってニュースを取得
            val newsDtoList = newsRssDataSource.fetchLatestNews(rssUrl)

            // DTO → Entity変換 & 件数制限 (negative countは0として扱う)
            val newsList = newsDtoList
                .take(maxOf(0, count))
                .map { it.toEntity() }

            Result.Success(newsList)
        } catch (e: IOException) {
            Result.Error(AppException.NetworkException(e.message ?: "Network error"))
        } catch (e: Exception) {
            Result.Error(AppException.ParseException(e.message ?: "Failed to parse RSS"))
        }
    }
}
