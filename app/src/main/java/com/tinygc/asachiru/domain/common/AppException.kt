package com.tinygc.asachiru.domain.common

/**
 * アプリケーション固有の例外を表すsealed class
 */
sealed class AppException(override val message: String) : Exception(message) {
    /**
     * ネットワーク関連の例外
     * @param message エラーメッセージ
     */
    data class NetworkException(override val message: String) : AppException(message)

    /**
     * API関連の例外
     * @param statusCode HTTPステータスコード
     * @param message エラーメッセージ
     */
    data class ApiException(val statusCode: Int, override val message: String) : AppException(message)

    /**
     * データ解析関連の例外
     * @param message エラーメッセージ
     */
    data class ParseException(override val message: String) : AppException(message)

    /**
     * 設定関連の例外
     * @param message エラーメッセージ
     */
    data class SettingsException(override val message: String) : AppException(message)
}
