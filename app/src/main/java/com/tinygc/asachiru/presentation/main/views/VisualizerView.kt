package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.audiofx.Visualizer
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.sin

/**
 * スペクトラムアナライザーを表示するカスタムビュー
 *
 * 音楽の波形データをリアルタイムで取得し、50本のバーで可視化します。
 * パステルレインボーカラーで表示され、25%の不透明度で背景として機能します。
 *
 * パフォーマンス最適化:
 * - フレームレート30fps制限
 * - ハードウェアアクセラレーション有効化
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
        private const val MIN_FRAME_INTERVAL = 33L // 30fps (1000ms / 30 = 33ms)

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
    private var lastDrawTime = 0L
    private var fallbackPhase = 0.0
    private var useFallback = false
    private var fallbackRetryCount = 0
    private val maxRetries = 3
    private var lastAudioSessionId: Int = 0
    private var useFft: Boolean = true // FFTを利用して周波数解析するかどうか
    private val fftTempMagnitudes = FloatArray(BAR_COUNT)
    private var lastFftUpdateTime = 0L
    private var fftUpdateCount = 0L

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        alpha = (255 * 0.25f).toInt() // 25%不透明度
    }
    
    private val debugPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        textSize = 24f
    }

    init {
        // ハードウェアアクセラレーションを有効化
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    /**
     * ビジュアライザーを開始
     * @param audioSessionId オーディオセッションID
     */
    fun startVisualizer(audioSessionId: Int) {
        stopVisualizer()
        useFallback = false
        fallbackRetryCount = 0
        lastAudioSessionId = audioSessionId
        try {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            waveform?.let {
                                if (!useFft) {
                                    updateBarHeights(it)
                                    postInvalidate()
                                }
                            }
                        }
                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (useFft) {
                                fft?.let {
                                    updateBarHeightsFromFft(it)
                                    postInvalidate()
                                }
                            }
                        }
                    },
                    Visualizer.getMaxCaptureRate(),
                    true,  // waveform
                    true   // fft
                )
                enabled = true
            }
            android.util.Log.d("Visualizer", "Visualizer初期化成功 audioSessionId=$audioSessionId rate=${Visualizer.getMaxCaptureRate()}")
        } catch (e: Exception) {
            visualizer = null
            useFallback = true
            android.util.Log.e("Visualizer", "初期化失敗 fallback有効: ${e.message}")
            scheduleRetryIfPossible()
        }
    }

    /**
     * 波形データからバーの高さを更新
     */
    private fun updateBarHeights(waveform: ByteArray) {
        val step = waveform.size / BAR_COUNT
        for (i in 0 until BAR_COUNT) {
            val index = i * step
            if (index < waveform.size) {
                val value = abs(waveform[index].toInt())
                val target = 0.15f + (value / 128f) * 0.7f
                barHeights[i] = barHeights[i] * 0.6f + target * 0.4f // 平滑化
            }
        }
    }

    /**
     * FFTデータ処理
     * VisualizerのFFTフォーマット: 先頭がDC成分 (実部), 2byte目がNyquist? 以降は r,i ペア
     * 対数周波数スケール（低音域を圧縮、高音域を拡張）でバーへマッピング
     */
    private fun updateBarHeightsFromFft(fft: ByteArray) {
        if (fft.isEmpty()) return
        lastFftUpdateTime = System.currentTimeMillis()
        
        // Debug: Log first FFT update and periodic updates
        if (fftUpdateCount == 0L) {
            val sample = fft.take(20).joinToString(",") { it.toString() }
            android.util.Log.d("Visualizer", "First FFT data received! fft.size=${fft.size} sample=[$sample...]")
        } else if (fftUpdateCount % 100 == 0L) {
            // 最初の数バイトの値を確認（全部0ならダミーデータの可能性）
            val hasData = fft.take(20).any { it != 0.toByte() }
            // 振幅の最大値をチェック（音楽が鳴ってれば大きな値があるはず）
            val maxMagnitude = fft.map { kotlin.math.abs(it.toInt()) }.maxOrNull() ?: 0
            android.util.Log.d("Visualizer", "FFT update #$fftUpdateCount hasNonZeroData=$hasData maxMagnitude=$maxMagnitude")
        }
        fftUpdateCount++
        
        val pairCount = fft.size / 2
        // DC成分(bin 0)とNyquist(bin 1)をスキップし、bin 2以降を使用
        val usableBins = pairCount - 2
        if (usableBins <= 0) return
        
        for (bar in 0 until BAR_COUNT) {
            // 対数スケールマッピング: 低音域は少ないbin、高音域は多いbinを割り当て
            val logStart = kotlin.math.ln((bar.toFloat() / BAR_COUNT * usableBins + 1).toDouble())
            val logEnd = kotlin.math.ln(((bar + 1).toFloat() / BAR_COUNT * usableBins + 1).toDouble())
            val startBin = (kotlin.math.exp(logStart) - 1).toInt() + 2
            val endBin = (kotlin.math.exp(logEnd) - 1).toInt().coerceAtMost(pairCount - 1) + 2
            
            var sum = 0f
            var count = 0
            for (bin in startBin..endBin) {
                if (bin * 2 + 1 < fft.size) {
                    val real = fft[bin * 2].toInt()
                    val imag = fft[bin * 2 + 1].toInt()
                    // 符号付き変換
                    val r = if (real > 127) real - 256 else real
                    val i = if (imag > 127) imag - 256 else imag
                    val mag = kotlin.math.sqrt((r * r + i * i).toDouble()).toFloat()
                    sum += mag
                    count++
                }
            }
            val avg = if (count > 0) sum / count else 0f
            
            // dBスケール（より広いダイナミックレンジ）
            val db = if (avg > 1f) (20 * kotlin.math.log10(avg.toDouble())).toFloat() else -80f
            // 正規化: -80dB..40dB を 0.15..0.85（音楽的な範囲）
            val norm = ((db + 80f) / 120f).coerceIn(0f, 1f)
            val target = 0.15f + norm * 0.7f
            fftTempMagnitudes[bar] = target
        }
        
        // 平滑化（高音域はより反応早く、低音域はゆっくり）
        for (i in 0 until BAR_COUNT) {
            val smoothFactor = 0.3f + (i.toFloat() / BAR_COUNT) * 0.4f // 0.3～0.7
            barHeights[i] = barHeights[i] * (1f - smoothFactor) + fftTempMagnitudes[i] * smoothFactor
        }
    }

    /**
     * ビジュアライザーを停止
     */
    fun stopVisualizer() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (e: Exception) {
            // 既に解放されている場合は無視
        }
        visualizer = null
        useFallback = false
        fallbackRetryCount = 0
    }

    override fun onDraw(canvas: Canvas) {
        // フレームレート制限（30fps）
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDrawTime < MIN_FRAME_INTERVAL) {
            return // フレームスキップ
        }
        lastDrawTime = currentTime

        super.onDraw(canvas)

        val startX = (width - (BAR_COUNT * (BAR_WIDTH + BAR_SPACING))) / 2

        // フォールバック: 規則的なゆらぎアニメ（再生中っぽい演出）
        if (visualizer == null && useFallback) {
            fallbackPhase += 0.08
            val base = (sin(fallbackPhase) + 1.0) / 2.0 // 0..1
            for (i in 0 until BAR_COUNT) {
                val localPhase = fallbackPhase + i * 0.15
                val wave = (sin(localPhase) + 1.0) / 2.0
                barHeights[i] = (0.15 + (base * wave) * 0.7).toFloat()
            }
        }

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
        
        // デバッグ表示: Fallback中か実データか
        if (useFallback) {
            canvas.drawText("FALLBACK", 20f, 40f, debugPaint)
        } else if (lastFftUpdateTime > 0) {
            val elapsed = System.currentTimeMillis() - lastFftUpdateTime
            if (elapsed < 1000) {
                debugPaint.color = Color.GREEN
                canvas.drawText("FFT ACTIVE", 20f, 40f, debugPaint)
                debugPaint.color = Color.RED
            }
        }
    }    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopVisualizer()
    }

    fun isUsingFallback(): Boolean = useFallback

    private fun scheduleRetryIfPossible() {
        if (fallbackRetryCount >= maxRetries || lastAudioSessionId == 0) return
        fallbackRetryCount++
        postDelayed({
            if (visualizer == null && useFallback && lastAudioSessionId != 0) {
                android.util.Log.d("Visualizer", "Retry $fallbackRetryCount/$maxRetries audioSessionId=$lastAudioSessionId")
                try {
                    visualizer = Visualizer(lastAudioSessionId).apply {
                        captureSize = Visualizer.getCaptureSizeRange()[1]
                        setDataCaptureListener(
                            object : Visualizer.OnDataCaptureListener {
                                override fun onWaveFormDataCapture(
                                    visualizer: Visualizer?,
                                    waveform: ByteArray?,
                                    samplingRate: Int
                                ) {
                                    if (!useFft) {
                                        waveform?.let {
                                            updateBarHeights(it)
                                            postInvalidate()
                                        }
                                    }
                                }
                                override fun onFftDataCapture(
                                    visualizer: Visualizer?,
                                    fft: ByteArray?,
                                    samplingRate: Int
                                ) {
                                    if (useFft) {
                                        fft?.let {
                                            updateBarHeightsFromFft(it)
                                            postInvalidate()
                                        }
                                    }
                                }
                            },
                            Visualizer.getMaxCaptureRate() / 2,
                            true,
                            useFft
                        )
                        enabled = true
                    }
                    useFallback = false
                    android.util.Log.d("Visualizer", "Retry成功 audioSessionId=$lastAudioSessionId")
                } catch (e: Exception) {
                    android.util.Log.e("Visualizer", "Retry失敗: ${e.message}")
                    useFallback = true
                    scheduleRetryIfPossible()
                }
            }
        }, 500)
    }

    /**
     * FFTモードを切替
     */
    fun setUseFft(enabled: Boolean) {
        useFft = enabled
    }
}
