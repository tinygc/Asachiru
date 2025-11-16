package com.tinygc.asachiru.domain.usecase.news

import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.News
import com.tinygc.asachiru.domain.repository.NewsRepository

/**
 * 最新ニュースを取得するユースケース
 */
class GetLatestNewsUseCase(
    private val newsRepository: NewsRepository
) {
    /**
     * 最新ニュースを取得
     * @param count 取得件数（デフォルト: 10件）
     * @return ニュースリスト（Result型）
     */
    suspend operator fun invoke(count: Int = 10): Result<List<News>> {
        return try {
            when (val result = newsRepository.getLatestNews(count)) {
                is Result.Success -> Result.Success(result.data.sortedBy { it.publishedAt })
                is Result.Error -> result
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
