package com.tinygc.asachiru.presentation.main

import com.tinygc.asachiru.domain.model.NewsResult

/**
 * ニュース読み上げに関するイベントを表す sealed class
 */
sealed class NewsReadingEvent {
    /**
     * アプリ起動時
     */
    object AppStarted : NewsReadingEvent()

    /**
     * 初回開始タイマーが満了（10秒経過）
     */
    object StartTimerExpired : NewsReadingEvent()

    /**
     * ニュース取得完了
     * @param newsResult 取得した記事(全記事と新規記事)
     */
    data class NewsFetched(val newsResult: NewsResult) : NewsReadingEvent()

    /**
     * 記事の読み上げ/表示完了
     */
    object ArticleCompleted : NewsReadingEvent()

    /**
     * 記事間インターバルが満了（5秒経過）
     */
    object IntervalExpired : NewsReadingEvent()

    /**
     * 全記事の読み上げ/表示完了
     */
    object AllArticlesCompleted : NewsReadingEvent()

    /**
     * セッション間インターバルが満了（設定時間経過）
     */
    object SessionIntervalExpired : NewsReadingEvent()

    /**
     * TTS設定が変更された
     * @param enabled TTS有効か
     */
    data class TtsSettingChanged(val enabled: Boolean) : NewsReadingEvent()

    /**
     * 詳細表示を開いた
     */
    object DetailOpened : NewsReadingEvent()

    /**
     * 詳細表示を閉じた
     */
    object DetailClosed : NewsReadingEvent()

    /**
     * アプリがバックグラウンドに遷移
     */
    object BackgroundTransition : NewsReadingEvent()

    /**
     * アプリがフォアグラウンドに遷移
     */
    object ForegroundTransition : NewsReadingEvent()

    /**
     * 手動で前の記事に移動
     */
    object NavigateToPrevious : NewsReadingEvent()

    /**
     * 手動で次の記事に移動
     */
    object NavigateToNext : NewsReadingEvent()
}
