package com.tinygc.asachiru.presentation.main

import android.util.Log
import com.tinygc.asachiru.domain.model.News
import com.tinygc.asachiru.domain.model.NewsResult
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
    private val onFetchNews: suspend () -> NewsResult,
    private val onReadArticle: suspend (News, Boolean) -> Unit,
    private val onMarkAsRead: suspend (String) -> Unit,
    private val onResetAllReadStatus: suspend () -> Unit = {} // 既読ステータスをリセット
) {
    private val _state = MutableStateFlow<NewsReadingState>(NewsReadingState.Idle)
    val state: StateFlow<NewsReadingState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var allArticles: List<News> = emptyList() // RSSから取得した全記事
    private var newArticles: List<News> = emptyList() // 未読の新しい記事
    private var pausedAtMs: Long = 0L // 一時停止時の時刻
    private var cachedSettings: Settings? = null // 設定をキャッシュ
    private var noNewArticlesSinceMs: Long = 0L // 新着なし状態が始まった時刻（1時間トラッキング用）

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
                    // 全記事と新規記事を保存
                    allArticles = event.newsResult.allArticles
                    newArticles = event.newsResult.newArticles
                    
                    if (newArticles.isNotEmpty()) {
                        // 新しい記事がある場合、最初の新規記事から読み上げ開始
                        // newArticles[0]のallArticles内でのインデックスを取得
                        val firstNewArticleId = newArticles[0].id
                        val startIndex = allArticles.indexOfFirst { it.id == firstNewArticleId }
                        if (startIndex >= 0) {
                            transitionToReadingArticle(startIndex, allArticles, settings)
                            Log.d("StateMachine", "New articles found: ${newArticles.size} / total: ${allArticles.size}, starting at index $startIndex")
                        } else {
                            Log.e("StateMachine", "First new article not found in allArticles!")
                        }
                    } else {
                        // 新しい記事がない場合、5分待機してから再フェッチ
                        Log.d("StateMachine", "No new articles, waiting 5 minutes before retry")
                        transitionToNoNewArticlesWait(settings)
                    }
                }
            }

            is NewsReadingEvent.ArticleCompleted -> {
                when (currentState) {
                    is NewsReadingState.ReadingArticle -> {
                        // ポップアップ表示中であれば遷移保留しフラグのみ立てる
                        if (currentState.isPaused) {
                            _state.value = currentState.copy(hasCompletedReading = true)
                            Log.d("StateMachine", "ArticleCompleted deferred (detail open) index=${currentState.articleIndex}")
                            return
                        }
                        
                        // 次に読むべき記事を探す(newArticlesの範囲内で)
                        val currentArticleId = currentState.article.id
                        val currentIndexInNew = newArticles.indexOfFirst { it.id == currentArticleId }
                        
                        if (currentIndexInNew >= 0 && currentIndexInNew + 1 < newArticles.size) {
                            // 次のnew記事がある
                            val nextNewArticleId = newArticles[currentIndexInNew + 1].id
                            val nextIndexInAll = allArticles.indexOfFirst { it.id == nextNewArticleId }
                            
                            if (nextIndexInAll >= 0) {
                                // TTS OFFの場合はインターバルなしで即次の記事へ
                                if (!settings.enableTts) {
                                    transitionToReadingArticle(nextIndexInAll, allArticles, settings)
                                } else {
                                    transitionToArticleInterval(nextIndexInAll, allArticles.size)
                                }
                            } else {
                                Log.e("StateMachine", "Next new article not found in allArticles!")
                                handleEvent(NewsReadingEvent.AllArticlesCompleted, settings)
                            }
                        } else {
                            // 全てのnew記事を読み終わった
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
                            allArticles,
                            settings
                        )
                    }
                    else -> Log.w("StateMachine", "IntervalExpired in unexpected state: $currentState")
                }
            }

            is NewsReadingEvent.AllArticlesCompleted -> {
                // 全記事読了後、allArticlesの全記事を既読としてマーク
                scope.launch {
                    allArticles.forEach { article ->
                        onMarkAsRead(article.id)
                    }
                    Log.d("StateMachine", "Marked all ${allArticles.size} articles as read before showing ad")
                }
                // newArticlesをクリアして次のフェッチで新規記事のみを対象にする
                newArticles = emptyList()
                transitionToSessionInterval(settings)
            }

            is NewsReadingEvent.SessionIntervalExpired -> {
                if (currentState is NewsReadingState.SessionInterval) {
                    // 広告終了後は常に新規ニュースをフェッチ
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
                                // TTS ON → OFF: タイマーを開始（タイトル文字数×7倍で計算）
                                cancelTimer()
                                val textLength = currentState.article.title.length * 7
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
                            _state.value = currentState.copy(isPaused = false)
                            
                            // TTS OFF時のみタイマー再開（TTS ON時はonReadArticleが継続中）
                            if (currentState.estimatedEndTimeMs > 0L) {
                                val pausedDurationMs = System.currentTimeMillis() - pausedAtMs
                                val newEstimatedEndTimeMs = currentState.estimatedEndTimeMs + pausedDurationMs
                                _state.value = currentState.copy(
                                    isPaused = false,
                                    estimatedEndTimeMs = newEstimatedEndTimeMs
                                )
                                // 残り時間でタイマー再開
                                val remainingMs = newEstimatedEndTimeMs - System.currentTimeMillis()
                                startTimer(remainingMs.coerceAtLeast(0)) {
                                    cachedSettings?.let { handleEvent(NewsReadingEvent.ArticleCompleted, it) }
                                }
                            }
                            // 読了済みだった場合は次のステップへ遷移再開
                            val updated = _state.value as NewsReadingState.ReadingArticle
                            if (updated.hasCompletedReading) {
                                val settingsLocal = cachedSettings ?: settings
                                val nextIndex = updated.articleIndex + 1
                                if (nextIndex < updated.totalArticles) {
                                    if (!settingsLocal.enableTts) {
                                        transitionToReadingArticle(nextIndex, allArticles, settingsLocal)
                                    } else {
                                        transitionToArticleInterval(nextIndex, updated.totalArticles)
                                    }
                                } else {
                                    handleEvent(NewsReadingEvent.AllArticlesCompleted, settingsLocal)
                                }
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
                // バックグラウンド遷移時はタイマーをキャンセルして一時停止
                Log.d("StateMachine", "Background transition in state: $currentState")
                when (currentState) {
                    is NewsReadingState.ReadingArticle -> {
                        if (!currentState.isPaused) {
                            pausedAtMs = System.currentTimeMillis()
                            cancelTimer()
                            _state.value = currentState.copy(isPaused = true)
                        }
                    }
                    is NewsReadingState.ArticleInterval -> {
                        if (!currentState.isPaused) {
                            pausedAtMs = System.currentTimeMillis()
                            cancelTimer()
                            _state.value = currentState.copy(isPaused = true)
                        }
                    }
                    is NewsReadingState.WaitingForStart -> {
                        // 開始待機中もタイマーをキャンセル
                        cancelTimer()
                    }
                    else -> { /* 他の状態では何もしない */ }
                }
            }

            is NewsReadingEvent.ForegroundTransition -> {
                // フォアグラウンド復帰時はタイマーを再開
                Log.d("StateMachine", "Foreground transition in state: $currentState")
                when (currentState) {
                    is NewsReadingState.ReadingArticle -> {
                        if (currentState.isPaused) {
                            val pausedDurationMs = System.currentTimeMillis() - pausedAtMs
                            val newEndTimeMs = currentState.estimatedEndTimeMs + pausedDurationMs
                            _state.value = currentState.copy(
                                isPaused = false,
                                estimatedEndTimeMs = newEndTimeMs
                            )
                            // TTS OFFの場合のみタイマー再開（TTS ONの場合は読み上げ完了で遷移）
                            if (!settings.enableTts) {
                                val remainingMs = newEndTimeMs - System.currentTimeMillis()
                                if (remainingMs > 0) {
                                    startTimer(remainingMs) {
                                        handleEvent(NewsReadingEvent.ArticleCompleted, settings)
                                    }
                                }
                            }
                        }
                    }
                    is NewsReadingState.ArticleInterval -> {
                        if (currentState.isPaused) {
                            val pausedDurationMs = System.currentTimeMillis() - pausedAtMs
                            val newEndTimeMs = currentState.endTimeMs + pausedDurationMs
                            _state.value = currentState.copy(
                                isPaused = false,
                                endTimeMs = newEndTimeMs
                            )
                            val remainingMs = newEndTimeMs - System.currentTimeMillis()
                            startTimer(remainingMs.coerceAtLeast(0)) {
                                handleEvent(NewsReadingEvent.IntervalExpired, settings)
                            }
                        }
                    }
                    is NewsReadingState.WaitingForStart -> {
                        // 開始タイマーを再開
                        val remainingMs = currentState.startTimeMs - System.currentTimeMillis()
                        if (remainingMs > 0) {
                            startTimer(remainingMs) {
                                handleEvent(NewsReadingEvent.StartTimerExpired, settings)
                            }
                        } else {
                            // すでに開始時刻を過ぎていたら即座に開始
                            handleEvent(NewsReadingEvent.StartTimerExpired, settings)
                        }
                    }
                    else -> { /* 他の状態では何もしない */ }
                }
            }

            is NewsReadingEvent.NavigateToPrevious -> {
                Log.d("StateMachine", "NavigateToPrevious: current state = $currentState")
                when (currentState) {
                    is NewsReadingState.ReadingArticle -> {
                        if (currentState.articleIndex > 0) {
                            // 前の記事があれば移動
                            val prevIndex = currentState.articleIndex - 1
                            cancelTimer()
                            transitionToReadingArticle(prevIndex, allArticles, settings)
                        } else {
                            // 最初の記事で上キーを押した場合は何もしない（読み上げ継続）
                            Log.d("StateMachine", "NavigateToPrevious: Already at first article, ignoring")
                        }
                    }
                    is NewsReadingState.ArticleInterval -> {
                        // インターバル中は現在の記事(次へ進む予定だった記事の1つ前)に戻る
                        val prevIndex = currentState.nextArticleIndex - 1
                        if (prevIndex >= 0) {
                            cancelTimer()
                            transitionToReadingArticle(prevIndex, allArticles, settings)
                        }
                    }
                    is NewsReadingState.SessionInterval -> {
                        // SessionInterval(広告非表示=新記事なし待機)から最後の記事に戻る
                        if (!currentState.showAd && allArticles.isNotEmpty()) {
                            Log.d("StateMachine", "NavigateToPrevious from SessionInterval(no ad): Going to last article (${allArticles.size - 1})")
                            cancelTimer()
                            transitionToReadingArticle(allArticles.size - 1, allArticles, settings)
                        } else {
                            Log.d("StateMachine", "NavigateToPrevious from SessionInterval: showAd=${currentState.showAd}, ignoring")
                        }
                    }
                    is NewsReadingState.FetchingNews -> {
                        // ニュース取得中(広告タイマー切れ後)は最後の記事に戻る
                        if (allArticles.isNotEmpty()) {
                            Log.d("StateMachine", "NavigateToPrevious from FetchingNews: Going to last article (${allArticles.size - 1})")
                            cancelTimer()
                            transitionToReadingArticle(allArticles.size - 1, allArticles, settings)
                        } else {
                            Log.w("StateMachine", "NavigateToPrevious from FetchingNews: allArticles is empty!")
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
                            transitionToReadingArticle(nextIndex, allArticles, settings)
                        } else {
                            // 最後の記事で下キーを押した場合は何もしない（読み上げ継続）
                            Log.d("StateMachine", "NavigateToNext: Already at last article, ignoring")
                        }
                    }
                    is NewsReadingState.ArticleInterval -> {
                        // インターバル中は次の記事にすぐ進む
                        if (currentState.nextArticleIndex < allArticles.size) {
                            cancelTimer()
                            transitionToReadingArticle(currentState.nextArticleIndex, allArticles, settings)
                        }
                    }
                    is NewsReadingState.SessionInterval -> {
                        // SessionInterval(広告非表示=新記事なし待機)から最初の新規記事に戻る
                        if (!currentState.showAd && newArticles.isNotEmpty()) {
                            val firstNewArticleId = newArticles[0].id
                            val startIndex = allArticles.indexOfFirst { it.id == firstNewArticleId }
                            if (startIndex >= 0) {
                                Log.d("StateMachine", "NavigateToNext from SessionInterval(no ad): Going to first new article ($startIndex)")
                                cancelTimer()
                                transitionToReadingArticle(startIndex, allArticles, settings)
                            } else {
                                Log.w("StateMachine", "NavigateToNext from SessionInterval: first new article not found")
                            }
                        } else {
                            Log.d("StateMachine", "NavigateToNext from SessionInterval: showAd=${currentState.showAd}, ignoring")
                        }
                    }
                    else -> Log.d("StateMachine", "NavigateToNext in state: $currentState (no action)")
                }
            }
        }
    }

    /**
     * 初回開始待機状態に遷移（3秒）
     */
    private fun transitionToWaitingForStart() {
        cancelTimer()
        val startTimeMs = System.currentTimeMillis() + 3_000
        _state.value = NewsReadingState.WaitingForStart(startTimeMs)
        startTimer(3_000) {
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
                val newsResult = onFetchNews()
                cachedSettings?.let { handleEvent(NewsReadingEvent.NewsFetched(newsResult), it) }
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
    /**
     * 記事読み上げ/表示状態に遷移
     * @param index allArticlesの中のインデックス
     * @param allArticles RSSから取得した全記事(ナビゲーション範囲)
     * @param settings 設定
     */
    private fun transitionToReadingArticle(index: Int, allArticles: List<News>, settings: Settings) {
        cancelTimer()
        val article = allArticles[index]

        _state.value = NewsReadingState.ReadingArticle(
            articleIndex = index,
            totalArticles = allArticles.size,
            article = article,
            estimatedEndTimeMs = 0L, // TTS完了まで時間不明
            isPaused = false,
            hasCompletedReading = false
        )

        // 記事を読み上げ（完了後に次へ進む）
        scope.launch {
            if (settings.enableTts) {
                // TTS ON: 読み上げ完了を待ってから次へ
                onReadArticle(article, true)
                handleEvent(NewsReadingEvent.ArticleCompleted, settings)
            } else {
                // TTS OFF: タイトルの文字数×7倍で表示時間を計算（詳細は含まない）
                val textLength = article.title.length * 7
                val displayDurationMs = (textLength * 200L) + 1000L
                val estimatedEndTimeMs = System.currentTimeMillis() + displayDurationMs
                
                _state.value = NewsReadingState.ReadingArticle(
                    articleIndex = index,
                    totalArticles = allArticles.size,
                    article = article,
                    estimatedEndTimeMs = estimatedEndTimeMs,
                    isPaused = false,
                    hasCompletedReading = false
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
     * セッション間待機状態に遷移（広告10秒 → 5分待機）
     * 新着ありで記事を読んだ後に呼ばれるので、noNewArticlesSinceMsをリセット
     */
    private fun transitionToSessionInterval(@Suppress("UNUSED_PARAMETER") settings: Settings) {
        cancelTimer()
        // 新着があったのでトラッキングをリセット
        noNewArticlesSinceMs = 0L
        
        val adDurationMs = 10_000L // 広告表示時間: 10秒
        val waitDurationMs = 5 * 60 * 1000L // 5分待機
        val totalDurationMs = adDurationMs + waitDurationMs
        val now = System.currentTimeMillis()
        val endTimeMs = now + totalDurationMs
        val adEndTimeMs = now + adDurationMs
        
        _state.value = NewsReadingState.SessionInterval(
            endTimeMs = endTimeMs,
            showAd = true, // 最初は広告表示
            adEndTimeMs = adEndTimeMs,
            noNewArticlesSinceMs = 0L // 新着があったのでリセット
        )
        Log.d("StateMachine", "SessionInterval: ad for ${adDurationMs}ms, then wait ${waitDurationMs}ms")
        
        // 10秒後に広告終了 → 待機状態に切り替え
        startTimer(adDurationMs) {
            Log.d("StateMachine", "SessionInterval: ad ended, now waiting")
            val currentState = _state.value
            if (currentState is NewsReadingState.SessionInterval && currentState.showAd) {
                _state.value = currentState.copy(showAd = false)
                // 残り5分待機
                startTimer(waitDurationMs) {
                    Log.d("StateMachine", "SessionInterval: wait expired, triggering SessionIntervalExpired")
                    cachedSettings?.let { handleEvent(NewsReadingEvent.SessionIntervalExpired, it) }
                }
            }
        }
    }

    /**
     * 新しい記事がない場合の待機状態（5分）
     * 1時間新着なしが続いたら全記事を既読解除して再表示
     */
    private fun transitionToNoNewArticlesWait(@Suppress("UNUSED_PARAMETER") settings: Settings) {
        cancelTimer()
        val now = System.currentTimeMillis()
        val oneHourMs = 60 * 60 * 1000L
        
        // 初回なら開始時刻を記録
        if (noNewArticlesSinceMs == 0L) {
            noNewArticlesSinceMs = now
            Log.d("StateMachine", "NoNewArticlesWait: started tracking no-new-articles time")
        }
        
        // 1時間経過チェック
        val elapsedMs = now - noNewArticlesSinceMs
        if (elapsedMs >= oneHourMs) {
            Log.d("StateMachine", "NoNewArticlesWait: 1 hour passed, resetting read status and showing all articles")
            // 1時間経過！既読リセットして全記事表示
            noNewArticlesSinceMs = 0L
            scope.launch {
                onResetAllReadStatus()
                // リセット後にフェッチすると全記事が新規扱いで返ってくる
                cachedSettings?.let { handleEvent(NewsReadingEvent.SessionIntervalExpired, it) }
            }
            return
        }
        
        val waitDurationMs = 5 * 60 * 1000L // 5分
        val endTimeMs = now + waitDurationMs
        _state.value = NewsReadingState.SessionInterval(
            endTimeMs = endTimeMs,
            showAd = false, // 広告は表示しない
            adEndTimeMs = 0L,
            noNewArticlesSinceMs = noNewArticlesSinceMs
        )
        val remainingMinutes = (oneHourMs - elapsedMs) / 60_000L
        Log.d("StateMachine", "NoNewArticlesWait: waiting 5min, ${remainingMinutes}min until full reset")
        // 5分後に再フェッチ
        startTimer(waitDurationMs) {
            Log.d("StateMachine", "NoNewArticlesWait: timeout, triggering SessionIntervalExpired event")
            cachedSettings?.let { handleEvent(NewsReadingEvent.SessionIntervalExpired, it) }
        }
    }

    /**
     * タイマー開始
     */
    private fun startTimer(durationMs: Long, onExpired: () -> Unit) {
        Log.d("StateMachine", "Timer started: $durationMs ms")
        timerJob = scope.launch {
            try {
                delay(durationMs)
                Log.d("StateMachine", "Timer expired after $durationMs ms")
                onExpired()
            } catch (e: kotlinx.coroutines.CancellationException) {
                // キャンセルは正常動作（次の記事へ移動時など）
                Log.d("StateMachine", "Timer cancelled (normal)")
            } catch (e: Exception) {
                Log.e("StateMachine", "Timer error: ${e.message}", e)
            }
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
