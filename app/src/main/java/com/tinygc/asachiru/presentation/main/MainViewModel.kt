package com.tinygc.asachiru.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinygc.asachiru.domain.common.IMusicPlayer
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.News
import com.tinygc.asachiru.domain.model.News as DomainNews
import com.tinygc.asachiru.domain.repository.SettingsRepository
import com.tinygc.asachiru.domain.usecase.clock.GetCurrentDateTimeUseCase
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
    private val getWeatherUseCase: GetWeatherUseCase,
    private val refreshWeatherUseCase: RefreshWeatherUseCase,
    private val getLatestNewsUseCase: GetLatestNewsUseCase,
    private val readNewsUseCase: ReadNewsUseCase,
    private val playMusicUseCase: PlayMusicUseCase,
    private val getCurrentTrackUseCase: GetCurrentTrackUseCase,
    private val settingsRepository: SettingsRepository,
    private val musicPlayer: IMusicPlayer,
    // テスト用のパラメータ（デフォルト値は本番用）
    private val clockUpdateIntervalMs: Long = 1000L,
    private val trackUpdateIntervalMs: Long = 1000L,
    private val weatherRefreshIntervalMs: Long = 30 * 60 * 1000L,
    // テスト時にinitブロックの自動起動をスキップするフラグ
    private val skipAutoStart: Boolean = false
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // State Machine
    private val stateMachine = NewsReadingStateMachine(
        scope = viewModelScope,
        onFetchNews = { fetchNewsArticles() },
        onReadArticle = { article, ttsEnabled -> readArticle(article, ttsEnabled) }
    )

    init {
        if (!skipAutoStart) {
            startClockUpdate()
            loadWeather()
            startWeatherAutoRefresh()
            startMusicPlayback()
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
                        _uiState.update { 
                            it.copy(
                                currentNews = convertToEntityNews(state.article),
                                newsProgressPercent = calculateProgress(state)
                            ) 
                        }
                    }
                    is NewsReadingState.ArticleInterval -> {
                        _uiState.update { 
                            it.copy(
                                currentNews = null,
                                newsProgressPercent = calculateProgress(state)
                            ) 
                        }
                    }
                    else -> {
                        _uiState.update { 
                            it.copy(
                                currentNews = null,
                                newsProgressPercent = 0f
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
     * ニュース記事を取得
     */
    private suspend fun fetchNewsArticles(): List<com.tinygc.asachiru.domain.model.News> {
        _uiState.update { it.copy(isNewsLoading = true) }

        return when (val result = getLatestNewsUseCase(10)) {
            is Result.Success -> {
                _uiState.update {
                    it.copy(
                        isNewsLoading = false,
                        newsError = null,
                        debugNewsList = result.data,
                        debugLastFetchTime = System.currentTimeMillis()
                    )
                }
                // domain.entity.News から domain.model.News に変換
                result.data.map { convertToDomainNews(it) }
            }
            is Result.Error -> {
                _uiState.update {
                    it.copy(
                        isNewsLoading = false,
                        newsError = result.exception.message
                    )
                }
                emptyList()
            }
        }
    }

    /**
     * 記事を読み上げ/表示
     */
    private suspend fun readArticle(article: com.tinygc.asachiru.domain.model.News, ttsEnabled: Boolean) {
        val entityNews = convertToEntityNews(article)
        if (ttsEnabled) {
            readNewsUseCase(
                newsList = listOf(entityNews),
                onNewsChanged = { },
                onComplete = { }
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
            imageUrl = null // entityにはimageUrlフィールドがない
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
            publishedAt = System.currentTimeMillis() // publishedAtは現在時刻で代用
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
                
                _uiState.update { 
                    it.copy(
                        dateTime = dateTime,
                        debugNextNewsRemainingSeconds = remainingSeconds,
                        newsProgressPercent = progress
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
        // BGM再生を再開
        viewModelScope.launch {
            playMusicUseCase()
            val settings = settingsRepository.getSettings()
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
            readNewsUseCase.stopReading() // 現在の読み上げを停止
            stateMachine.handleEvent(NewsReadingEvent.NavigateToNext, settings)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stateMachine.cleanup()
    }
}
