# モジュール設計書 - 音楽再生・ビジュアライザー機能

## 1. 概要

音楽再生機能は、アプリに組み込まれたLo-Fi音源をループ再生する機能。
ビジュアライザー機能は、再生中の音楽に反応してスペクトラムアナライザーを表示する機能。

---

## 2. 機能要件（再掲）

### 2.1 音楽再生機能

#### 2.1.1 基本仕様
- **ジャンル**: Lo-Fi
- **音源**: フリー音源をアプリに組み込み
- **曲数**: 初期バージョンで3曲

#### 2.1.2 再生方式
- **再生モード**: ループ再生
- **クロスフェード**: 3秒間のクロスフェードで曲を繋ぐ

#### 2.1.3 音量調整
- アプリ側では音量調整機能を提供しない
- TV本体の主音量で調整

#### 2.1.4 表示
- 再生中の曲名を画面左下に表示

### 2.2 ビジュアライザー機能

#### 2.2.1 基本仕様
- **タイプ**: スペクトラムアナライザー（縦のバータイプ）
- **動作**: 再生中の音楽に反応してリアルタイムに表示
- **デザインイメージ**: 昔のステレオデッキ風、パステルカラーでカラフルに

#### 2.2.2 詳細仕様
- **バー本数**: 50本
- **配置**: 画面全体の下部に均等配置
- **バーの幅**: 16px
- **バーの間隔**: 6px
- **配色**: パステルレインボーカラー（8色を循環）
  - #FF6B9D（パステルピンク）
  - #FFA07A（パステルオレンジ）
  - #FFD93D（パステルイエロー）
  - #6BCF7F（パステルグリーン）
  - #4ECDC4（パステルシアン）
  - #95B8D1（パステルブルー）
  - #B8A9C9（パステルパープル）
  - #F38FB1（パステルローズ）
- **アニメーション**: 0.8秒周期で上下に変化
- **高さの範囲**: 15%～85%
- **不透明度**: 25%（背景として表示）

---

## 3. アーキテクチャ設計

### 3.1 レイヤー構成

```
[Presentation Layer]
  - MusicView (Custom View)
  - VisualizerView (Custom View)
  - MainViewModel
  - MusicPlayer (音楽プレイヤーラッパー)

[Domain Layer]
  - PlayMusicUseCase
  - GetCurrentTrackUseCase
  - Music (Entity)
  - MusicRepository (Interface)

[Data Layer]
  - MusicRepositoryImpl
  - MusicLocalDataSource
```

---

## 4. クラス設計

### 4.1 Domain Layer

#### 4.1.1 Music Entity

```kotlin
package com.tinygc.asachiru.domain.entity

/**
 * 音楽を表すエンティティ
 */
data class Music(
    val id: String,
    val title: String,
    val artist: String,
    val resourceId: Int, // res/raw/のリソースID
    val durationMs: Long
) {
    companion object {
        /**
         * 空のMusic（初期値用）
         */
        val EMPTY = Music(
            id = "",
            title = "",
            artist = "",
            resourceId = 0,
            durationMs = 0L
        )
    }
}
```

#### 4.1.2 MusicRepository Interface

```kotlin
package com.tinygc.asachiru.domain.repository

import com.tinygc.asachiru.domain.entity.Music

/**
 * 音楽情報を管理するリポジトリのインターフェース
 */
interface MusicRepository {
    /**
     * すべての曲を取得
     * @return 曲のリスト
     */
    fun getAllTracks(): List<Music>

    /**
     * 曲を再生
     * @param trackId 曲ID
     */
    fun playTrack(trackId: String)

    /**
     * 再生を停止
     */
    fun stopTrack()

    /**
     * 現在再生中の曲を取得
     * @return 現在再生中の曲（再生していない場合null）
     */
    fun getCurrentTrack(): Music?
}
```

#### 4.1.3 PlayMusicUseCase

```kotlin
package com.tinygc.asachiru.domain.usecase.music

import com.tinygc.asachiru.domain.repository.MusicRepository

/**
 * 音楽を再生するユースケース
 */
class PlayMusicUseCase(
    private val musicRepository: MusicRepository
) {
    /**
     * ループ再生を開始
     */
    operator fun invoke() {
        val tracks = musicRepository.getAllTracks()
        if (tracks.isNotEmpty()) {
            // 最初の曲から再生開始
            musicRepository.playTrack(tracks.first().id)
        }
    }
}
```

