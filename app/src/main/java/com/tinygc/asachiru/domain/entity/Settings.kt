package com.tinygc.asachiru.domain.entity

/**
 * 設定を表すエンティティ
 */
data class Settings(
    val postalCode: String,
    val newsIntervalMinutes: Int,
    val rssUrl: String? = null,              // 旧形式（後方互換性のため残す）
    val enableTts: Boolean = false,
    val enableBgm: Boolean = true,
    val rssPreset: String? = null,           // 旧形式（後方互換性のため残す）
    val rssFeeds: List<RssFeed> = emptyList() // 複数RSS対応（新形式）
) {
    /**
     * 設定が有効かチェック
     */
    fun isValid(): Boolean {
        return isPostalCodeValid() && isNewsIntervalValid() && isRssFeedsValid()
    }

    /**
     * 郵便番号が有効かチェック
     */
    fun isPostalCodeValid(): Boolean {
        return postalCode.length == 7 && postalCode.all { it.isDigit() }
    }

    /**
     * ニュース読み上げ間隔が有効かチェック
     */
    fun isNewsIntervalValid(): Boolean {
        return newsIntervalMinutes in 1..60
    }

    /**
     * RSSフィードが有効かチェック
     * 新形式（rssFeeds）が設定されていればそれを使用
     * 旧形式（rssUrl）のみの場合も有効とする（後方互換性）
     */
    fun isRssFeedsValid(): Boolean {
        return rssFeeds.isNotEmpty() || rssUrl?.isNotBlank() == true
    }

    /**
     * 実際に使用するRSSフィードリストを取得
     * 新形式（rssFeeds）があればそれを返す
     * 旧形式（rssUrl）しかなければ、それをRssFeedに変換して返す
     */
    fun getActiveRssFeeds(): List<RssFeed> {
        return if (rssFeeds.isNotEmpty()) {
            rssFeeds
        } else if (rssUrl != null && rssPreset != null) {
            // 旧形式からの変換
            val feed = if (rssPreset == "カスタムURL入力") {
                RssFeed.fromCustomUrl(rssUrl)
            } else {
                RssFeed.fromPreset(rssPreset, rssUrl)
            }
            listOf(feed)
        } else {
            emptyList()
        }
    }

    companion object {
        /**
         * デフォルト設定
         */
        val DEFAULT = Settings(
            postalCode = "",
            newsIntervalMinutes = 5,
            rssUrl = null,
            enableTts = false,
            enableBgm = true,
            rssPreset = null,
            rssFeeds = emptyList()
        )
    }
}
