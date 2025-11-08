package com.tinygc.asachiru.data.repository

import com.tinygc.asachiru.data.datasource.remote.NewsRssDataSource
import com.tinygc.asachiru.domain.common.AppException
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.News
import com.tinygc.asachiru.domain.repository.NewsRepository
import java.io.IOException

/**
 * NewsRepositoryの実装
 */
class NewsRepositoryImpl(
    private val newsRssDataSource: NewsRssDataSource
) : NewsRepository {

    override suspend fun getLatestNews(count: Int): Result<List<News>> {
        return try {
            val newsDtoList = newsRssDataSource.fetchLatestNews()

            // DTO → Entity変換 & 件数制限
            val newsList = newsDtoList
                .take(count)
                .map { it.toEntity() }

            Result.Success(newsList)
        } catch (e: IOException) {
            Result.Error(AppException.NetworkException(e.message ?: "Network error"))
        } catch (e: Exception) {
            Result.Error(AppException.ParseException(e.message ?: "Failed to parse RSS"))
        }
    }
}
