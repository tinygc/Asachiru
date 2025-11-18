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
 * - 早朝（5:00～6:59）: 深い青紫から明るい青へのグラデーション
 * - 朝～昼（7:00～16:59）: 柔らかい青系から明るいパステルカラー
 * - 夕方（17:00～18:59）: オレンジからピンクへの夕焼け色
 * - 夜（19:00～4:59）: 深い紺色から紫へのダークグラデーション
 *
 * 白文字の視認性を確保するため、暗めのトーンを基調としています。
 * 30秒周期で滑らかに変化（AccelerateDecelerateInterpolator使用）。
 */
class BackgroundGradientView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 早朝（5:00～6:59）のグラデーション - 深い青紫から明るい青へ
    private val earlyMorningGradients = listOf(
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
            Color.parseColor("#2196F3")  // ライトブルー
        ),
        intArrayOf(
            Color.parseColor("#1B5E20"), // ダークグリーン
            Color.parseColor("#2E7D32"), // フォレストグリーン
            Color.parseColor("#388E3C"), // グリーン
            Color.parseColor("#4CAF50")  // ライトグリーン
        ),
        intArrayOf(
            Color.parseColor("#4A148C"), // ディープパープル
            Color.parseColor("#6A1B9A"), // ダークバイオレット
            Color.parseColor("#7B1FA2"), // パープル
            Color.parseColor("#9C27B0")  // ミディアムパープル
        )
    )

    // 朝～昼（7:00～16:59）のグラデーション - 柔らかい青系から明るいパステル
    private val dayGradients = listOf(
        intArrayOf(
            Color.parseColor("#37474F"), // ブルーグレー
            Color.parseColor("#546E7A"), // スレートブルー
            Color.parseColor("#78909C"), // ライトブルーグレー
            Color.parseColor("#90A4AE")  // シルバーブルー
        ),
        intArrayOf(
            Color.parseColor("#455A64"), // ダークブルーグレー
            Color.parseColor("#607D8B"), // ブルーグレー
            Color.parseColor("#78909C"), // ライトブルーグレー
            Color.parseColor("#B0BEC5")  // ペールブルーグレー
        ),
        intArrayOf(
            Color.parseColor("#1976D2"), // ブルー
            Color.parseColor("#2196F3"), // ライトブルー
            Color.parseColor("#64B5F6"), // スカイブルー
            Color.parseColor("#90CAF9")  // ペールスカイブルー
        ),
        intArrayOf(
            Color.parseColor("#0288D1"), // ディープスカイブルー
            Color.parseColor("#03A9F4"), // ライトブルー
            Color.parseColor("#4FC3F7"), // アクアブルー
            Color.parseColor("#81D4FA")  // ライトアクアブルー
        )
    )

    // 夕方（17:00～18:59）のグラデーション - オレンジからピンクへの夕焼け色
    private val eveningGradients = listOf(
        intArrayOf(
            Color.parseColor("#BF360C"), // ディープオレンジ
            Color.parseColor("#D84315"), // ダークオレンジ
            Color.parseColor("#E64A19"), // オレンジレッド
            Color.parseColor("#FF5722")  // ディープオレンジ
        ),
        intArrayOf(
            Color.parseColor("#E65100"), // ダークオレンジ
            Color.parseColor("#EF6C00"), // オレンジ
            Color.parseColor("#F57C00"), // ディープオレンジ
            Color.parseColor("#FB8C00")  // オレンジ
        ),
        intArrayOf(
            Color.parseColor("#C2185B"), // ディープピンク
            Color.parseColor("#D81B60"), // ピンク
            Color.parseColor("#E91E63"), // ホットピンク
            Color.parseColor("#F06292")  // ライトピンク
        ),
        intArrayOf(
            Color.parseColor("#AD1457"), // ディープマゼンタ
            Color.parseColor("#C2185B"), // マゼンタ
            Color.parseColor("#D81B60"), // ホットピンク
            Color.parseColor("#EC407A")  // ピンク
        )
    )

    // 夜（19:00～4:59）のグラデーション - 深い紺色から紫へ
    private val nightGradients = listOf(
        intArrayOf(
            Color.parseColor("#0D47A1"), // ダークブルー
            Color.parseColor("#1565C0"), // オーシャンブルー
            Color.parseColor("#1976D2"), // ブルー
            Color.parseColor("#1E88E5")  // ライトブルー
        ),
        intArrayOf(
            Color.parseColor("#1A237E"), // ディープインディゴ
            Color.parseColor("#283593"), // インディゴブルー
            Color.parseColor("#3949AB"), // ロイヤルブルー
            Color.parseColor("#5C6BC0")  // パープリッシュブルー
        ),
        intArrayOf(
            Color.parseColor("#4A148C"), // ディープパープル
            Color.parseColor("#6A1B9A"), // ダークバイオレット
            Color.parseColor("#7B1FA2"), // パープル
            Color.parseColor("#8E24AA")  // ミディアムパープル
        ),
        intArrayOf(
            Color.parseColor("#311B92"), // ディープパープル
            Color.parseColor("#4527A0"), // ダークバイオレット
            Color.parseColor("#512DA8"), // パープル
            Color.parseColor("#5E35B1")  // ミディアムパープル
        )
    )

    // 現在の時間帯に応じたグラデーションを取得
    private val gradientColors: List<IntArray>
        get() = when (getCurrentTimeOfDay()) {
            TimeOfDay.EARLY_MORNING -> earlyMorningGradients
            TimeOfDay.DAY -> dayGradients
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
        EARLY_MORNING,  // 早朝（5:00～6:59）
        DAY,            // 朝～昼（7:00～16:59）
        EVENING,        // 夕方（17:00～18:59）
        NIGHT           // 夜（19:00～4:59）
    }

    /**
     * 現在の時間帯を取得
     */
    private fun getCurrentTimeOfDay(): TimeOfDay {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        return when (hour) {
            in 5..6 -> TimeOfDay.EARLY_MORNING
            in 7..16 -> TimeOfDay.DAY
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
