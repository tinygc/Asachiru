package com.tinygc.asachiru.domain.repository

import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.News

/**
 * ニュース情報を取得するリポジトリのインターフェース
 */
interface NewsRepository {
    /**
     * 最新のニュースを取得
     * @param count 取得件数
     * @return ニュースリスト（Result型）
     */
    suspend fun getLatestNews(count: Int): Result<List<News>>

    /**
     * 指定されたRSS URLからニュースを取得
     * @param rssUrl RSS URL
     * @param sourceName 配信元名（表示用）
     * @return ニュースリスト（Result型）
     */
    suspend fun fetchNewsFromUrl(rssUrl: String, sourceName: String): Result<List<News>>
}
