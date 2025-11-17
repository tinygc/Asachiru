package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.tinygc.asachiru.domain.entity.News
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * ニュース取得状況をデバッグ表示するカスタムビュー
 *
 * RSS取得時刻、各記事のタイトルと公開時刻をリスト表示します。
 */
class NewsDebugView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var newsList: List<News> = emptyList()
    private var lastFetchTime: Long = 0L

    // 背景用
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x80000000.toInt() // 黒の半透明背景
        style = Paint.Style.FILL
    }

    private val backgroundRect = RectF()

    // テキスト用
    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
        color = Color.YELLOW
        setShadowLayer(4f, 2f, 2f, Color.argb(180, 0, 0, 0))
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        color = Color.WHITE
        setShadowLayer(4f, 2f, 2f, Color.argb(180, 0, 0, 0))
    }

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.JAPAN).apply {
        timeZone = TimeZone.getTimeZone("Asia/Tokyo")
    }

    /**
     * デバッグ情報を更新
     */
    fun updateDebugInfo(newsList: List<News>, lastFetchTime: Long) {
        this.newsList = newsList
        this.lastFetchTime = lastFetchTime
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 半透明背景を描画
        backgroundRect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRect(backgroundRect, backgroundPaint)

        val padding = 20f
        var y = padding + headerPaint.textSize

        // RSS取得時刻を描画
        val fetchTimeText = if (lastFetchTime > 0) {
            "RSS取得時刻: ${dateFormat.format(Date(lastFetchTime))}"
        } else {
            "RSS取得時刻: 未取得"
        }
        canvas.drawText(fetchTimeText, padding, y, headerPaint)
        y += headerPaint.textSize + 10f

        // 記事数を描画
        canvas.drawText("記事数: ${newsList.size}", padding, y, headerPaint)
        y += headerPaint.textSize + 20f

        // 各記事を描画
        for (index in newsList.indices) {
            val news = newsList[index]
            
            // インデックスと公開時刻
            val timeText = "${index + 1}. [${dateFormat.format(Date(news.publishedAt))}]"
            canvas.drawText(timeText, padding, y, textPaint)
            y += textPaint.textSize + 5f

            // タイトル（長すぎる場合は省略）
            val titleText = if (news.title.length > 50) {
                "   ${news.title.take(50)}..."
            } else {
                "   ${news.title}"
            }
            canvas.drawText(titleText, padding, y, textPaint)
            y += textPaint.textSize + 15f

            // 画面からはみ出したら終了
            if (y > height - padding) {
                break
            }
        }
    }
}
