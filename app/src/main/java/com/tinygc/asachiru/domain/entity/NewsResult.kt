package com.tinygc.asachiru.domain.entity

/**
 * ニュース取得結果
 * @param allArticles RSSから取得した全記事
 * @param newArticles 未読の新しい記事のみ
 */
data class NewsResult(
    val allArticles: List<News>,
    val newArticles: List<News>
)
