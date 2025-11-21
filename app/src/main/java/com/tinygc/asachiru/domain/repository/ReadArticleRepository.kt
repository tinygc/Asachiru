package com.tinygc.asachiru.domain.repository

/**
 * 既読記事を管理するリポジトリ
 */
interface ReadArticleRepository {
    /**
     * 既読記事IDを取得
     * @return 既読記事IDのセット
     */
    suspend fun getReadArticleIds(): Set<String>

    /**
     * 記事を既読としてマーク
     * @param articleId 記事ID
     */
    suspend fun markArticleAsRead(articleId: String)

    /**
     * 記事が既読かどうか判定
     * @param articleId 記事ID
     * @return 既読の場合true
     */
    suspend fun isArticleRead(articleId: String): Boolean

    /**
     * 既読記事をクリーンアップ(RSSから消えた記事の既読フラグを削除)
     * @param validArticleIds RSSに存在する記事IDのセット
     */
    suspend fun cleanupReadArticles(validArticleIds: Set<String>)

    /**
     * 全ての既読記事情報をクリア
     */
    suspend fun clearAll()
}
