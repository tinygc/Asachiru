package com.tinygc.asachiru.domain.model

/**
 * ニュース取得結果(Presentation層用)
 * @param allArticles RSSから取得した全記事
 * @param newArticles 未読の新しい記事のみ
 */
data class NewsResult(
    val allArticles: List<News>,
    val newArticles: List<News>
)
