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
        alpha = (255 * 0.15f).toInt() // 15%の不透明度
        style = Paint.Style.FILL
    }

    // Glassmorphism境界線用
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = (255 * 0.3f).toInt() // 30%の不透明度
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val backgroundRect = RectF()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
        color = Color.WHITE
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
        canvas.drawText(
            "Error: $errorMessage",
            50f, height - 50f, textPaint
        )
    }

    /**
     * ニュースタイトルを描画（画面左下）
     */
    private fun drawNewsTitle(canvas: Canvas, news: News) {
        textPaint.color = Color.WHITE
        canvas.drawText(
            "📰 ${news.title}",
            50f, height - 50f, textPaint
        )
    }
}
