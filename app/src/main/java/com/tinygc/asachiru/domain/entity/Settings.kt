package com.tinygc.asachiru.domain.entity

/**
 * 設定を表すエンティティ
 */
data class Settings(
    val postalCode: String,
    val newsIntervalMinutes: Int
) {
    /**
     * 設定が有効かチェック
     */
    fun isValid(): Boolean {
        return isPostalCodeValid() && isNewsIntervalValid()
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

    companion object {
        /**
         * デフォルト設定
         */
        val DEFAULT = Settings(
            postalCode = "",
            newsIntervalMinutes = 30
        )
    }
}
