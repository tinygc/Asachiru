package com.tinygc.asachiru.presentation.setup

/**
 * 設定画面のUI状態
 *
 * StateFlowで管理され、入力値とバリデーション結果を保持します。
 */
data class SetupUiState(
    /** 郵便番号（7桁、ハイフンなし） */
    val postalCode: String = "",


    /** RSS URL */
    val rssUrl: String = "",

    /** TTS有効化 */
    val enableTts: Boolean = false,

    /** BGM有効化 */
    val enableBgm: Boolean = true,

    /** RSSプリセット名 */
    val rssPreset: String? = null,

    /** 郵便番号のバリデーション結果 */
    val isPostalCodeValid: Boolean = true,

    /** ニュース読み上げ間隔（1～60分）: 画面からは削除済みだが、Settings互換のため保持 */
    val newsInterval: Int = 5,

    /** ニュース読み上げ間隔のバリデーション結果: UIでは使用しない */
    val isNewsIntervalValid: Boolean = true,

    /** RSS URLのバリデーション結果 */
    val isRssUrlValid: Boolean = false,

    /** 保存処理中フラグ */
    val isSaving: Boolean = false,

    /** 保存時のエラーメッセージ */
    val saveError: String? = null,

    /** 保存完了フラグ */
    val isComplete: Boolean = false
)