#### 4.1.4 GetCurrentTrackUseCase

```kotlin
package com.tinygc.asachiru.domain.usecase.music

import com.tinygc.asachiru.domain.entity.Music
import com.tinygc.asachiru.domain.repository.MusicRepository

/**
 * 現在再生中の曲を取得するユースケース
 */
class GetCurrentTrackUseCase(
    private val musicRepository: MusicRepository
) {
    /**
     * 現在再生中の曲を取得
     * @return 現在再生中の曲（再生していない場合null）
     */
    operator fun invoke(): Music? {
        return musicRepository.getCurrentTrack()
    }
}
```

### 4.2 Data Layer

#### 4.2.1 MusicLocalDataSource

```kotlin
package com.tinygc.asachiru.data.datasource.local

import com.tinygc.asachiru.R
import com.tinygc.asachiru.domain.entity.Music

/**
 * ローカルの音源データを管理するデータソース
 */
class MusicLocalDataSource {
    /**
     * すべての曲を取得
     * @return 曲のリスト
     */
    fun getAllTracks(): List<Music> {
        return listOf(
            Music(
                id = "lofi_01",
                title = "Chill Morning",
                artist = "Unknown Artist",
                resourceId = R.raw.lofi_01,
                durationMs = 180_000L // 3分
            ),
            Music(
                id = "lofi_02",
                title = "Peaceful Vibes",
                artist = "Unknown Artist",
                resourceId = R.raw.lofi_02,
                durationMs = 200_000L // 3分20秒
            ),
            Music(
                id = "lofi_03",
                title = "Relaxing Beats",
                artist = "Unknown Artist",
                resourceId = R.raw.lofi_03,
                durationMs = 190_000L // 3分10秒
            )
        )
    }
}
```

#### 4.2.2 MusicRepositoryImpl

```kotlin
package com.tinygc.asachiru.data.repository

import com.tinygc.asachiru.data.datasource.local.MusicLocalDataSource
import com.tinygc.asachiru.domain.entity.Music
import com.tinygc.asachiru.domain.repository.MusicRepository
import com.tinygc.asachiru.presentation.util.MusicPlayer

/**
 * MusicRepositoryの実装
 */
class MusicRepositoryImpl(
    private val musicLocalDataSource: MusicLocalDataSource,
    private val musicPlayer: MusicPlayer
) : MusicRepository {

    override fun getAllTracks(): List<Music> {
        return musicLocalDataSource.getAllTracks()
    }

    override fun playTrack(trackId: String) {
        val track = getAllTracks().find { it.id == trackId }
        track?.let {
            musicPlayer.play(it)
        }
    }

    override fun stopTrack() {
        musicPlayer.stop()
    }

    override fun getCurrentTrack(): Music? {
        return musicPlayer.getCurrentTrack()
    }
}
```

### 4.3 Presentation Layer

#### 4.3.1 MusicPlayer

