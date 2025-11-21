package com.tinygc.asachiru.data.repository

import com.tinygc.asachiru.data.datasource.local.SettingsLocalDataSource
import com.tinygc.asachiru.domain.repository.ReadArticleRepository

/**
 * 既読記事リポジトリの実装
 */
class ReadArticleRepositoryImpl(
    private val settingsLocalDataSource: SettingsLocalDataSource
) : ReadArticleRepository {

    override suspend fun getReadArticleIds(): Set<String> {
        return settingsLocalDataSource.getReadArticleIds()
    }

    override suspend fun markArticleAsRead(articleId: String) {
        settingsLocalDataSource.markArticleAsRead(articleId)
    }

    override suspend fun isArticleRead(articleId: String): Boolean {
        return settingsLocalDataSource.isArticleRead(articleId)
    }

    override suspend fun cleanupReadArticles(validArticleIds: Set<String>) {
        settingsLocalDataSource.cleanupReadArticles(validArticleIds)
    }

    override suspend fun clearAll() {
        settingsLocalDataSource.clearReadArticles()
    }
}
