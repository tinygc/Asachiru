package com.tinygc.asachiru.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinygc.asachiru.domain.common.Result
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
                    return
                }

                _uiState.update {
                    it.copy(
                        isNewsLoading = false,
                        newsError = null
                    )
                }

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
                    // TTS無効時は最初のニュースを表示のみ
                    readNewsIds += unread.first().id
                    _uiState.update { it.copy(currentNews = unread.first()) }
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
                _uiState.update { it.copy(currentTrack = currentTrack) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Foreground復帰時の処理
     */
    fun onResume() {
        refreshWeather()
    }

    /**
     * ニュース詳細表示を切り替える
     */
    fun toggleNewsDetail() {
        viewModelScope.launch {
            val newShowDetail = !_uiState.value.showNewsDetail
            _uiState.update { it.copy(showNewsDetail = newShowDetail) }

            // 詳細表示時にTTS ONなら読み上げ
            if (newShowDetail && _uiState.value.enableTts) {
                _uiState.value.currentNews?.let { news ->
                    readNewsUseCase(
                        newsList = listOf(news),
                        onNewsChanged = { },
                        onComplete = { }
                    )
                }
            }
        }
    }

    /**
     * ニュース詳細を閉じる
     */
    fun closeNewsDetail() {
        _uiState.update { it.copy(showNewsDetail = false) }
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
        }
    }
}
