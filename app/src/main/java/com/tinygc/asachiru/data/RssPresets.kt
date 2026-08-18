package com.tinygc.asachiru.data

/**
 * RSS URLのプリセット定義
 *
 * ここがプリセットURLの唯一の正となる。
 * 配信元のURL変更時はこのマップを更新するだけでよく、
 * 設定に保存済みのフィードは [RssFeedMigrator] が読み込み時に追従させる。
 */
object RssPresets {
    /**
     * プリセット名とURL のマッピング
     */
    val PRESETS = mapOf(
        "NHK" to "https://news.web.nhk/n-data/conf/na/rss/cat0.xml",
        "Yahoo!ニュース" to "https://news.yahoo.co.jp/rss/topics/top-picks.xml",
        "毎日新聞" to "https://mainichi.jp/rss/etc/mainichi-flash.rss",
        "Yahoo!ニュース スポーツ" to "https://news.yahoo.co.jp/rss/topics/sports.xml",
        "音楽ナタリー" to "https://natalie.mu/music/feed/news",
        "ITmedia NEWS" to "https://rss.itmedia.co.jp/rss/2.0/news_bursts.xml",
        "GIGAZINE" to "https://gigazine.net/news/rss_2.0/"
    )

    /**
     * プリセット名のリスト (UIでの選択用)
     */
    val PRESET_NAMES = listOf("選択してください") + PRESETS.keys.toList() + listOf("カスタムURL入力")

    /**
     * カスタムURL入力を示す定数
     */
    const val CUSTOM_URL = "カスタムURL入力"

    /**
     * プリセット名からURLを取得
     */
    fun getUrl(presetName: String): String? {
        return PRESETS[presetName]
    }

    /**
     * 現行のプリセット名かどうか
     */
    fun isPreset(presetName: String): Boolean {
        return PRESETS.containsKey(presetName)
    }
}
