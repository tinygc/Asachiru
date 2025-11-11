package com.tinygc.asachiru.presentation.main.views

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

/**
 * パステルカラーのグラデーション背景を表示するカスタムビュー
 *
 * 20秒周期でグラデーションの色がゆっくりと変化します。
 */
class BackgroundGradientView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // パステルカラーのグラデーション定義（4つのグラデーションを循環）
    private val gradientColors = listOf(
        // グラデーション1: ピンク → パープル
        intArrayOf(
            Color.parseColor("#FFB5D8"), // パステルピンク
            Color.parseColor("#D5B5E8"), // パステルパープル
            Color.parseColor("#B5C8F0")  // パステルブルー
        ),
        // グラデーション2: ブルー → シアン
        intArrayOf(
            Color.parseColor("#B5D8FF"), // パステルブルー
            Color.parseColor("#B5F0E8"), // パステルシアン
            Color.parseColor("#C8F0B5")  // パステルグリーン
        ),
        // グラデーション3: イエロー → オレンジ
        intArrayOf(
            Color.parseColor("#FFF0B5"), // パステルイエロー
            Color.parseColor("#FFD8B5"), // パステルオレンジ
            Color.parseColor("#FFB5C8")  // パステルローズ
        ),
        // グラデーション4: パープル → ピンク
        intArrayOf(
            Color.parseColor("#E8B5F0"), // パステルパープル
            Color.parseColor("#F0B5D8"), // パステルピンク
            Color.parseColor("#FFB5D8")  // パステルピンク
        )
    )

    private var currentGradientIndex = 0
    private var nextGradientIndex = 1
    private var animationProgress = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 20000 // 20秒
        interpolator = LinearInterpolator()
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { animation ->
            animationProgress = animation.animatedValue as Float

            // アニメーションが完了したら次のグラデーションへ
            if (animationProgress >= 0.99f) {
                currentGradientIndex = nextGradientIndex
                nextGradientIndex = (nextGradientIndex + 1) % gradientColors.size
            }

            invalidate() // 再描画
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
