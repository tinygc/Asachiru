package com.tinygc.asachiru.presentation.main.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import java.util.Calendar

/**
 * 時間帯に応じたグラデーション背景を表示するカスタムビュー
 *
 * 時間帯に応じて背景色が自動的に変化します：
 * - 朝（6:00～11:59）: 爽やかで柔らかいパステルカラー
 * - 昼（12:00～16:59）: より明るく開放的なパステルカラー
 * - 夕方（17:00～18:59）: 劇的でドラマチックなオレンジ系・夕焼け色
 * - 夜（19:00～5:59）: 深みのある神秘的なダーク系
 *
 * Material Design 3準拠の洗練された4色グラデーション。
 * 30秒周期で滑らかに変化（AccelerateDecelerateInterpolator使用）。
 */
class BackgroundGradientView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 朝（6:00～11:59）のパステルカラーグラデーション - より爽やかで柔らかく
    private val morningGradients = listOf(
        intArrayOf(
            Color.parseColor("#FFE5EC"), // 柔らかいピンク
            Color.parseColor("#FFC9DD"), // ローズピンク
            Color.parseColor("#C7CEEA"), // ラベンダー
            Color.parseColor("#B7E4F9")  // スカイブルー
        ),
        intArrayOf(
            Color.parseColor("#E0F7FA"), // アイスブルー
            Color.parseColor("#B2EBF2"), // アクアマリン
            Color.parseColor("#E1F5C4"), // ライムグリーン
            Color.parseColor("#FFFDE7")  // クリームイエロー
        ),
        intArrayOf(
            Color.parseColor("#FFF9C4"), // レモンクリーム
            Color.parseColor("#FFE082"), // サンライトイエロー
            Color.parseColor("#FFCCBC"), // ピーチ
            Color.parseColor("#F8BBD0")  // ブロッサムピンク
        ),
        intArrayOf(
            Color.parseColor("#E1BEE7"), // オーキッド
            Color.parseColor("#F3E5F5"), // ライラック
            Color.parseColor("#FCE4EC"), // シェルピンク
            Color.parseColor("#FFF0F5")  // ラベンダーブラッシュ
        )
    )

    // 昼（12:00～16:59）の明るいパステルカラーグラデーション - より明るく開放的に
    private val afternoonGradients = listOf(
        intArrayOf(
            Color.parseColor("#FFFEF0"), // アイボリー
            Color.parseColor("#FFF9E6"), // バニラ
            Color.parseColor("#FFE9A0"), // ゴールデンクリーム
            Color.parseColor("#FFD07B")  // サンセットゴールド
        ),
        intArrayOf(
            Color.parseColor("#E3F2FD"), // アリスブルー
            Color.parseColor("#BBDEFB"), // ベビーブルー
            Color.parseColor("#90CAF9"), // コーンフラワー
            Color.parseColor("#64B5F6")  // スカイブルー
        ),
        intArrayOf(
            Color.parseColor("#F1F8E9"), // ミントホワイト
            Color.parseColor("#DCEDC8"), // セラドン
            Color.parseColor("#C5E1A5"), // ライトグリーン
            Color.parseColor("#AED581")  // ピスタチオ
        ),
        intArrayOf(
            Color.parseColor("#FFF3E0"), // パパイヤホイップ
            Color.parseColor("#FFE0B2"), // ピーチクリーム
            Color.parseColor("#FFCCBC"), // アプリコット
            Color.parseColor("#FFAB91")  // コーラルピンク
        )
    )

    // 夕方（17:00～18:59）のオレンジ系・夕焼け色グラデーション - より劇的でドラマチックに
    private val eveningGradients = listOf(
        intArrayOf(
            Color.parseColor("#FF6F61"), // 燃えるコーラル
            Color.parseColor("#FF8A65"), // マンゴーオレンジ
            Color.parseColor("#FFAB91"), // サーモンピンク
            Color.parseColor("#FFD6C9")  // ピーチクリーム
        ),
        intArrayOf(
            Color.parseColor("#FF7043"), // ディープオレンジ
            Color.parseColor("#FF8A50"), // タンジェリン
            Color.parseColor("#FFAB40"), // アンバー
            Color.parseColor("#FFC947")  // ゴールデンサンセット
        ),
        intArrayOf(
            Color.parseColor("#EF5350"), // トマトレッド
            Color.parseColor("#FF6E40"), // フレームオレンジ
            Color.parseColor("#FF9E80"), // コーラルオレンジ
            Color.parseColor("#FFB2A6")  // ローズゴールド
        ),
        intArrayOf(
            Color.parseColor("#EC407A"), // ホットピンク
            Color.parseColor("#F06292"), // ローズピンク
            Color.parseColor("#FF80AB"), // ライトローズ
            Color.parseColor("#FFB3C6")  // ブラッシュピンク
        )
    )

    // 夜（19:00～5:59）のダーク系・落ち着いた色グラデーション - より深みのある神秘的な色
    private val nightGradients = listOf(
        intArrayOf(
            Color.parseColor("#1A237E"), // ディープインディゴ
            Color.parseColor("#283593"), // インディゴブルー
            Color.parseColor("#3949AB"), // ロイヤルブルー
            Color.parseColor("#5C6BC0")  // パープリッシュブルー
        ),
        intArrayOf(
            Color.parseColor("#0D47A1"), // ダークブルー
            Color.parseColor("#1565C0"), // オーシャンブルー
            Color.parseColor("#1976D2"), // ブルー
            Color.parseColor("#1E88E5")  // ライトブルー
        ),
        intArrayOf(
            Color.parseColor("#4A148C"), // ディープパープル
            Color.parseColor("#6A1B9A"), // ダークバイオレット
            Color.parseColor("#7B1FA2"), // パープル
            Color.parseColor("#8E24AA")  // ミディアムパープル
        ),
        intArrayOf(
            Color.parseColor("#1B5E20"), // ダークグリーン
            Color.parseColor("#2E7D32"), // フォレストグリーン
            Color.parseColor("#388E3C"), // グリーン
            Color.parseColor("#43A047")  // ライムグリーン
        )
    )

    // 現在の時間帯に応じたグラデーションを取得
    private val gradientColors: List<IntArray>
        get() = when (getCurrentTimeOfDay()) {
            TimeOfDay.MORNING -> morningGradients
            TimeOfDay.AFTERNOON -> afternoonGradients
            TimeOfDay.EVENING -> eveningGradients
            TimeOfDay.NIGHT -> nightGradients
        }

    private var currentGradientIndex = 0
    private var nextGradientIndex = 1
    private var animationProgress = 0f
    private var currentTimeOfDay = getCurrentTimeOfDay()

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 30000 // 30秒 - よりゆったりとした遷移
        interpolator = AccelerateDecelerateInterpolator() // 滑らかな加速・減速
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { animation ->
            animationProgress = animation.animatedValue as Float

            // 時間帯の変化をチェック
            val newTimeOfDay = getCurrentTimeOfDay()
            if (newTimeOfDay != currentTimeOfDay) {
                currentTimeOfDay = newTimeOfDay
                // 時間帯が変わったらグラデーションをリセット
                currentGradientIndex = 0
                nextGradientIndex = 1
            }

            invalidate() // 再描画
        }
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationRepeat(animation: Animator) {
                // アニメーションがリピートする際に次のグラデーションへ切り替え
                currentGradientIndex = nextGradientIndex
                nextGradientIndex = (nextGradientIndex + 1) % gradientColors.size
            }
        })
    }

    /**
     * 時間帯を表すEnum
     */
    private enum class TimeOfDay {
        MORNING,    // 朝（6:00～11:59）
        AFTERNOON,  // 昼（12:00～16:59）
        EVENING,    // 夕方（17:00～18:59）
        NIGHT       // 夜（19:00～5:59）
    }

    /**
     * 現在の時間帯を取得
     */
    private fun getCurrentTimeOfDay(): TimeOfDay {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        return when (hour) {
            in 6..11 -> TimeOfDay.MORNING
            in 12..16 -> TimeOfDay.AFTERNOON
            in 17..18 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 現在のグラデーションと次のグラデーションを補間
        val currentColors = gradientColors[currentGradientIndex]
        val nextColors = gradientColors[nextGradientIndex]

        val interpolatedColors = IntArray(currentColors.size) { i ->
            interpolateColor(currentColors[i], nextColors[i], animationProgress)
        }

        // グラデーションを作成
        val gradient = LinearGradient(
            0f, 0f,
            0f, height.toFloat(),
            interpolatedColors,
            null,
            Shader.TileMode.CLAMP
        )

        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    /**
     * 2つの色を補間
     */
    private fun interpolateColor(color1: Int, color2: Int, fraction: Float): Int {
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)

        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)

        val r = (r1 + (r2 - r1) * fraction).toInt()
        val g = (g1 + (g2 - g1) * fraction).toInt()
        val b = (b1 + (b2 - b1) * fraction).toInt()

        return Color.rgb(r, g, b)
    }
}
