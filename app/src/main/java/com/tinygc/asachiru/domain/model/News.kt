package com.tinygc.asachiru.domain.model

/**
 * ニュース記事を表すドメインモデル（State Machine用）
 */
data class News(
    val id: String,
    val title: String,
    val description: String,
    val link: String,
    val imageUrl: String?
)
