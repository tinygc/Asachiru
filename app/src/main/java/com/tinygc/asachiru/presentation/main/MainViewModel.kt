package com.tinygc.asachiru.presentation.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinygc.asachiru.domain.common.IMusicPlayer
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.News
import com.tinygc.asachiru.domain.model.News as DomainNews
import com.tinygc.asachiru.domain.repository.SettingsRepository
import com.tinygc.asachiru.domain.usecase.clock.GetCurrentDateTimeUseCase
import com.tinygc.asachiru.domain.usecase.clock.ConvertTimestampToDateTimeUseCase
import com.tinygc.asachiru.domain.usecase.music.GetCurrentTrackUseCase
import com.tinygc.asachiru.domain.usecase.music.PlayMusicUseCase
import com.tinygc.asachiru.domain.usecase.news.GetLatestNewsUseCase
import com.tinygc.asachiru.domain.usecase.news.ReadNewsUseCase
import com.tinygc.asachiru.domain.usecase.weather.GetWeatherUseCase
import com.tinygc.asachiru.domain.usecase.weather.RefreshWeatherUseCase
import com.tinygc.asachiru.presentation.util.FlowTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * メイン画面のViewModel
 *
 * 全ての機能（時計、天気、ニュース、音楽）を統合し、
 * StateFlowで状態を管理します。
 * ニュース読み上げは NewsReadingStateMachine で管理します。
 */
