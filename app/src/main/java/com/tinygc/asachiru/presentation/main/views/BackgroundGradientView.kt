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
import android.view.animation.LinearInterpolator
import java.util.Calendar

/**
 * 時間帯に応じたグラデーション背景を表示するカスタムビュー
 *
 * 時間帯に応じて背景色が自動的に変化します：
 * - 朝（6:00～11:59）: パステルカラー
 * - 昼（12:00～16:59）: 明るいパステルカラー
 * - 夕方（17:00～18:59）: オレンジ系・夕焼け色
 * - 夜（19:00～5:59）: ダーク系・落ち着いた色
 *
 * 20秒周期でグラデーションの色がゆっくりと変化します。
 */
class BackgroundGradientView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 朝（6:00～11:59）のパステルカラーグラデーション
    private val morningGradients = listOf(
        intArrayOf(
            Color.parseColor("#FFB5D8"), // パステルピンク
            Color.parseColor("#D5B5E8"), // パステルパープル
            Color.parseColor("#B5C8F0")  // パステルブルー
        ),
        intArrayOf(
            Color.parseColor("#B5D8FF"), // パステルブルー
            Color.parseColor("#B5F0E8"), // パステルシアン
            Color.parseColor("#C8F0B5")  // パステルグリーン
        ),
        intArrayOf(
            Color.parseColor("#FFF0B5"), // パステルイエロー
            Color.parseColor("#FFD8B5"), // パステルオレンジ
            Color.parseColor("#FFB5C8")  // パステルローズ
        ),
        intArrayOf(
            Color.parseColor("#E8B5F0"), // パステルパープル
            Color.parseColor("#F0B5D8"), // パステルピンク
            Color.parseColor("#FFB5D8")  // パステルピンク
        )
    )

    // 昼（12:00～16:59）の明るいパステルカラーグラデーション
    private val afternoonGradients = listOf(
        intArrayOf(
            Color.parseColor("#FFFACD"), // レモンシフォン
            Color.parseColor("#FFE4B5"), // モカシン
            Color.parseColor("#FFDAB9")  // ピーチパフ
        ),
        intArrayOf(
            Color.parseColor("#E0FFFF"), // ライトシアン
            Color.parseColor("#B0E0E6"), // パウダーブルー
            Color.parseColor("#ADD8E6")  // ライトブルー
        ),
        intArrayOf(
            Color.parseColor("#F0FFF0"), // ハニーデュー
            Color.parseColor("#F5FFFA"), // ミントクリーム
            Color.parseColor("#E0FFE0")  // ライトグリーン
        ),
        intArrayOf(
            Color.parseColor("#FFF0F5"), // ラベンダーブラッシュ
            Color.parseColor("#FFE4E1"), // ミスティローズ
            Color.parseColor("#FFB6C1")  // ライトピンク
        )
    )

    // 夕方（17:00～18:59）のオレンジ系・夕焼け色グラデーション
    private val eveningGradients = listOf(
        intArrayOf(
            Color.parseColor("#FF6B35"), // オレンジレッド
            Color.parseColor("#FF8C42"), // オレンジ
            Color.parseColor("#FFA556")  // ライトオレンジ
        ),
        intArrayOf(
            Color.parseColor("#FF7F50"), // コーラル
            Color.parseColor("#FF6347"), // トマト
            Color.parseColor("#FF4500")  // オレンジレッド
        ),
        intArrayOf(
            Color.parseColor("#FFB347"), // パステルオレンジ
            Color.parseColor("#FFCC99"), // ピーチ
            Color.parseColor("#FFE5B4")  // ピーチ
        ),
        intArrayOf(
            Color.parseColor("#E9967A"), // ダークサーモン
            Color.parseColor("#FA8072"), // サーモン
            Color.parseColor("#FFA07A")  // ライトサーモン
        )
    )

    // 夜（19:00～5:59）のダーク系・落ち着いた色グラデーション
    private val nightGradients = listOf(
        intArrayOf(
            Color.parseColor("#2C3E50"), // ミッドナイトブルー
            Color.parseColor("#34495E"), // ウェットアスファルト
            Color.parseColor("#1C2833")  // ダークブルー
        ),
        intArrayOf(
            Color.parseColor("#191970"), // ミッドナイトブルー
            Color.parseColor("#1C1C3C"), // ダークネイビー
            Color.parseColor("#0F1C2E")  // ディープブルー
        ),
        intArrayOf(
            Color.parseColor("#2F4F4F"), // ダークスレートグレー
            Color.parseColor("#363B4E"), // ダークグレー
            Color.parseColor("#2E3338")  // チャコール
        ),
        intArrayOf(
            Color.parseColor("#483D8B"), // ダークスレートブルー
            Color.parseColor("#4B0082"), // インディゴ
            Color.parseColor("#301934")  // ダークパープル
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
        duration = 20000 // 20秒
        interpolator = LinearInterpolator()
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
