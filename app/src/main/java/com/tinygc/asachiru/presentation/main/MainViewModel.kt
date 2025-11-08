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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        startClockUpdate()
        loadWeather()
        startWeatherAutoRefresh()
        startNewsReading()
        startMusicPlayback()
        startTrackInfoUpdate()
    }

    /**
     * 時計の定期更新を開始（1秒ごと）
     */
    private fun startClockUpdate() {
        viewModelScope.launch {
            while (isActive) {
                val dateTime = getCurrentDateTimeUseCase()
                _uiState.update { currentState ->
                    currentState.copy(dateTime = dateTime)
                }
                delay(1000) // 1秒ごと
            }
        }
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
     * 天気情報の自動更新を開始（30分ごと）
     */
    private fun startWeatherAutoRefresh() {
        viewModelScope.launch {
            while (isActive) {
                delay(30 * 60 * 1000L) // 30分
                refreshWeather()
            }
        }
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

            while (isActive) {
                readNews()

                // 設定された間隔で待機
                val settings = settingsRepository.getSettings()
                val intervalMs = settings.newsIntervalMinutes * 60 * 1000L
                delay(intervalMs)
            }
        }
    }

    /**
     * ニュースを読み上げる
     */
    private suspend fun readNews() {
        _uiState.update { it.copy(isNewsLoading = true) }

        when (val result = getLatestNewsUseCase(10)) {
            is Result.Success -> {
                _uiState.update {
                    it.copy(
                        isNewsLoading = false,
                        newsError = null
                    )
                }

                // 読み上げ実行
                readNewsUseCase(
                    newsList = result.data,
                    onNewsChanged = { news ->
                        _uiState.update { it.copy(currentNews = news) }
                    },
                    onComplete = {
                        _uiState.update { it.copy(currentNews = null) }
                    }
                )
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
     * 曲情報の定期更新（1秒ごと）
     */
    private fun startTrackInfoUpdate() {
        viewModelScope.launch {
            while (isActive) {
                val currentTrack = getCurrentTrackUseCase()
                _uiState.update { it.copy(currentTrack = currentTrack) }
                delay(1000L) // 1秒ごと
            }
        }
    }

    /**
     * Foreground復帰時の処理
     */
    fun onResume() {
        refreshWeather()
    }
}
