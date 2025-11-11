package com.tinygc.asachiru.presentation.main.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

/**
 * パステルカラーの円形パーティクルが画面内をふわふわ浮遊するカスタムビュー
 *
 * 約20個のパーティクルが、10-20秒周期でランダムに移動します。
 */
class ParticleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // パステルレインボーカラー（8色）
    private val pastelColors = listOf(
        Color.parseColor("#FF6B9D"), // パステルピンク
        Color.parseColor("#FFA07A"), // パステルオレンジ
        Color.parseColor("#FFD93D"), // パステルイエロー
        Color.parseColor("#6BCF7F"), // パステルグリーン
        Color.parseColor("#4ECDC4"), // パステルシアン
        Color.parseColor("#95B8D1"), // パステルブルー
        Color.parseColor("#B8A9C9"), // パステルパープル
        Color.parseColor("#F38FB1")  // パステルローズ
    )

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        // 20個のパーティクルを初期化
        repeat(20) {
            particles.add(Particle())
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // サイズが変わったらパーティクルの位置を再初期化
        particles.forEach { it.init(w, h) }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        particles.forEach { it.startAnimation() }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        particles.forEach { it.stopAnimation() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        particles.forEach { particle ->
            paint.color = particle.color
            paint.alpha = (255 * 0.3f).toInt() // 30%の不透明度
            canvas.drawCircle(particle.x, particle.y, particle.radius, paint)
        }
    }

    /**
     * パーティクルクラス
     */
    private inner class Particle {
        var x = 0f
        var y = 0f
        var radius = 0f
        var color = 0

        private var startX = 0f
        private var startY = 0f
        private var endX = 0f
        private var endY = 0f

        private var animator: ValueAnimator? = null

        init {
            // ランダムな色を選択
            color = pastelColors.random()
            // ランダムなサイズ（10-40px）
            radius = Random.nextFloat() * 30f + 10f
        }

        fun init(width: Int, height: Int) {
            // ランダムな開始位置
            startX = Random.nextFloat() * width
            startY = Random.nextFloat() * height
            x = startX
            y = startY

            // ランダムな終了位置
            endX = Random.nextFloat() * width
            endY = Random.nextFloat() * height
        }

        fun startAnimation() {
            val duration = (Random.nextFloat() * 10000 + 10000).toLong() // 10-20秒

            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                this.duration = duration
                interpolator = LinearInterpolator()
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                addUpdateListener { animation ->
                    val progress = animation.animatedValue as Float

                    // 開始位置から終了位置へ補間
                    x = startX + (endX - startX) * progress
                    y = startY + (endY - startY) * progress

                    invalidate() // 再描画
                }
                start()
            }
        }

        fun stopAnimation() {
            animator?.cancel()
            animator = null
        }
    }
}
