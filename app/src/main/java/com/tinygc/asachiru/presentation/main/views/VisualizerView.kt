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
import kotlin.math.pow
import kotlin.math.hypot
import kotlin.math.log10

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
    private var previousEndBin = -1 // CAVA補正ロジック用: 前のバーの終了bin

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
                // captureSizeは1024に設定（512周波数bin = DC + 511成分）
                // getCaptureSizeRange()[1]だと大きすぎて処理が重くなる可能性
                captureSize = 1024
                // SCALING_MODE_NORMALIZED: ボリューム影響を受けず、音楽ビジュアライゼーションに最適
                // (デフォルトモードだが明示的に設定)
                scalingMode = Visualizer.SCALING_MODE_NORMALIZED
                
                android.util.Log.d("Visualizer", "Setup: captureSize=$captureSize scalingMode=$scalingMode samplingRate=${samplingRate}")
                
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
     * VisualizerのFFTフォーマット（ドキュメント準拠）:
     * - Index 0: DC成分の実部 (Rf0)
     * - Index 1: Nyquist周波数の実部 (Rf(n/2))
     * - Index 2,3: 1番目の周波数の実部・虚部 (Rf1, If1)
     * - Index 4,5: 2番目の周波数の実部・虚部 (Rf2, If2)
     * ...
     * 周波数k (k=1..n/2-1) は fft[k*2] (実部) と fft[k*2+1] (虚部) に格納
     */
    private fun updateBarHeightsFromFft(fft: ByteArray) {
        if (fft.isEmpty()) return
        lastFftUpdateTime = System.currentTimeMillis()
        
        val n = fft.size
        
        // Debug: 最初の更新時と定期的にログ出力
        if (fftUpdateCount == 0L) {
            val sample = fft.take(20).joinToString(",") { (it.toInt() and 0xFF).toString() }
            android.util.Log.d("Visualizer", "First FFT! n=$n sample=[$sample...]")
        } else if (fftUpdateCount % 100 == 0L) {
            val maxVal = fft.maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 0
            val avgVal = fft.map { kotlin.math.abs(it.toInt()) }.average()
            android.util.Log.d("Visualizer", "FFT #$fftUpdateCount max=$maxVal avg=$avgVal")
        }
        fftUpdateCount++
        
        // 周波数binの数 = n/2 + 1 (DC + 1..(n/2-1) + Nyquist)
        val numFrequencies = n / 2 + 1
        
        // magnitude配列を計算（ドキュメントの例に準拠）
        val magnitudes = FloatArray(numFrequencies)
        magnitudes[0] = kotlin.math.abs(fft[0].toInt()).toFloat()  // DC成分
        if (n > 1) {
            magnitudes[n / 2] = kotlin.math.abs(fft[1].toInt()).toFloat()  // Nyquist
        }
        
        // k=1 .. n/2-1 の周波数成分を計算
        for (k in 1 until n / 2) {
            val i = k * 2
            if (i + 1 < fft.size) {
                val real = fft[i].toInt().toDouble()
                val imag = fft[i + 1].toInt().toDouble()
                // magnitude = sqrt(real^2 + imag^2)
                magnitudes[k] = hypot(real, imag).toFloat()
            }
        }
        
        // 50本のバーに周波数成分をマッピング
        // CAVAの対数分布アルゴリズムを参考に実装
        // 周波数定数を計算 (CAVA cavacore.c:183-201をベースに)
        val lowerCutOff = 50.0   // 50Hz
        val upperCutOff = 5000.0 // 5kHz (ローファイ音源の倍音範囲に最適化)
        val maxBin = (n / 2 - 1).toDouble()
        
        // frequency_constant = log10(lower/upper) / (1/(bars+1) - 1)
        val frequencyConstant = log10(lowerCutOff / upperCutOff) / 
                                (1.0 / (BAR_COUNT + 1) - 1.0)
        
        // 各バーのカットオフ周波数を計算
        val cutOffFrequencies = DoubleArray(BAR_COUNT + 1)
        for (bar in 0..BAR_COUNT) {
            val barCoeff = frequencyConstant * -1.0 + 
                          (bar + 1.0) / (BAR_COUNT + 1.0) * frequencyConstant
            cutOffFrequencies[bar] = upperCutOff * 10.0.pow(barCoeff)
        }
        
        // サンプリングレート（Android Visualizerのデフォルト: 通常44100Hz）
        val samplingRate = 44100.0 
        val nyquistFreq = samplingRate / 2.0
        
        // 各バーごとにリセット
        previousEndBin = -1
        
        // 各バーごとにbinの範囲を計算して平均magnitude取得
        for (bar in 0 until BAR_COUNT) {
            // 周波数→bin変換: bin = frequency / nyquist * maxBin
            val startFreq = cutOffFrequencies[bar]
            val endFreq = cutOffFrequencies[bar + 1]
            
            var startBin = ((startFreq / nyquistFreq) * maxBin).toInt().coerceAtLeast(0)
            var endBin = ((endFreq / nyquistFreq) * maxBin).toInt().coerceAtMost(maxBin.toInt())
            
            // バンドが重なったら次のバンドにシフト (CAVA cavacore.c:221-243の補正ロジック)
            if (bar > 0 && startBin <= previousEndBin) {
                startBin = previousEndBin + 1
            }
            if (startBin > endBin) {
                endBin = startBin
            }
            
            // この範囲の平均magnitude
            var sum = 0f
            var count = 0
            for (bin in startBin..endBin) {
                if (bin < magnitudes.size) {
                    sum += magnitudes[bin]
                    count++
                }
            }
            val avg = if (count > 0) sum / count else 0f
            
            // デバッグ: 最初の更新時にバーごとの割り当てを確認
            if (fftUpdateCount == 1L && (bar % 10 == 0 || bar == BAR_COUNT - 1)) {
                android.util.Log.d("Visualizer", "bar[$bar] freq[${startFreq.toInt()}-${endFreq.toInt()}Hz] bins[$startBin-$endBin] count=$count avg=$avg")
            }
            
            // 次のループ用に記録
            previousEndBin = endBin
            
            // 正規化: magnitudeの範囲は実測で調整
            // byte値(-128～127)の2値のhypotだから、最大は sqrt(127^2 + 127^2) ≈ 180
            val normalized = (avg / 180f).coerceIn(0f, 1f)
            val target = 0.15f + normalized * 0.7f
            fftTempMagnitudes[bar] = target
        }
        
        // 平滑化（音楽的な応答性）
        for (i in 0 until BAR_COUNT) {
            val smoothFactor = 0.5f // 50%ずつミックス
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
