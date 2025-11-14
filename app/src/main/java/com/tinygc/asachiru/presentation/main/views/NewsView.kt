package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.tinygc.asachiru.domain.entity.News

/**
 * ニューステキストを表示するカスタムビュー
 *
 * 読み上げ中のニュースタイトルを画面左下に表示します。
 * エラー時は赤色でエラーメッセージを表示します。
 * Glassmorphism効果で半透明の背景と境界線を追加しています。
 */
class NewsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentNews: News? = null
    private var errorMessage: String? = null

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

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 96f // Android TV (4K)用に3倍に拡大（視認性向上）
        color = Color.WHITE
        setShadowLayer(6f, 2f, 2f, Color.argb(180, 0, 0, 0)) // 影を追加（視認性向上）
    }

    /**
     * ニュースを更新
     * @param news 表示するニュース（nullの場合は非表示）
     */
    fun updateNews(news: News?) {
        this.currentNews = news
        this.errorMessage = null
        invalidate()
    }

    /**
     * エラーメッセージを表示
     * @param message エラーメッセージ
     */
    fun showError(message: String) {
        this.errorMessage = message
        this.currentNews = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Glassmorphism背景を描画
        drawGlassmorphismBackground(canvas)

        if (errorMessage != null) {
            drawError(canvas)
            return
        }

        currentNews?.let {
            drawNewsTitle(canvas, it)
        }
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
     * エラーメッセージを描画（画面左下）
     */
    private fun drawError(canvas: Canvas) {
        textPaint.color = Color.RED
        val text = "Error: $errorMessage"
        val textWidth = textPaint.measureText(text)
        val availableWidth = width - 100f // 左右パディング50fずつ

        if (textWidth > availableWidth) {
            val scale = availableWidth / textWidth
            canvas.save()
            canvas.scale(scale, scale, 50f, 0f)
        }

        canvas.drawText(text, 50f, height - 50f, textPaint)

        if (textWidth > availableWidth) {
            canvas.restore()
        }
    }

    /**
     * ニュースタイトルを描画（画面左下）
     */
    private fun drawNewsTitle(canvas: Canvas, news: News) {
        textPaint.color = Color.WHITE
        val text = "📰 ${news.title}"
        val textWidth = textPaint.measureText(text)
        val availableWidth = width - 100f // 左右パディング50fずつ

        if (textWidth > availableWidth) {
            val scale = availableWidth / textWidth
            canvas.save()
            canvas.scale(scale, scale, 50f, 0f)
        }

        canvas.drawText(text, 50f, height - 50f, textPaint)

        if (textWidth > availableWidth) {
            canvas.restore()
        }
    }
}
