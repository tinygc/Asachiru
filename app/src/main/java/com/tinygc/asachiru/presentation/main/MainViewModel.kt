package com.tinygc.asachiru.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinygc.asachiru.domain.common.IMusicPlayer
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.News
import com.tinygc.asachiru.domain.repository.SettingsRepository
import com.tinygc.asachiru.domain.usecase.clock.GetCurrentDateTimeUseCase
import com.tinygc.asachiru.domain.usecase.music.GetCurrentTrackUseCase
import com.tinygc.asachiru.domain.usecase.music.PlayMusicUseCase
import com.tinygc.asachiru.domain.usecase.news.GetLatestNewsUseCase
import com.tinygc.asachiru.domain.usecase.news.ReadNewsUseCase
import com.tinygc.asachiru.domain.usecase.weather.GetWeatherUseCase
import com.tinygc.asachiru.domain.usecase.weather.RefreshWeatherUseCase
import com.tinygc.asachiru.presentation.util.FlowTimer
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
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

    // セッション内の既読ニュースIDを保持（idはlinkを想定）
    private val readNewsIds = mutableSetOf<String>()
    
    // 現在実行中のニュース表示/読み上げJob
    private var currentNewsJob: Job? = null
    
    // ニュースリストと現在のインデックス
    private var currentNewsList: List<News> = emptyList()
    private var currentNewsIndex: Int = -1

    init {
        if (!skipAutoStart) {
            startClockUpdate()
            loadWeather()
            startWeatherAutoRefresh()
            loadTtsSettings()
            startNewsReading()
            startMusicPlayback()
            startTrackInfoUpdate()
        }
    }

    /**
     * TTS設定を読み込む
     */
    private fun loadTtsSettings() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            _uiState.update { it.copy(enableTts = settings.enableTts) }
        }
    }

    /**
     * 時計の定期更新を開始
     */
    private fun startClockUpdate() {
        FlowTimer.ticker(intervalMillis = clockUpdateIntervalMs)
            .onEach {
                val dateTime = getCurrentDateTimeUseCase()
                _uiState.update { it.copy(dateTime = dateTime) }
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
     * ニュース読み上げを開始
     * 初回は10秒待機、その後は設定された間隔で読み上げ
     */
    private fun startNewsReading() {
        viewModelScope.launch {
            // 初回は10秒待機
            delay(10_000L)

            // 設定された間隔を取得
            val settings = settingsRepository.getSettings()
            val intervalMs = settings.newsIntervalMinutes * 60 * 1000L

            FlowTimer.ticker(intervalMillis = intervalMs)
                .onEach { readNews() }
                .launchIn(this) // viewModelScope.launch内なので、thisを使用
        }
    }

    /**
     * ニュースを読み上げる
     */
    private suspend fun readNews() {
        _uiState.update { it.copy(isNewsLoading = true) }

        when (val result = getLatestNewsUseCase(10)) {
            is Result.Success -> {
                // デバッグ情報を更新
                _uiState.update {
                    it.copy(
                        debugNewsList = result.data,
                        debugLastFetchTime = System.currentTimeMillis()
                    )
                }

                // 既読を除外
                val unread = result.data.filter { it.id !in readNewsIds }

                if (unread.isEmpty()) {
                    // 未読なし
                    _uiState.update {
                        it.copy(
                            isNewsLoading = false,
                            newsError = null,
                            currentNews = null
                        )
                    }
                    currentNewsList = emptyList()
                    currentNewsIndex = -1
                    return
                }

                // ニュースリストを保存
                currentNewsList = unread
                currentNewsIndex = 0

                _uiState.update {
                    it.copy(
                        isNewsLoading = false,
                        newsError = null
                    )
                }

                // 既存のニュース表示/読み上げJobをキャンセル
                currentNewsJob?.cancel()
                
                // 新しいJobを開始
                currentNewsJob = viewModelScope.launch {
                    // TTS有効時のみ読み上げ実行
                    if (_uiState.value.enableTts) {
                        readNewsUseCase(
                            newsList = unread,
                            onNewsChanged = { news ->
                                readNewsIds += news.id
                                _uiState.update { it.copy(currentNews = news) }
                            },
                            onComplete = {
                                _uiState.update { it.copy(currentNews = null) }
                            }
                        )
                    } else {
                        // TTS無効時は文字数に応じた時間で次のニュースを表示（一時停止可能）
                        unread.forEachIndexed { index, news ->
                            if (index > 0) {
                                readNewsUseCase.pauseableDelay(5000L)
                            }
                            readNewsIds += news.id
                            _uiState.update { it.copy(currentNews = news) }
                            
                            // タイトル+説明文の文字数から表示時間を計算（読み上げ速度に合わせて）
                            val textLength = news.title.length + news.description.length
                            // 1文字あたり約200ms（読み上げ速度に近い）+ 余裕で1秒追加
                            val displayTime = (textLength * 200L) + 1000L
                            readNewsUseCase.pauseableDelay(displayTime)
                        }
                        _uiState.update { it.copy(currentNews = null) }
                    }
                }
            }
            is Result.Error -> {
                _uiState.update {
                    it.copy(
                        isNewsLoading = false,
                        newsError = result.exception.message
                    )
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
        // 音楽再生を再開
        startMusicPlayback()
    }

    /**
     * Background移行時の処理
     */
    fun onPause() {
        // TTS停止
        readNewsUseCase.stopReading()
        // ニュース自動送りJob停止
        currentNewsJob?.cancel()
        // 音楽停止はonStopでやるから、ここでは何もしないよ☆
        
    }

    /**
     * 完全に停止時の処理
     */
    fun onStop() {
        // 音楽を停止
        musicPlayer.stop()
    }

    /**
     * ニュース詳細表示を切り替える
     */
    fun toggleNewsDetail() {
        viewModelScope.launch {
            val newShowDetail = !_uiState.value.showNewsDetail
            _uiState.update { it.copy(showNewsDetail = newShowDetail) }

            // ポップアップ表示時はニュース送りを一時停止、閉じたら再開
            if (newShowDetail) {
                readNewsUseCase.pause()
            } else {
                readNewsUseCase.resume()
            }
        }
    }

    /**
     * ニュース詳細を閉じる
     */
    fun closeNewsDetail() {
        _uiState.update { it.copy(showNewsDetail = false) }
        // ニュース送りを再開
        readNewsUseCase.resume()
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
            } else {
                // TTS ONにした場合は現在のニュースから読み上げ開始
                currentNewsJob?.cancel()
                val currentNews = _uiState.value.currentNews
                if (currentNews != null && currentNewsList.isNotEmpty() && currentNewsIndex >= 0) {
                    // 現在表示中のニュースがあれば、そのニュースを読み上げてから残りも継続
                    currentNewsJob = viewModelScope.launch {
                        // 現在のニュースを読み上げ
                        readNewsUseCase(
                            newsList = listOf(currentNews),
                            onNewsChanged = { },
                            onComplete = { }
                        )
                        
                        // 残りのニュースがあれば続けて読み上げ
                        val remainingNews = currentNewsList.drop(currentNewsIndex + 1)
                        if (remainingNews.isNotEmpty()) {
                            readNewsUseCase(
                                newsList = remainingNews,
                                onNewsChanged = { news ->
                                    currentNewsIndex++
                                    readNewsIds += news.id
                                    _uiState.update { it.copy(currentNews = news) }
                                },
                                onComplete = {
                                    _uiState.update { it.copy(currentNews = null) }
                                }
                            )
                        } else {
                            _uiState.update { it.copy(currentNews = null) }
                        }
                    }
                } else {
                    // 表示中のニュースがなければ新しく取得
                    readNews()
                }
            }
        }
    }

    /**
     * 前のニュースへ移動
     */
    fun navigateToPreviousNews() {
        if (currentNewsList.isEmpty()) return
        
        currentNewsJob?.cancel()
        readNewsUseCase.stopReading()
        
        // インデックスを1つ戻す（0未満にならないように）
        currentNewsIndex = maxOf(0, currentNewsIndex - 1)
        val news = currentNewsList[currentNewsIndex]
        
        _uiState.update { it.copy(currentNews = news) }
        
        // 手動選択後も自動送りを継続（残りのニュースを再開）
        currentNewsJob = viewModelScope.launch {
            if (_uiState.value.enableTts) {
                // TTS ON: 選択したニュースを読み上げてから残りを継続
                readNewsUseCase(
                    newsList = listOf(news),
                    onNewsChanged = { },
                    onComplete = { }
                )
            }
            
            // 残りのニュースを自動送り
            val remainingNews = currentNewsList.drop(currentNewsIndex + 1)
            if (remainingNews.isNotEmpty()) {
                if (_uiState.value.enableTts) {
                    readNewsUseCase(
                        newsList = remainingNews,
                        onNewsChanged = { nextNews ->
                            currentNewsIndex++
                            readNewsIds += nextNews.id
                            _uiState.update { it.copy(currentNews = nextNews) }
                        },
                        onComplete = {
                            _uiState.update { it.copy(currentNews = null) }
                        }
                    )
                } else {
                    // TTS OFF時の自動送り
                    remainingNews.forEachIndexed { index, nextNews ->
                        if (index > 0) {
                            readNewsUseCase.pauseableDelay(5000L)
                        }
                        currentNewsIndex++
                        readNewsIds += nextNews.id
                        _uiState.update { it.copy(currentNews = nextNews) }
                        
                        val textLength = nextNews.title.length + nextNews.description.length
                        val displayTime = (textLength * 200L) + 1000L
                        readNewsUseCase.pauseableDelay(displayTime)
                    }
                    _uiState.update { it.copy(currentNews = null) }
                }
            }
        }
    }

    /**
     * 次のニュースへ移動
     */
    fun navigateToNextNews() {
        if (currentNewsList.isEmpty()) return
        
        currentNewsJob?.cancel()
        readNewsUseCase.stopReading()
        
        // インデックスを1つ進める（リスト末尾を超えないように）
        currentNewsIndex = minOf(currentNewsList.size - 1, currentNewsIndex + 1)
        val news = currentNewsList[currentNewsIndex]
        
        _uiState.update { it.copy(currentNews = news) }
        
        // 手動選択後も自動送りを継続（残りのニュースを再開）
        currentNewsJob = viewModelScope.launch {
            if (_uiState.value.enableTts) {
                // TTS ON: 選択したニュースを読み上げてから残りを継続
                readNewsUseCase(
                    newsList = listOf(news),
                    onNewsChanged = { },
                    onComplete = { }
                )
            }
            
            // 残りのニュースを自動送り
            val remainingNews = currentNewsList.drop(currentNewsIndex + 1)
            if (remainingNews.isNotEmpty()) {
                if (_uiState.value.enableTts) {
                    readNewsUseCase(
                        newsList = remainingNews,
                        onNewsChanged = { nextNews ->
                            currentNewsIndex++
                            readNewsIds += nextNews.id
                            _uiState.update { it.copy(currentNews = nextNews) }
                        },
                        onComplete = {
                            _uiState.update { it.copy(currentNews = null) }
                        }
                    )
                } else {
                    // TTS OFF時の自動送り
                    remainingNews.forEachIndexed { index, nextNews ->
                        if (index > 0) {
                            readNewsUseCase.pauseableDelay(5000L)
                        }
                        currentNewsIndex++
                        readNewsIds += nextNews.id
                        _uiState.update { it.copy(currentNews = nextNews) }
                        
                        val textLength = nextNews.title.length + nextNews.description.length
                        val displayTime = (textLength * 200L) + 1000L
                        readNewsUseCase.pauseableDelay(displayTime)
                    }
                    _uiState.update { it.copy(currentNews = null) }
                }
            }
        }
    }
}
