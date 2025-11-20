package com.tinygc.asachiru.presentation.main

import android.util.Log
import com.tinygc.asachiru.domain.model.News
import com.tinygc.asachiru.domain.entity.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ニュース読み上げのState Machine
 */
class NewsReadingStateMachine(
    private val scope: CoroutineScope,
    private val onFetchNews: suspend () -> List<News>,
    private val onReadArticle: suspend (News, Boolean) -> Unit
) {
    private val _state = MutableStateFlow<NewsReadingState>(NewsReadingState.Idle)
    val state: StateFlow<NewsReadingState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var currentArticles: List<News> = emptyList()
    private var pausedAtMs: Long = 0L // 一時停止時の時刻
    private var cachedSettings: Settings? = null // 設定をキャッシュ

    /**
     * イベントをハンドリングして状態を遷移
     */
    fun handleEvent(event: NewsReadingEvent, settings: Settings) {
        cachedSettings = settings // 設定をキャッシュ
        val currentState = _state.value
        Log.d("StateMachine", "Event: $event, CurrentState: $currentState")

        when (event) {
            is NewsReadingEvent.AppStarted -> {
                if (currentState is NewsReadingState.Idle) {
                    transitionToWaitingForStart()
                }
            }

            is NewsReadingEvent.StartTimerExpired -> {
                if (currentState is NewsReadingState.WaitingForStart) {
                    transitionToFetchingNews()
                }
            }

            is NewsReadingEvent.NewsFetched -> {
                if (currentState is NewsReadingState.FetchingNews) {
                    currentArticles = event.articles
                    if (event.articles.isNotEmpty()) {
                        transitionToReadingArticle(0, event.articles, settings)
                    } else {
                        // 記事が0件の場合、セッション待機に移行
                        transitionToSessionInterval(settings)
                    }
                }
            }

            is NewsReadingEvent.ArticleCompleted -> {
                when (currentState) {
                    is NewsReadingState.ReadingArticle -> {
                        val nextIndex = currentState.articleIndex + 1
                        if (nextIndex < currentState.totalArticles) {
                            transitionToArticleInterval(nextIndex, currentState.totalArticles)
                        } else {
                            handleEvent(NewsReadingEvent.AllArticlesCompleted, settings)
                        }
                    }
                    else -> Log.w("StateMachine", "ArticleCompleted in unexpected state: $currentState")
                }
            }

            is NewsReadingEvent.IntervalExpired -> {
                when (currentState) {
                    is NewsReadingState.ArticleInterval -> {
                        transitionToReadingArticle(
                            currentState.nextArticleIndex,
                            currentArticles,
                            settings
                        )
                    }
                    else -> Log.w("StateMachine", "IntervalExpired in unexpected state: $currentState")
                }
            }

            is NewsReadingEvent.AllArticlesCompleted -> {
                transitionToSessionInterval(settings)
            }

            is NewsReadingEvent.SessionIntervalExpired -> {
                if (currentState is NewsReadingState.SessionInterval) {
                    transitionToFetchingNews()
                }
            }

            is NewsReadingEvent.TtsSettingChanged -> {
                when (currentState) {
                    is NewsReadingState.ReadingArticle -> {
                        if (!currentState.isPaused) {
                            if (event.enabled) {
                                // TTS OFF → ON: タイマーをキャンセルして読み上げ開始
                                cancelTimer()
                                _state.value = currentState.copy(estimatedEndTimeMs = 0L)
                                scope.launch {
                                    onReadArticle(currentState.article, true)
                                    cachedSettings?.let { handleEvent(NewsReadingEvent.ArticleCompleted, it) }
                                }
                            } else {
                                // TTS ON → OFF: タイマーを開始
                                cancelTimer()
                                val textLength = currentState.article.title.length + currentState.article.description.length
                                val displayDurationMs = (textLength * 200L) + 1000L
                                val estimatedEndTimeMs = System.currentTimeMillis() + displayDurationMs
                                
                                _state.value = currentState.copy(estimatedEndTimeMs = estimatedEndTimeMs)
                                
                                scope.launch {
                                    onReadArticle(currentState.article, false)
                                }
                                
                                startTimer(displayDurationMs) {
                                    cachedSettings?.let { handleEvent(NewsReadingEvent.ArticleCompleted, it) }
                                }
                            }
                        }
                    }
                    else -> {
                        // 他の状態では設定のみ更新（次回読み上げ時に反映）
                        Log.d("StateMachine", "TTS setting changed to ${event.enabled}, will apply on next article")
                    }
                }
            }

            is NewsReadingEvent.DetailOpened -> {
                when (currentState) {
                    is NewsReadingState.ReadingArticle -> {
                        // 一時停止
                        pausedAtMs = System.currentTimeMillis()
                        cancelTimer()
                        _state.value = currentState.copy(isPaused = true)
                    }
                    is NewsReadingState.ArticleInterval -> {
                        // 一時停止
                        pausedAtMs = System.currentTimeMillis()
                        cancelTimer()
                        _state.value = currentState.copy(isPaused = true)
                    }
                    else -> Log.d("StateMachine", "DetailOpened in state: $currentState (no action)")
                }
            }

            is NewsReadingEvent.DetailClosed -> {
                when (currentState) {
                    is NewsReadingState.ReadingArticle -> {
                        if (currentState.isPaused) {
                            // 一時停止を解除
                            val pausedDurationMs = System.currentTimeMillis() - pausedAtMs
                            val newEstimatedEndTimeMs = currentState.estimatedEndTimeMs + pausedDurationMs
                            _state.value = currentState.copy(
                                isPaused = false,
                                estimatedEndTimeMs = newEstimatedEndTimeMs
                            )
                            // 残り時間でタイマー再開
                            val remainingMs = newEstimatedEndTimeMs - System.currentTimeMillis()
                            startTimer(remainingMs.coerceAtLeast(0)) {
                                handleEvent(NewsReadingEvent.ArticleCompleted, settings)
                            }
                        }
                    }
                    is NewsReadingState.ArticleInterval -> {
                        if (currentState.isPaused) {
                            // 一時停止を解除
                            val pausedDurationMs = System.currentTimeMillis() - pausedAtMs
                            val newEndTimeMs = currentState.endTimeMs + pausedDurationMs
                            _state.value = currentState.copy(
                                isPaused = false,
                                endTimeMs = newEndTimeMs
                            )
                            // 残り時間でタイマー再開
                            val remainingMs = newEndTimeMs - System.currentTimeMillis()
                            startTimer(remainingMs.coerceAtLeast(0)) {
                                handleEvent(NewsReadingEvent.IntervalExpired, settings)
                            }
                        }
                    }
                    else -> Log.d("StateMachine", "DetailClosed in state: $currentState (no action)")
                }
            }

            is NewsReadingEvent.BackgroundTransition -> {
                // バックグラウンド遷移時は状態は保持するが、外部でBGM停止などを行う
                Log.d("StateMachine", "Background transition in state: $currentState")
            }

            is NewsReadingEvent.ForegroundTransition -> {
                // フォアグラウンド復帰時は状態は保持するが、外部でBGM再開などを行う
                Log.d("StateMachine", "Foreground transition in state: $currentState")
            }

            is NewsReadingEvent.NavigateToPrevious -> {
                when (currentState) {
                    is NewsReadingState.ReadingArticle -> {
                        if (currentState.articleIndex > 0) {
                            // 前の記事があれば移動
                            val prevIndex = currentState.articleIndex - 1
                            cancelTimer()
                            transitionToReadingArticle(prevIndex, currentArticles, settings)
                        }
                    }
                    is NewsReadingState.ArticleInterval -> {
                        // インターバル中は現在の記事（次へ進む予定だった記事の1つ前）に戻る
                        val prevIndex = currentState.nextArticleIndex - 1
                        if (prevIndex >= 0) {
                            cancelTimer()
                            transitionToReadingArticle(prevIndex, currentArticles, settings)
                        }
                    }
                    else -> Log.d("StateMachine", "NavigateToPrevious in state: $currentState (no action)")
                }
            }

            is NewsReadingEvent.NavigateToNext -> {
                when (currentState) {
                    is NewsReadingState.ReadingArticle -> {
                        if (currentState.articleIndex < currentState.totalArticles - 1) {
                            // 次の記事があれば移動
                            val nextIndex = currentState.articleIndex + 1
                            cancelTimer()
                            transitionToReadingArticle(nextIndex, currentArticles, settings)
                        }
                    }
                    is NewsReadingState.ArticleInterval -> {
                        // インターバル中は次の記事にすぐ進む
                        if (currentState.nextArticleIndex < currentArticles.size) {
                            cancelTimer()
                            transitionToReadingArticle(currentState.nextArticleIndex, currentArticles, settings)
                        }
                    }
                    else -> Log.d("StateMachine", "NavigateToNext in state: $currentState (no action)")
                }
            }
        }
    }

    /**
     * 初回開始待機状態に遷移（10秒）
     */
    private fun transitionToWaitingForStart() {
        cancelTimer()
        val startTimeMs = System.currentTimeMillis() + 10_000
        _state.value = NewsReadingState.WaitingForStart(startTimeMs)
        startTimer(10_000) {
            cachedSettings?.let { handleEvent(NewsReadingEvent.StartTimerExpired, it) }
        }
    }

    /**
     * ニュース取得状態に遷移
     */
    private fun transitionToFetchingNews() {
        cancelTimer()
        _state.value = NewsReadingState.FetchingNews
        scope.launch {
            try {
                val articles = onFetchNews()
                cachedSettings?.let { handleEvent(NewsReadingEvent.NewsFetched(articles), it) }
            } catch (e: Exception) {
                Log.e("StateMachine", "Failed to fetch news", e)
                // フェッチ失敗時は待機に戻る
                cachedSettings?.let { transitionToSessionInterval(it) }
            }
        }
    }

    /**
     * 記事読み上げ状態に遷移
     */
    private fun transitionToReadingArticle(index: Int, articles: List<News>, settings: Settings) {
        cancelTimer()
        val article = articles[index]

        _state.value = NewsReadingState.ReadingArticle(
            articleIndex = index,
            totalArticles = articles.size,
            article = article,
            estimatedEndTimeMs = 0L, // TTS完了まで時間不明
            isPaused = false
        )

        // 記事を読み上げ（完了後に次へ進む）
        scope.launch {
            if (settings.enableTts) {
                // TTS ON: 読み上げ完了を待ってから次へ
                onReadArticle(article, true)
                handleEvent(NewsReadingEvent.ArticleCompleted, settings)
            } else {
                // TTS OFF: 文字数ベースで表示時間を計算
                val textLength = article.title.length + article.description.length
                val displayDurationMs = (textLength * 200L) + 1000L
                val estimatedEndTimeMs = System.currentTimeMillis() + displayDurationMs
                
                _state.value = NewsReadingState.ReadingArticle(
                    articleIndex = index,
                    totalArticles = articles.size,
                    article = article,
                    estimatedEndTimeMs = estimatedEndTimeMs,
                    isPaused = false
                )
                
                onReadArticle(article, false)
                
                // タイマーで次へ進む
                startTimer(displayDurationMs) {
                    handleEvent(NewsReadingEvent.ArticleCompleted, settings)
                }
            }
        }
    }

    /**
     * 記事間待機状態に遷移（5秒）
     */
    private fun transitionToArticleInterval(nextIndex: Int, totalArticles: Int) {
        cancelTimer()
        val endTimeMs = System.currentTimeMillis() + 5_000
        _state.value = NewsReadingState.ArticleInterval(
            nextArticleIndex = nextIndex,
            totalArticles = totalArticles,
            endTimeMs = endTimeMs,
            isPaused = false
        )
        startTimer(5_000) {
            cachedSettings?.let { handleEvent(NewsReadingEvent.IntervalExpired, it) }
        }
    }

    /**
     * セッション間待機状態に遷移（設定時間）
     */
    private fun transitionToSessionInterval(settings: Settings) {
        cancelTimer()
        val intervalMs = settings.newsIntervalMinutes * 60 * 1000L
        val endTimeMs = System.currentTimeMillis() + intervalMs
        _state.value = NewsReadingState.SessionInterval(endTimeMs)
        startTimer(intervalMs) {
            cachedSettings?.let { handleEvent(NewsReadingEvent.SessionIntervalExpired, it) }
        }
    }

    /**
     * タイマー開始
     */
    private fun startTimer(durationMs: Long, onExpired: () -> Unit) {
        timerJob = scope.launch {
            delay(durationMs)
            onExpired()
        }
    }

    /**
     * タイマーキャンセル
     */
    private fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * クリーンアップ
     */
    fun cleanup() {
        cancelTimer()
        _state.value = NewsReadingState.Idle
    }
}