```kotlin
package com.tinygc.asachiru.presentation.util

import android.content.Context
import android.media.MediaPlayer
import com.tinygc.asachiru.domain.entity.Music

/**
 * 音楽再生を管理するプレイヤークラス
 */
class MusicPlayer(private val context: Context) {

    private var currentPlayer: MediaPlayer? = null
    private var nextPlayer: MediaPlayer? = null
    private var currentTrack: Music? = null
    private var trackList: List<Music> = emptyList()
    private var currentTrackIndex = 0

    /**
     * トラックリストを設定
     */
    fun setTrackList(tracks: List<Music>) {
        this.trackList = tracks
    }

    /**
     * 曲を再生
     * @param track 再生する曲
     */
    fun play(track: Music) {
        stop()

        currentTrack = track
        currentTrackIndex = trackList.indexOf(track)

        currentPlayer = MediaPlayer.create(context, track.resourceId).apply {
            setOnCompletionListener {
                playNext()
            }
            start()
        }

        // 次の曲を準備（クロスフェード用）
        prepareNextTrack()
    }

    /**
     * 次の曲を準備
     */
    private fun prepareNextTrack() {
        if (trackList.isEmpty()) return

        val nextIndex = (currentTrackIndex + 1) % trackList.size
        val nextTrack = trackList[nextIndex]

        nextPlayer = MediaPlayer.create(context, nextTrack.resourceId)
    }

    /**
     * 次の曲を再生（クロスフェード）
     */
    private fun playNext() {
        // クロスフェード処理
        val fadeDurationMs = 3000L // 3秒
        fadeOut(currentPlayer, fadeDurationMs)

        currentPlayer = nextPlayer
        currentTrackIndex = (currentTrackIndex + 1) % trackList.size
        currentTrack = trackList[currentTrackIndex]

        currentPlayer?.apply {
            setOnCompletionListener {
                playNext()
            }
            start()
        }

        prepareNextTrack()
    }

    /**
     * フェードアウト処理
     * 指定された時間で音量を徐々に下げる
     * @param player 対象のMediaPlayer
     * @param durationMs フェードアウト時間（ミリ秒）
     */
    private fun fadeOut(player: MediaPlayer?, durationMs: Long) {
        if (player == null) return

        val handler = Handler(Looper.getMainLooper())
        val steps = 20 // フェードのステップ数
        val stepDuration = durationMs / steps
        var currentStep = 0

        val fadeRunnable = object : Runnable {
            override fun run() {
                if (currentStep < steps && player.isPlaying) {
                    // 音量を徐々に下げる（1.0 → 0.0）
                    val volume = 1.0f - (currentStep.toFloat() / steps)
                    player.setVolume(volume, volume)
                    currentStep++
                    handler.postDelayed(this, stepDuration)
                } else {
                    // フェードアウト完了
                    player.setVolume(0f, 0f)
                    player.stop()
                    player.release()
                }
            }
        }
        handler.post(fadeRunnable)
    }

    /**
     * 停止
     */
    fun stop() {
        currentPlayer?.stop()
        currentPlayer?.release()
        currentPlayer = null

        nextPlayer?.release()
        nextPlayer = null

        currentTrack = null
    }

    /**
     * 現在再生中の曲を取得
     */
    fun getCurrentTrack(): Music? {
        return currentTrack
    }

    /**
     * オーディオセッションIDを取得（ビジュアライザー用）
     */
    fun getAudioSessionId(): Int {
        return currentPlayer?.audioSessionId ?: 0
    }
}
```

#### 4.3.2 VisualizerView

```kotlin
package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.audiofx.Visualizer
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs

/**
 * スペクトラムアナライザーを表示するカスタムビュー
 */
class VisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val BAR_COUNT = 50
        private const val BAR_WIDTH = 16f
        private const val BAR_SPACING = 6f

        // パステルレインボーカラー
        private val PASTEL_COLORS = intArrayOf(
            Color.parseColor("#FF6B9D"), // パステルピンク
            Color.parseColor("#FFA07A"), // パステルオレンジ
            Color.parseColor("#FFD93D"), // パステルイエロー
            Color.parseColor("#6BCF7F"), // パステルグリーン
            Color.parseColor("#4ECDC4"), // パステルシアン
            Color.parseColor("#95B8D1"), // パステルブルー
            Color.parseColor("#B8A9C9"), // パステルパープル
            Color.parseColor("#F38FB1")  // パステルローズ
        )
    }

    private var visualizer: Visualizer? = null
    private val barHeights = FloatArray(BAR_COUNT) { 0.3f } // 初期値30%

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        alpha = (255 * 0.25f).toInt() // 25%不透明度
    }

    /**
     * ビジュアライザーを開始
     * @param audioSessionId オーディオセッションID
     */
    fun startVisualizer(audioSessionId: Int) {
        stopVisualizer()

        visualizer = Visualizer(audioSessionId).apply {
            captureSize = Visualizer.getCaptureSizeRange()[1]
            setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        // 波形データからバーの高さを計算
                        waveform?.let {
                            updateBarHeights(it)
                            invalidate()
                        }
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        // FFTデータは使用しない
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                true,
                false
            )
            enabled = true
        }
    }

    /**
     * 波形データからバーの高さを更新
     */
    private fun updateBarHeights(waveform: ByteArray) {
        val step = waveform.size / BAR_COUNT
        for (i in 0 until BAR_COUNT) {
            val index = i * step
            val value = abs(waveform[index].toInt())
            // 0-128の範囲を0.15-0.85にマッピング
            barHeights[i] = 0.15f + (value / 128f) * 0.7f
        }
    }

    /**
     * ビジュアライザーを停止
     */
    fun stopVisualizer() {
        visualizer?.enabled = false
        visualizer?.release()
        visualizer = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val startX = (width - (BAR_COUNT * (BAR_WIDTH + BAR_SPACING))) / 2

        for (i in 0 until BAR_COUNT) {
            val x = startX + i * (BAR_WIDTH + BAR_SPACING)
            val barHeight = height * barHeights[i]
            val y = height - barHeight

            // 色を循環
            barPaint.color = PASTEL_COLORS[i % PASTEL_COLORS.size]

            canvas.drawRect(
                x,
                y,
                x + BAR_WIDTH,
                height.toFloat(),
                barPaint
            )
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopVisualizer()
    }
}
```

