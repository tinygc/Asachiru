package com.tinygc.asachiru.domain.entity

/**
 * RSSフィードを表すエンティティ
 */
data class RssFeed(
    val id: String,          // 一意なID（プリセット名 or UUID）
    val name: String,        // 表示名（プリセット名 or カスタム名）
    val url: String,         // RSS URL
    val isCustom: Boolean    // カスタムURLかどうか
) {
    companion object {
        /**
         * プリセットからRssFeedを生成
         */
        fun fromPreset(presetName: String, url: String): RssFeed {
            return RssFeed(
                id = presetName,
                name = presetName,
                url = url,
                isCustom = false
            )
        }

        /**
         * カスタムURLからRssFeedを生成
         */
        fun fromCustomUrl(url: String, customName: String? = null): RssFeed {
            return RssFeed(
                id = java.util.UUID.randomUUID().toString(),
                name = customName ?: url,
                url = url,
                isCustom = true
            )
        }
    }
}
