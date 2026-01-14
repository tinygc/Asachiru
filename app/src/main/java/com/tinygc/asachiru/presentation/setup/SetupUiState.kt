package com.tinygc.asachiru.presentation.setup

import com.tinygc.asachiru.domain.entity.RssFeed

/**
 * 設定画面のUI状態
 *
 * StateFlowで管理され、入力値とバリデーション結果を保持します。
 */
data class SetupUiState(
    /** 郵便番号（7桁、ハイフンなし） */
    val postalCode: String = "",

    /** RSS URL（旧形式、後方互換性のため残す） */
    val rssUrl: String = "",

    /** 選択されたRSSフィードリスト（新形式） */
    val selectedRssFeeds: List<RssFeed> = emptyList(),

    /** カスタムRSSフィードリスト */
    val customRssFeeds: List<RssFeed> = emptyList(),

    /** TTS有効化 */
    val enableTts: Boolean = false,

    /** BGM有効化 */
    val enableBgm: Boolean = true,

    /** RSSプリセット名（旧形式、後方互換性のため残す） */
    val rssPreset: String? = null,

    /** 郵便番号のバリデーション結果 */
    val isPostalCodeValid: Boolean = true,

    /** ニュース読み上げ間隔（1～60分）: 画面からは削除済みだが、Settings互換のため保持 */
    val newsInterval: Int = 5,

    /** ニュース読み上げ間隔のバリデーション結果: UIでは使用しない */
    val isNewsIntervalValid: Boolean = true,

    /** RSS URLのバリデーション結果（旧形式、後方互換性のため残す） */
    val isRssUrlValid: Boolean = false,

    /** RSSフィード選択のバリデーション結果（最低1つ選択必須） */
    val isRssFeedsValid: Boolean = false,

    /** 保存処理中フラグ */
    val isSaving: Boolean = false,

    /** 保存時のエラーメッセージ */
    val saveError: String? = null,

    /** 保存完了フラグ */
    val isComplete: Boolean = false
)
