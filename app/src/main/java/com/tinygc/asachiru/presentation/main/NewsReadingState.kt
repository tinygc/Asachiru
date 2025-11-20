package com.tinygc.asachiru.presentation.main

import com.tinygc.asachiru.domain.model.News

/**
 * ニュース読み上げの状態を表す sealed class
 */
sealed class NewsReadingState {
    /**
     * 待機中（初期状態）
     */
    object Idle : NewsReadingState()

    /**
     * 初回開始待機中（10秒）
     * @param startTimeMs 開始予定時刻（エポックミリ秒）
     */
    data class WaitingForStart(val startTimeMs: Long) : NewsReadingState()

    /**
     * ニュース取得中
     */
    object FetchingNews : NewsReadingState()

    /**
     * 記事読み上げ/表示中
     * @param articleIndex 現在の記事インデックス（0始まり）
     * @param totalArticles 総記事数
     * @param article 現在の記事
     * @param estimatedEndTimeMs 読み上げ終了予定時刻（エポックミリ秒）
     * @param isPaused 詳細表示により一時停止中か
     */
    data class ReadingArticle(
        val articleIndex: Int,
        val totalArticles: Int,
        val article: News,
        val estimatedEndTimeMs: Long,
        val isPaused: Boolean = false
    ) : NewsReadingState()

    /**
     * 記事間待機中（5秒）
     * @param nextArticleIndex 次の記事インデックス
     * @param totalArticles 総記事数
     * @param endTimeMs 待機終了予定時刻（エポックミリ秒）
     * @param isPaused 詳細表示により一時停止中か
     */
    data class ArticleInterval(
        val nextArticleIndex: Int,
        val totalArticles: Int,
        val endTimeMs: Long,
        val isPaused: Boolean = false
    ) : NewsReadingState()

    /**
     * セッション間待機中（設定された間隔：デフォルト30分）
     * @param endTimeMs 待機終了予定時刻（エポックミリ秒）
     */
    data class SessionInterval(val endTimeMs: Long) : NewsReadingState()

    /**
     * 次のイベントまでの残り時間（秒）を計算
     */
    fun getRemainingSeconds(): Long {
        val currentTimeMs = System.currentTimeMillis()
        return when (this) {
            is Idle -> 0L
            is WaitingForStart -> {
                val remaining = (startTimeMs - currentTimeMs) / 1000
                remaining.coerceAtLeast(0L)
            }
            is FetchingNews -> 0L
            is ReadingArticle -> {
                if (isPaused) {
                    // 一時停止中はカウントダウンしない
                    (estimatedEndTimeMs - currentTimeMs) / 1000
                } else {
                    val remaining = (estimatedEndTimeMs - currentTimeMs) / 1000
                    remaining.coerceAtLeast(0L)
                }
            }
            is ArticleInterval -> {
                if (isPaused) {
                    // 一時停止中はカウントダウンしない
                    (endTimeMs - currentTimeMs) / 1000
                } else {
                    val remaining = (endTimeMs - currentTimeMs) / 1000
                    remaining.coerceAtLeast(0L)
                }
            }
            is SessionInterval -> {
                val remaining = (endTimeMs - currentTimeMs) / 1000
                remaining.coerceAtLeast(0L)
            }
        }
    }
}
