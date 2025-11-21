package com.tinygc.asachiru.domain.usecase.news

import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.NewsResult
import com.tinygc.asachiru.domain.repository.NewsRepository
import com.tinygc.asachiru.domain.repository.ReadArticleRepository

/**
 * 最新ニュースを取得するユースケース
 */
class GetLatestNewsUseCase(
    private val newsRepository: NewsRepository,
    private val readArticleRepository: ReadArticleRepository
) {
    /**
     * 最新ニュースを取得
     * @param count 取得件数（デフォルト: 10件）
     * @return ニュース取得結果（全記事と新規記事）
     */
    suspend operator fun invoke(count: Int = 10): Result<NewsResult> {
        return try {
            when (val result = newsRepository.getLatestNews(count)) {
                is Result.Success -> {
                    val allArticles = result.data.sortedBy { it.publishedAt }
                    
                    // 既読記事IDを取得
                    val readIds = readArticleRepository.getReadArticleIds()
                    
                    // 新規記事のみフィルタ
                    val newArticles = allArticles.filter { !readIds.contains(it.id) }
                    
                    // RSSから消えた記事の既読フラグをクリーンアップ
                    val currentArticleIds = allArticles.map { it.id }.toSet()
                    readArticleRepository.cleanupReadArticles(currentArticleIds)
                    
                    Result.Success(NewsResult(allArticles, newArticles))
                }
                is Result.Error -> result
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

