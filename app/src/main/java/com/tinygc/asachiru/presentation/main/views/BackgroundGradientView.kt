package com.tinygc.asachiru.presentation.main.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import java.util.Calendar
import kotlin.math.min

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
 *
 * モダンでチルな雰囲気を演出するため、以下の効果を追加：
 * - ビネット効果: 四隅を暗くして中心に視線を集める
 * - 発光エフェクト: 画面隅から柔らかい光が漏れる
 * - 波紋効果: 中心から波紋が広がる禅的なアニメーション
 */
class BackgroundGradientView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f // 控えめに調整
    }

    // 波紋アニメーション用
    private var rippleProgress = 0f
    private val rippleCount = 3 // 3つの波紋

    // 各波紋のランダムなオフセット（0.0～1.0）
    private val rippleOffsets = FloatArray(rippleCount) { kotlin.random.Random.nextFloat() }

    // 各波紋のランダムな周期（12～18秒）
    private val rippleDurations = FloatArray(rippleCount) { 12000f + kotlin.random.Random.nextFloat() * 6000f }

    // 発光エフェクト用
    private var glowAnimationTime = 0f

    // ===========================================
    // Asachiru専用 ローファイ/チルアウト風パステルグラデーション
    // ===========================================

    // 早朝（5:00～6:59）のグラデーション - ラベンダーからピンクへの朝焼け
    private val earlyMorningGradientsAsachiru = listOf(
        intArrayOf(
            Color.parseColor("#E0D4FF"), // ラベンダー
            Color.parseColor("#FFD4E9"), // ピンクラベンダー
            Color.parseColor("#FFE4D4"), // ピーチ
            Color.parseColor("#FFF4E4")  // クリーム
        ),
        intArrayOf(
            Color.parseColor("#C4B5FD"), // パステルラベンダー
            Color.parseColor("#FFB5D4"), // パステルピンク
            Color.parseColor("#FFD4C4"), // サーモンピンク
            Color.parseColor("#FFECD4")  // パステルピーチ
        ),
        intArrayOf(
            Color.parseColor("#D4C4FF"), // ライラック
            Color.parseColor("#FFC4E9"), // ロゼ
            Color.parseColor("#FFD4B5"), // アプリコット
            Color.parseColor("#FFE4C4")  // ペールオレンジ
        )
    )

    // 朝～昼（7:00～16:59）のグラデーション - スカイブルーからミントへ
    private val dayGradientsAsachiru = listOf(
        intArrayOf(
            Color.parseColor("#B5E5FF"), // パステルスカイブルー
            Color.parseColor("#D4F0FF"), // ライトスカイブルー
            Color.parseColor("#E4FAFF"), // ペールスカイブルー
            Color.parseColor("#F4FFFF")  // ほぼ白
        ),
        intArrayOf(
            Color.parseColor("#B5FFDA"), // パステルミント
            Color.parseColor("#D4FFE9"), // ライトミント
            Color.parseColor("#E4FFF4"), // ペールミント
            Color.parseColor("#F4FFFA")  // ほぼ白
        ),
        intArrayOf(
            Color.parseColor("#B5F0FF"), // アクアブルー
            Color.parseColor("#D4FAFF"), // ライトアクア
            Color.parseColor("#E4FCFF"), // ペールアクア
            Color.parseColor("#F4FEFF")  // ほぼ白
        )
    )

    // 夕方（17:00～18:59）のグラデーション - コーラルからピーチへの夕焼け
    private val eveningGradientsAsachiru = listOf(
        intArrayOf(
            Color.parseColor("#FFB5A4"), // パステルコーラル
            Color.parseColor("#FFD4C4"), // サーモンピンク
            Color.parseColor("#FFE4D4"), // ピーチ
            Color.parseColor("#FFF4E4")  // クリーム
        ),
        intArrayOf(
            Color.parseColor("#FFC494"), // パステルオレンジ
            Color.parseColor("#FFD4A4"), // パステルピーチ
            Color.parseColor("#FFE4B5"), // ライトピーチ
            Color.parseColor("#FFF4C4")  // ライトクリーム
        ),
        intArrayOf(
            Color.parseColor("#FFB5C4"), // ピンクコーラル
            Color.parseColor("#FFD4D4"), // ライトピンク
            Color.parseColor("#FFE4E4"), // ペールピンク
            Color.parseColor("#FFF4F4")  // ほぼ白
        )
    )

    // 夜（19:00～4:59）のグラデーション - ラベンダーからディープパープルへ
    private val nightGradientsAsachiru = listOf(
        intArrayOf(
            Color.parseColor("#C4B5FD"), // パステルラベンダー
            Color.parseColor("#B5A4ED"), // パステルパープル
            Color.parseColor("#D4C4FD"), // ライトラベンダー
            Color.parseColor("#E0D4FF")  // ペールラベンダー
        ),
        intArrayOf(
            Color.parseColor("#B5A4ED"), // パステルパープル
            Color.parseColor("#C4B5FD"), // パステルラベンダー
            Color.parseColor("#D4C4FF"), // ライラック
            Color.parseColor("#E4D4FF")  // ペールライラック
        ),
        intArrayOf(
            Color.parseColor("#A4B5ED"), // パステルペリウィンクル
            Color.parseColor("#B5C4FD"), // ライトブルーパープル
            Color.parseColor("#C4D4FF"), // ペールペリウィンクル
            Color.parseColor("#D4E4FF")  // ペールブルー
        )
    )

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

    // 現在の時間帯に応じたグラデーションを取得（フレーバーによって分岐）
    private val gradientColors: List<IntArray>
        get() {
            val isAsachiru = com.tinygc.asachiru.BuildConfig.FLAVOR == "asachiru"
            return when (getCurrentTimeOfDay()) {
                TimeOfDay.EARLY_MORNING -> if (isAsachiru) earlyMorningGradientsAsachiru else earlyMorningGradients
                TimeOfDay.DAY -> if (isAsachiru) dayGradientsAsachiru else dayGradients
                TimeOfDay.EVENING -> if (isAsachiru) eveningGradientsAsachiru else eveningGradients
                TimeOfDay.NIGHT -> if (isAsachiru) nightGradientsAsachiru else nightGradients
            }
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

            // 発光アニメーション
            glowAnimationTime += 0.016f // 約60fps想定

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

        // 1. ベースグラデーション描画
        drawBaseGradient(canvas)

        // 2. 発光エフェクト
        drawGlowEffect(canvas)

        // 3. 波紋効果
        drawRippleEffect(canvas)

        // 4. ビネット効果（最後に描画して全体を引き締める）
        drawVignetteEffect(canvas)
    }

    /**
     * ベースグラデーションを描画
     */
    private fun drawBaseGradient(canvas: Canvas) {
        val currentColors = gradientColors[currentGradientIndex]
        val nextColors = gradientColors[nextGradientIndex]

        val interpolatedColors = IntArray(currentColors.size) { i ->
            interpolateColor(currentColors[i], nextColors[i], animationProgress)
        }

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
     * ビネット効果を描画（四隅を暗くする）
     */
    private fun drawVignetteEffect(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) * 0.8f // 画面の80%の半径

        // 中心から外側に向かって暗くなるRadialGradient
        val vignetteGradient = RadialGradient(
            centerX, centerY, radius,
            intArrayOf(Color.TRANSPARENT, Color.argb(120, 0, 0, 0)),
            floatArrayOf(0.3f, 1.0f),
            Shader.TileMode.CLAMP
        )

        vignettePaint.shader = vignetteGradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
    }

    /**
     * 発光エフェクト（画面四隅から柔らかい光）
     */
    private fun drawGlowEffect(canvas: Canvas) {
        // 左上の発光
        drawGlow(canvas, width * 0.15f, height * 0.15f, glowAnimationTime)

        // 右上の発光（位相をずらす）
        drawGlow(canvas, width * 0.85f, height * 0.15f, glowAnimationTime + 1.5f)

        // 左下の発光（位相をずらす）
        drawGlow(canvas, width * 0.15f, height * 0.85f, glowAnimationTime + 3.0f)

        // 右下の発光（位相をずらす）
        drawGlow(canvas, width * 0.85f, height * 0.85f, glowAnimationTime + 4.5f)
    }

    /**
     * 個別の発光を描画
     */
    private fun drawGlow(canvas: Canvas, x: Float, y: Float, time: Float) {
        // サイン波で明るさを変化（ゆっくり呼吸するように）
        val brightness = (kotlin.math.sin(time * 0.5) * 0.5 + 0.5).toFloat() // 0.0-1.0
        val alpha = (40 + brightness * 40).toInt() // 40-80のアルファ値

        val radius = 300f + brightness * 100f // 300-400pxの半径

        // 現在のグラデーション色をベースにした発光色
        val baseColor = gradientColors[currentGradientIndex][0]
        val r = Color.red(baseColor)
        val g = Color.green(baseColor)
        val b = Color.blue(baseColor)

        val glowGradient = RadialGradient(
            x, y, radius,
            intArrayOf(Color.argb(alpha, r, g, b), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        glowPaint.shader = glowGradient
        canvas.drawCircle(x, y, radius, glowPaint)
    }

    /**
     * 波紋効果を描画（中心から広がる円 - ランダムな発生タイミング）
     */
    private fun drawRippleEffect(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f
        val baseMaxRadius = min(width, height) * 0.6f

        // 白色でコントラストを出す
        val r = 255
        val g = 255
        val b = 255

        val currentTime = System.currentTimeMillis()

        // 3つの波紋を独立した周期とオフセットで描画
        for (i in 0 until rippleCount) {
            // 各波紋の独自の周期とオフセットを使用
            val duration = rippleDurations[i].toLong()
            val offset = rippleOffsets[i]
            val progress = ((currentTime % duration).toFloat() / duration.toFloat() + offset) % 1.0f

            // 半径にも少しランダム性を持たせる（±10%）
            val radiusVariation = 0.9f + (offset * 0.2f)
            val maxRadius = baseMaxRadius * radiusVariation

            val radius = maxRadius * progress
            val alpha = ((1f - progress) * 80).toInt() // 徐々に透明に（控えめに）

            ripplePaint.color = Color.argb(alpha, r, g, b)
            canvas.drawCircle(centerX, centerY, radius, ripplePaint)
        }
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
