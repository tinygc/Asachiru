package com.tinygc.asachiru.domain.entity

/**
 * ニュースを表すエンティティ
 */
data class News(
    val id: String,
    val title: String,
    val description: String,
    val publishedAt: Long // Unixタイムスタンプ（ミリ秒）
) {
    /**
     * 読み上げ用テキストを取得
     */
    fun getSpeechText(): String {
        return "$title。$description"
    }

    companion object {
        /**
         * 空のNews（初期値用）
         */
        val EMPTY = News(
            id = "",
            title = "",
            description = "",
            publishedAt = 0L
        )
    }
}