#### 4.3.3 MainViewModel（音楽部分のみ抜粋）

```kotlin
package com.tinygc.asachiru.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinygc.asachiru.domain.usecase.music.PlayMusicUseCase
import com.tinygc.asachiru.domain.usecase.music.GetCurrentTrackUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(
    private val playMusicUseCase: PlayMusicUseCase,
    private val getCurrentTrackUseCase: GetCurrentTrackUseCase,
    // ... 他のUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        startMusicPlayback()
        startTrackInfoUpdate()
        // ... 他の初期化処理
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
        viewModelScope.launch {
            while (isActive) {
                val currentTrack = getCurrentTrackUseCase()
                _uiState.update { it.copy(currentTrack = currentTrack) }
                delay(1000L) // 1秒ごとに更新
            }
        }
    }
}
```

---

## 5. テスト設計

### 5.1 単体テスト

#### 5.1.1 PlayMusicUseCaseTest

```kotlin
package com.tinygc.asachiru.domain.usecase.music

import com.tinygc.asachiru.domain.entity.Music
import com.tinygc.asachiru.domain.repository.MusicRepository
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PlayMusicUseCaseTest {

    @Mock
    private lateinit var musicRepository: MusicRepository

    private lateinit var useCase: PlayMusicUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = PlayMusicUseCase(musicRepository)
    }

    @Test
    fun `invoke should play first track`() {
        // Arrange
        val tracks = listOf(
            Music("1", "Track 1", "Artist", 0, 0L),
            Music("2", "Track 2", "Artist", 0, 0L)
        )
        whenever(musicRepository.getAllTracks()).thenReturn(tracks)

        // Act
        useCase()

        // Assert
        verify(musicRepository).playTrack("1")
    }
}
```

---

## 6. ファイル構成

```
domain/
├── entity/
│   └── Music.kt
├── repository/
│   └── MusicRepository.kt
└── usecase/
    └── music/
        ├── PlayMusicUseCase.kt
        └── GetCurrentTrackUseCase.kt

data/
├── repository/
│   └── MusicRepositoryImpl.kt
└── datasource/
    └── local/
        └── MusicLocalDataSource.kt

presentation/
├── util/
│   └── MusicPlayer.kt
└── main/
    ├── MainViewModel.kt
    └── views/
        ├── MusicView.kt
        └── VisualizerView.kt
```

---

## 7. リソース

### 7.1 音源ファイル

```
res/raw/
├── lofi_01.mp3  (Chill Morning - 3分)
├── lofi_02.mp3  (Peaceful Vibes - 3分20秒)
└── lofi_03.mp3  (Relaxing Beats - 3分10秒)
```

**ライセンス:** フリー音源（商用利用可能なもの）

---

## 8. 承認

- 作成日: 2025-11-06
- 作成者: Claude
- バージョン: 1.0

---

**次のステップ:**
初回設定機能のモジュール設計書を作成する。
