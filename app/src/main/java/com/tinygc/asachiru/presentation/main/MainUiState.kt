package com.tinygc.asachiru.presentation.main

import com.tinygc.asachiru.domain.entity.DateTime
import com.tinygc.asachiru.domain.entity.Music
import com.tinygc.asachiru.domain.entity.News
import com.tinygc.asachiru.domain.entity.Weather

/**
 * メイン画面のUI状態
 *
 * StateFlowで管理され、各機能の状態を保持します。
 */
data class MainUiState(
    // 時計
    val dateTime: DateTime = DateTime.EMPTY,

    // 天気
    val weather: Weather? = null,
    val isWeatherLoading: Boolean = false,
    val weatherError: String? = null,

    // ニュース
    val currentNews: News? = null,
    val isNewsLoading: Boolean = false,
    val newsError: String? = null,
    val showNewsDetail: Boolean = false,
    val enableTts: Boolean = false,

    // 音楽
    val currentTrack: Music? = null,
    val isMusicPlaying: Boolean = false,

    // デバッグ情報
    val debugNewsList: List<News> = emptyList(),
    val debugLastFetchTime: Long = 0L
)
