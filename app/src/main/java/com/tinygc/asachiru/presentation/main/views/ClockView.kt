package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
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

    // Glassmorphism背景用
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = (255 * 0.35f).toInt() // 35%の不透明度（視認性向上のため増加）
        style = Paint.Style.FILL
    }

    // Glassmorphism境界線用
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = (255 * 0.3f).toInt() // 30%の不透明度
        style = Paint.Style.STROKE
        strokeWidth = 4f // 視認性向上のため太く
    }

    private val backgroundRect = RectF()

    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 240f // Android TV (4K)用に2倍に拡大
        color = Color.WHITE
        typeface = Typeface.MONOSPACE
        setShadowLayer(8f, 2f, 2f, Color.argb(180, 0, 0, 0)) // 影を追加（視認性向上）
    }

    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 96f // Android TV (4K)用に2倍に拡大
        typeface = Typeface.MONOSPACE
        setShadowLayer(6f, 2f, 2f, Color.argb(180, 0, 0, 0)) // 影を追加（視認性向上）
    }

    /**
     * 日時を更新
     * 即座に再描画が行われます。
     * @param dateTime 表示する日時
     */
    fun updateDateTime(dateTime: DateTime) {
        this.currentDateTime = dateTime
        invalidate() // 再描画をリクエスト
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
     * Glassmorphism背景を描画（角丸の半透明背景＋境界線）
     */
    private fun drawGlassmorphismBackground(canvas: Canvas) {
        // 少し内側に描画（パディング）
        val padding = 10f
        backgroundRect.set(
            padding,
            padding,
            width.toFloat() - padding,
            height.toFloat() - padding
        )

        val cornerRadius = 24f // 角丸の半径

        // 半透明背景
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, backgroundPaint)

        // 境界線
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, borderPaint)
    }

    /**
     * 時刻を描画
     */
    private fun drawTime(canvas: Canvas) {
        val text = currentDateTime.timeString
        val textWidth = timePaint.measureText(text)

        // Viewの幅に対してテキストが大きすぎる場合はスケーリング
        val availableWidth = width - 100f // 左右パディング50fずつ
        if (textWidth > availableWidth) {
            val scale = availableWidth / textWidth
            canvas.save()
            canvas.scale(scale, scale, 50f, 0f)
        }

        val x = 50f
        val y = height * 0.4f // Viewの高さの40%の位置に配置

        canvas.drawText(text, x, y, timePaint)

        if (textWidth > availableWidth) {
            canvas.restore()
        }
    }

    /**
     * 日付を描画（曜日の色分けあり）
     */
    private fun drawDate(canvas: Canvas) {
        val text = currentDateTime.dateString
        val textWidth = datePaint.measureText(text)

        // Viewの幅に対してテキストが大きすぎる場合はスケーリング
        val availableWidth = width - 100f // 左右パディング50fずつ
        if (textWidth > availableWidth) {
            val scale = availableWidth / textWidth
            canvas.save()
            canvas.scale(scale, scale, 50f, 0f)
        }

        val x = 50f
        val y = height * 0.7f // Viewの高さの70%の位置に配置

        // 曜日の色を設定
        datePaint.color = currentDateTime.dayOfWeek.getColor()

        canvas.drawText(text, x, y, datePaint)

        if (textWidth > availableWidth) {
            canvas.restore()
        }
    }
}
