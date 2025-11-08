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

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        alpha = (255 * 0.25f).toInt() // 25%不透明度
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
                            // 波形データからバーの高さを計算
                            waveform?.let {
                                updateBarHeights(it)
                                postInvalidate()
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
        } catch (e: Exception) {
            // Visualizerの初期化に失敗した場合（権限がない、既に使用中など）
            visualizer = null
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
                // 0-128の範囲を0.15-0.85にマッピング
                barHeights[i] = 0.15f + (value / 128f) * 0.7f
            }
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
