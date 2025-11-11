package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.tinygc.asachiru.domain.entity.News

/**
 * ニューステキストを表示するカスタムビュー
 *
 * 読み上げ中のニュースタイトルを画面左下に表示します。
 * エラー時は赤色でエラーメッセージを表示します。
 */
class NewsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentNews: News? = null
    private var errorMessage: String? = null

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

        if (errorMessage != null) {
            drawError(canvas)
            return
        }

        currentNews?.let {
            drawNewsTitle(canvas, it)
        }
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
