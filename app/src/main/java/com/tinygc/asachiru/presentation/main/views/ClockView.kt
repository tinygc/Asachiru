package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.tinygc.asachiru.domain.entity.DateTime

/**
 * 時計を表示するカスタムビュー
 *
 * 時刻（HH:MM）と日付（MM/DD (Day)）を表示します。
 * 曜日は色分け表示されます：
 * - 日曜: 赤
 * - 土曜: 青
 * - 平日: 黒
 *
 * Glassmorphism効果で半透明の背景と境界線を追加しています。
 */
class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentDateTime: DateTime = DateTime.EMPTY

    // Material Design 3 + Neumorphism背景用
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x50202020.toInt() // 暗めの半透明（白文字の視認性確保）
        style = Paint.Style.FILL
    }

    // Neumorphism外側の影用
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        alpha = (255 * 0.15f).toInt() // 15%の薄い影
        style = Paint.Style.FILL
    }

    // Neumorphism内側のハイライト用
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = (255 * 0.4f).toInt() // 40%の明るいハイライト
        style = Paint.Style.FILL
    }

    // 境界線用（エレベーション表現）
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = (255 * 0.5f).toInt() // 50%の不透明度（強調）
        style = Paint.Style.STROKE
        strokeWidth = 2f // より繊細に
    }

    private val backgroundRect = RectF()
    private val shadowRect = RectF()
    private val highlightRect = RectF()

    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 60f, context.resources.displayMetrics)
        color = Color.WHITE
        typeface = Typeface.MONOSPACE
        letterSpacing = 0.02f // Material Design 3準拠（視認性向上）
        setShadowLayer(8f, 2f, 2f, Color.argb(180, 0, 0, 0)) // 影を追加（視認性向上）
    }

    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24f, context.resources.displayMetrics)
        typeface = Typeface.MONOSPACE
        letterSpacing = 0.02f // Material Design 3準拠（視認性向上）
        setShadowLayer(6f, 2f, 2f, Color.argb(180, 0, 0, 0)) // 影を追加（視認性向上）
    }

    /**
     * 日時を更新
     * 初回表示時はフェードインアニメーションで表示されます。
     * @param dateTime 表示する日時
     */
    fun updateDateTime(dateTime: DateTime) {
        val shouldAnimate = this.currentDateTime == DateTime.EMPTY && dateTime != DateTime.EMPTY
        this.currentDateTime = dateTime

        if (shouldAnimate) {
            // 初回表示時はフェードインアニメーション（Material Design 3準拠）
            alpha = 0f
            invalidate()
            animate()
                .alpha(1f)
                .setDuration(300) // 300ms
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        } else {
            invalidate() // 再描画をリクエスト
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Glassmorphism背景を描画
        drawGlassmorphismBackground(canvas)

        if (currentDateTime == DateTime.EMPTY) {
            return // 初期値の場合は何も描画しない
        }

        drawTime(canvas)
        drawDate(canvas)
    }

    /**
     * Material Design 3 + Neumorphism背景を描画
     * - 外側の影（エレベーション）
     * - 半透明背景
     * - 内側のハイライト（Neumorphism）
     * - 境界線
     */
    private fun drawGlassmorphismBackground(canvas: Canvas) {
        // パディング（余白たっぷり）
        val padding = 24f
        val cornerRadius = 48f // より大きな角丸（Material Design 3風）
        val shadowOffset = 8f // 影のオフセット

        // 外側の影（右下にずらして描画）
        shadowRect.set(
            padding + shadowOffset,
            padding + shadowOffset,
            width.toFloat() - padding + shadowOffset,
            height.toFloat() - padding + shadowOffset
        )
        canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint)

        // メイン背景
        backgroundRect.set(
            padding,
            padding,
            width.toFloat() - padding,
            height.toFloat() - padding
        )
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, backgroundPaint)

        // 内側のハイライト（左上に小さく描画）Neumorphism効果
        val highlightInset = 4f
        highlightRect.set(
            padding + highlightInset,
            padding + highlightInset,
            width.toFloat() - padding - highlightInset * 8,
            height.toFloat() - padding - highlightInset * 8
        )
        canvas.drawRoundRect(highlightRect, cornerRadius - highlightInset, cornerRadius - highlightInset, highlightPaint)

        // 境界線（エレベーション強調）
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, borderPaint)
    }

    /**
     * 時刻を描画（Material Design 3 - 8dpグリッド準拠）
     */
    private fun drawTime(canvas: Canvas) {
        val text = currentDateTime.timeString
        val textWidth = timePaint.measureText(text)

        // 8dpグリッド準拠のパディング（48dp = 48f）
        val paddingHorizontal = 48f
        val availableWidth = width - (paddingHorizontal * 2)

        if (textWidth > availableWidth) {
            val scale = availableWidth / textWidth
            canvas.save()
            canvas.scale(scale, scale, paddingHorizontal, 0f)
        }

        val x = paddingHorizontal
        val y = height * 0.4f // Viewの高さの40%の位置に配置

        canvas.drawText(text, x, y, timePaint)

        if (textWidth > availableWidth) {
            canvas.restore()
        }
    }

    /**
     * 日付を描画（曜日の色分けあり - Material Design 3 - 8dpグリッド準拠）
     */
    private fun drawDate(canvas: Canvas) {
        val text = currentDateTime.dateString
        val textWidth = datePaint.measureText(text)

        // 8dpグリッド準拠のパディング（48dp = 48f）
        val paddingHorizontal = 48f
        val availableWidth = width - (paddingHorizontal * 2)

        if (textWidth > availableWidth) {
            val scale = availableWidth / textWidth
            canvas.save()
            canvas.scale(scale, scale, paddingHorizontal, 0f)
        }

        val x = paddingHorizontal
        val y = height * 0.7f // Viewの高さの70%の位置に配置

        // 曜日の色を設定
        datePaint.color = currentDateTime.dayOfWeek.getColor()

        canvas.drawText(text, x, y, datePaint)

        if (textWidth > availableWidth) {
            canvas.restore()
        }
    }
}