class MainViewModel(
    private val getCurrentDateTimeUseCase: GetCurrentDateTimeUseCase,
    private val convertTimestampToDateTimeUseCase: ConvertTimestampToDateTimeUseCase,
    private val getWeatherUseCase: GetWeatherUseCase,
    private val refreshWeatherUseCase: RefreshWeatherUseCase,
    private val getLatestNewsUseCase: GetLatestNewsUseCase,
    private val readNewsUseCase: ReadNewsUseCase,
    private val playMusicUseCase: PlayMusicUseCase,
    private val getCurrentTrackUseCase: GetCurrentTrackUseCase,
    private val settingsRepository: SettingsRepository,
    private val readArticleRepository: com.tinygc.asachiru.domain.repository.ReadArticleRepository,
    private val musicPlayer: IMusicPlayer,
    // テスト用のパラメータ（デフォルト値は本番用）
    private val clockUpdateIntervalMs: Long = 1000L,
    private val trackUpdateIntervalMs: Long = 1000L,
    private val weatherRefreshIntervalMs: Long = 30 * 60 * 1000L, // 30分ごとに更新
    // テスト時にinitブロックの自動起動をスキップするフラグ
    private val skipAutoStart: Boolean = false
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // State Machine (publicにしてMainActivityからアクセス可能に)
    val stateMachine = NewsReadingStateMachine(
        scope = viewModelScope,
        onFetchNews = { fetchNewsArticles() },
        onReadArticle = { article, ttsEnabled -> readArticle(article, ttsEnabled) },
        onMarkAsRead = { articleId -> markArticleAsRead(articleId) }
    )

    init {
        if (!skipAutoStart) {
            // アプリ起動時に既読情報をクリア
            clearReadArticles()
            startClockUpdate()
            loadWeather()
            startWeatherAutoRefresh()
            // BGM設定を読み込んでから再生開始
            viewModelScope.launch {
                val settings = settingsRepository.getSettings()
                _uiState.update { it.copy(enableBgm = settings.enableBgm) }
                if (settings.enableBgm) {
                    startMusicPlayback()
                }
            }
            startTrackInfoUpdate()
            startNewsStateMachine()
            observeStateMachine()
            startTtsSettingsMonitor()
        }
    }


    /**
     * State Machineを開始
     */
    private fun startNewsStateMachine() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            stateMachine.handleEvent(NewsReadingEvent.AppStarted, settings)
        }
    }

    /**
     * State Machineの状態を監視してUIStateを更新
     */
    private fun observeStateMachine() {
        viewModelScope.launch {
            stateMachine.state.collect { state ->
                when (state) {
                    is NewsReadingState.ReadingArticle -> {
                        // 時計は常に現在時刻を表示（記事の時刻は使わない）
                        val dateTime = getCurrentDateTimeUseCase()
                        _uiState.update { 
                            it.copy(
                                dateTime = dateTime,
                                currentNews = convertToEntityNews(state.article),
                                newsProgressPercent = calculateProgress(state),
                                showAd = false, // 記事表示中は広告非表示
                                adRemainingSeconds = 0L,
                                currentArticleIndex = state.articleIndex,
                                totalArticles = state.totalArticles
                            ) 
                        }
                    }
                    is NewsReadingState.ArticleInterval -> {
                        val dateTime = getCurrentDateTimeUseCase()
                        _uiState.update { 
                            it.copy(
                                dateTime = dateTime,
                                currentNews = null,
                                newsProgressPercent = calculateProgress(state),
                                showAd = false, // インターバル中も広告非表示
                                adRemainingSeconds = 0L,
                                currentArticleIndex = state.nextArticleIndex - 1,
                                totalArticles = state.totalArticles,
                                nextEventRemainingMinutes = calculateNextEventRemainingMinutes(state)
                            ) 
                        }
                    }
                    is NewsReadingState.SessionInterval -> {
                        // 広告表示制御
                        val dateTime = getCurrentDateTimeUseCase()
                        val currentTimeMs = System.currentTimeMillis()
                        val showAd = state.showAd && currentTimeMs < state.adEndTimeMs
                        val adRemainingSeconds = if (showAd) {
                            ((state.adEndTimeMs - currentTimeMs) / 1000L).coerceAtLeast(0L)
                        } else {
                            0L
                        }
                        _uiState.update { 
                            it.copy(
                                dateTime = dateTime,
                                currentNews = null,
                                newsProgressPercent = 0f,
                                showAd = showAd,
                                adRemainingSeconds = adRemainingSeconds,
                                currentArticleIndex = 0,
                                totalArticles = 0,
                                nextEventRemainingMinutes = calculateNextEventRemainingMinutes(state)
                            ) 
                        }
                    }
                    else -> {
                        val dateTime = getCurrentDateTimeUseCase()
                        _uiState.update { 
                            it.copy(
                                dateTime = dateTime,
                                currentNews = null,
                                newsProgressPercent = 0f,
                                showAd = false, // SessionInterval以外の状態では広告非表示
                                adRemainingSeconds = 0L,
                                currentArticleIndex = 0,
                                totalArticles = 0
                            ) 
                        }
                    }
                }
            }
        }
    }

    /**
     * プログレスバーの進行度を計算（0.0～1.0）
     * TTS OFF時のみ有効、TTS ON時は0を返す
     */
    private fun calculateProgress(state: NewsReadingState): Float {
        // TTS ONの場合はプログレスバーを表示しない
        if (_uiState.value.enableTts) {
            return 0f
        }

        return when (state) {
            is NewsReadingState.ReadingArticle -> {
                if (state.estimatedEndTimeMs == 0L) {
                    // TTS ON時
                    0f
                } else {
                    // 開始時刻を逆算（タイトル×7倍で計算）
                    val remainingMs = (state.estimatedEndTimeMs - System.currentTimeMillis()).coerceAtLeast(0L)
                    val totalDurationMs = state.article.title.length * 7 * 200L + 1000L
                    val elapsedMs = totalDurationMs - remainingMs
                    (elapsedMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                }
            }
            is NewsReadingState.ArticleInterval -> {
                // 記事間インターバル（5秒）のプログレス
                val remainingMs = (state.endTimeMs - System.currentTimeMillis()).coerceAtLeast(0L)
                val totalDurationMs = 5000L
                val elapsedMs = totalDurationMs - remainingMs
                (elapsedMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
            }
            else -> 0f
        }
    }

    /**
     * 次のイベント（記事取得・読み上げ）までの残り時間（分単位）を計算
     * 記事がない状態でユーザーに表示するための値
     */
    private fun calculateNextEventRemainingMinutes(state: NewsReadingState): Long {
        return when (state) {
            is NewsReadingState.ArticleInterval -> {
                // 次の記事まで5秒
                1L // 1分未満は1分と表示
            }
            is NewsReadingState.SessionInterval -> {
                // セッション間待機（広告10秒または5分）
                val remainingMs = (state.endTimeMs - System.currentTimeMillis()).coerceAtLeast(0L)
                val remainingMinutes = (remainingMs + 59_999) / 60_000L // 切り上げ
                remainingMinutes.coerceAtLeast(1L)
            }
            is NewsReadingState.FetchingNews -> {
                // ニュース取得中
                1L // 1分未満は1分と表示
            }
            is NewsReadingState.WaitingForStart -> {
                // 初回開始待機
                1L // 1分未満は1分と表示
            }
            else -> 0L // 記事表示中またはIdle時は表示しない
        }
    }

    /**
     * TTS設定の変更を監視
     */
    private fun startTtsSettingsMonitor() {
        viewModelScope.launch {
            var previousEnableTts: Boolean? = null
            while (true) {
                val settings = settingsRepository.getSettings()
                val currentEnableTts = settings.enableTts
                
                if (previousEnableTts != null && previousEnableTts != currentEnableTts) {
                    // TTS設定変更イベントを発火
                    stateMachine.handleEvent(NewsReadingEvent.TtsSettingChanged(currentEnableTts), settings)
                }
                
                _uiState.update { it.copy(enableTts = currentEnableTts) }
                previousEnableTts = currentEnableTts
                
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    /**
     * 既読記事情報をクリア
     */
    fun clearReadArticles() {
        viewModelScope.launch {
            try {
                readArticleRepository.clearAll()
                Log.d("MainViewModel", "既読記事情報をクリアしました")
            } catch (e: Exception) {
                Log.e("MainViewModel", "既読記事情報のクリアに失敗", e)
            }
        }
    }

    /**
     * ニュース記事を取得
     */
    private suspend fun fetchNewsArticles(): com.tinygc.asachiru.domain.model.NewsResult {
        _uiState.update { it.copy(isNewsLoading = true) }

        return when (val result = getLatestNewsUseCase(10)) {
            is Result.Success -> {
                _uiState.update {
                    it.copy(
                        isNewsLoading = false,
                        newsError = null,
                        debugNewsList = result.data.allArticles,
                        debugLastFetchTime = System.currentTimeMillis()
                    )
                }
                // domain.entity.NewsResult から domain.model.NewsResult に変換
                com.tinygc.asachiru.domain.model.NewsResult(
                    allArticles = result.data.allArticles.map { convertToDomainNews(it) },
                    newArticles = result.data.newArticles.map { convertToDomainNews(it) }
                )
            }
            is Result.Error -> {
                _uiState.update {
                    it.copy(
                        isNewsLoading = false,
                        newsError = result.exception.message
                    )
                }
                com.tinygc.asachiru.domain.model.NewsResult(emptyList(), emptyList())
            }
        }
    }

    /**
     * 記事を読み上げ/表示
     */
    private suspend fun readArticle(article: com.tinygc.asachiru.domain.model.News, ttsEnabled: Boolean) {
        val entityNews = convertToEntityNews(article)
        if (ttsEnabled) {
            // 読み上げ開始
            _uiState.update { it.copy(isSpeaking = true) }
            readNewsUseCase(
                newsList = listOf(entityNews),
                onNewsChanged = { },
                onComplete = {
                    // 読み上げ完了
                    _uiState.update { it.copy(isSpeaking = false) }
                }
            )
        }
        // TTS無効時は表示のみ（State Machineがタイマー管理）
    }

    /**
     * domain.entity.News から domain.model.News に変換
     */
    private fun convertToDomainNews(entityNews: News): DomainNews {
        return DomainNews(
            id = entityNews.id,
            title = entityNews.title,
            description = entityNews.description,
            link = entityNews.id, // entityにはlinkフィールドがないのでidを使用
            imageUrl = null, // entityにはimageUrlフィールドがない
            publishedAt = entityNews.publishedAt // 公開時刻を保持
        )
    }

    /**
     * domain.model.News から domain.entity.News に変換
     */
    private fun convertToEntityNews(domainNews: DomainNews): News {
        return News(
            id = domainNews.id,
            title = domainNews.title,
            description = domainNews.description,
            publishedAt = domainNews.publishedAt // 記事の公開時刻を使用
        )
    }


    /**
     * 時計の定期更新を開始
     */
    private fun startClockUpdate() {
        FlowTimer.ticker(intervalMillis = clockUpdateIntervalMs)
            .onEach {
                val dateTime = getCurrentDateTimeUseCase()
                
                // State Machineから残り時間とプログレスを取得
                val currentState = stateMachine.state.value
                val remainingSeconds = currentState.getRemainingSeconds()
                val progress = calculateProgress(currentState)
                
                // 広告表示中の残り時間を更新
                val (showAd, adRemainingSeconds) = if (currentState is NewsReadingState.SessionInterval) {
                    val currentTimeMs = System.currentTimeMillis()
                    val adActive = currentState.showAd && currentTimeMs < currentState.adEndTimeMs
                    val adRemaining = if (adActive) {
                        ((currentState.adEndTimeMs - currentTimeMs) / 1000L).coerceAtLeast(0L)
                    } else {
                        0L
                    }
                    Pair(adActive, adRemaining)
                } else {
                    Pair(_uiState.value.showAd, _uiState.value.adRemainingSeconds)
                }
                
                _uiState.update { 
                    it.copy(
                        dateTime = dateTime,
                        debugNextNewsRemainingSeconds = remainingSeconds,
                        newsProgressPercent = progress,
                        showAd = showAd,
                        adRemainingSeconds = adRemainingSeconds
                    ) 
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 天気情報を取得
     */
    private fun loadWeather() {
        viewModelScope.launch {
            _uiState.update { it.copy(isWeatherLoading = true) }

            when (val result = getWeatherUseCase()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            weather = result.data,
                            isWeatherLoading = false,
                            weatherError = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isWeatherLoading = false,
                            weatherError = result.exception.message
                        )
                    }
                }
            }
        }
    }

    /**
     * 天気情報の自動更新を開始
     */
    private fun startWeatherAutoRefresh() {
        FlowTimer.ticker(
            intervalMillis = weatherRefreshIntervalMs,
            initialDelayMillis = weatherRefreshIntervalMs
        )
            .onEach { refreshWeather() }
            .launchIn(viewModelScope)
    }

    /**
     * 天気情報を手動で再取得
     */
    fun refreshWeather() {
        viewModelScope.launch {
            _uiState.update { it.copy(isWeatherLoading = true) }

            when (val result = refreshWeatherUseCase()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            weather = result.data,
                            isWeatherLoading = false,
                            weatherError = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isWeatherLoading = false,
                            weatherError = result.exception.message
                        )
                    }
                }
            }
        }
    }

    /**
     * 音楽再生を開始
     */
    private fun startMusicPlayback() {
        playMusicUseCase()
        _uiState.update { it.copy(isMusicPlaying = true) }
    }

    /**
     * 曲情報の定期更新
     */
    private fun startTrackInfoUpdate() {
        FlowTimer.ticker(intervalMillis = trackUpdateIntervalMs)
            .onEach {
                val currentTrack = getCurrentTrackUseCase()
                val currentPosition = musicPlayer.getCurrentPosition()
                _uiState.update { it.copy(currentTrack = currentTrack, currentPosition = currentPosition) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Foreground復帰時の処理
     */
    fun onResume() {
        refreshWeather()
        // BGM設定を確認してから再生を再開
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            _uiState.update { it.copy(enableBgm = settings.enableBgm) }
            if (settings.enableBgm) {
                playMusicUseCase()
            }
            stateMachine.handleEvent(NewsReadingEvent.ForegroundTransition, settings)
        }
    }

    /**
     * Background移行時の処理
     */
    fun onPause() {
        // TTS停止
        readNewsUseCase.stopReading()
        // BGM停止
        musicPlayer.stop()
        // State Machineにイベント通知
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            stateMachine.handleEvent(NewsReadingEvent.BackgroundTransition, settings)
        }
    }

    /**
     * 完全に停止時の処理
     */
    fun onStop() {
        // BGM停止
        musicPlayer.stop()
    }

    /**
     * ニュース詳細表示を切り替える
     */
    fun toggleNewsDetail() {
        viewModelScope.launch {
            val newShowDetail = !_uiState.value.showNewsDetail
            _uiState.update { it.copy(showNewsDetail = newShowDetail) }

            val settings = settingsRepository.getSettings()
            if (newShowDetail) {
                // 詳細表示を開く
                stateMachine.handleEvent(NewsReadingEvent.DetailOpened, settings)
                readNewsUseCase.pause()
            } else {
                // 詳細表示を閉じる
                stateMachine.handleEvent(NewsReadingEvent.DetailClosed, settings)
                readNewsUseCase.resume()
            }
        }
    }

    /**
     * ニュース詳細を閉じる
     */
    fun closeNewsDetail() {
        _uiState.update { it.copy(showNewsDetail = false) }
        readNewsUseCase.resume()
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            stateMachine.handleEvent(NewsReadingEvent.DetailClosed, settings)
        }
    }

    /**
     * TTS ON/OFFを切り替える
     */
    fun toggleTts() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            val newEnableTts = !settings.enableTts
            val newSettings = settings.copy(enableTts = newEnableTts)
            settingsRepository.saveSettings(newSettings)
            _uiState.update { it.copy(enableTts = newEnableTts) }
            
            if (!newEnableTts) {
                // TTS OFFにした場合は即座に読み上げを停止
                readNewsUseCase.stopReading()
            }
            // TTS設定変更はstartTtsSettingsMonitor()が検知してイベント発火
        }
    }

    /**
     * 前のニュースへ移動
     */
    fun navigateToPreviousNews() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            val currentState = stateMachine.state.value
            
            // 広告表示中は上キー無効
            if (currentState is NewsReadingState.SessionInterval && currentState.showAd) {
                Log.d("MainViewModel", "navigateToPreviousNews: Ad is showing, ignoring")
                return@launch
            }
            
            // 最初の記事の場合は何もしない(読み上げを継続)
            if (currentState is NewsReadingState.ReadingArticle &&
                currentState.articleIndex <= 0) {
                Log.d("MainViewModel", "navigateToPreviousNews: Already at first article, ignoring")
                return@launch
            }
            
            readNewsUseCase.stopReading() // 現在の読み上げを停止
            stateMachine.handleEvent(NewsReadingEvent.NavigateToPrevious, settings)
        }
    }

    /**
     * 次のニュースへ移動
     */
    fun navigateToNextNews() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            val currentState = stateMachine.state.value
            
            // 広告表示中は下キー無効
            if (currentState is NewsReadingState.SessionInterval && currentState.showAd) {
                Log.d("MainViewModel", "navigateToNextNews: Ad is showing, ignoring")
                return@launch
            }
            
            // 最後の記事の場合は何もしない(読み上げを継続)
            if (currentState is NewsReadingState.ReadingArticle &&
                currentState.articleIndex >= currentState.totalArticles - 1) {
                Log.d("MainViewModel", "navigateToNextNews: Already at last article, ignoring")
                return@launch
            }
            
            readNewsUseCase.stopReading() // 現在の読み上げを停止
            stateMachine.handleEvent(NewsReadingEvent.NavigateToNext, settings)
        }
    }

    /**
     * 記事を既読としてマーク
     */
    private suspend fun markArticleAsRead(articleId: String) {
        readArticleRepository.markArticleAsRead(articleId)
    }

    override fun onCleared() {
        super.onCleared()
        stateMachine.cleanup()
    }
}

