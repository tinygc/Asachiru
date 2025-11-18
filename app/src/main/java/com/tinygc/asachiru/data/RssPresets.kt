package com.tinygc.asachiru.data

/**
 * RSS URLのプリセット定義
 */
object RssPresets {
    /**
     * プリセット名とURL のマッピング
     */
    val PRESETS = mapOf(
        "NHK" to "https://www.nhk.or.jp/rss/news/cat0.xml",
        "Yahoo!ニュース" to "https://news.yahoo.co.jp/rss/topics/top-picks.xml",
        "毎日新聞" to "https://mainichi.jp/rss/etc/mainichi-flash.rss"
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
}
