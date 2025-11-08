package com.tinygc.asachiru.presentation.setup

/**
 * 設定画面のUI状態
 *
 * StateFlowで管理され、入力値とバリデーション結果を保持します。
 */
data class SetupUiState(
    /** 郵便番号（7桁、ハイフンなし） */
    val postalCode: String = "",

    /** ニュース読み上げ間隔（1～60分） */
    val newsInterval: Int = 30,

    /** 郵便番号のバリデーション結果 */
    val isPostalCodeValid: Boolean = true,

    /** ニュース読み上げ間隔のバリデーション結果 */
    val isNewsIntervalValid: Boolean = true,

    /** 保存処理中フラグ */
    val isSaving: Boolean = false,

    /** 保存時のエラーメッセージ */
    val saveError: String? = null,

    /** 保存完了フラグ */
    val isComplete: Boolean = false
)
