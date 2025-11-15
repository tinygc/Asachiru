package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.tinygc.asachiru.domain.entity.News
import java.util.Calendar
import java.util.TimeZone

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

    // Material Design 3 + Neumorphism背景用
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = (255 * 0.6f).toInt() // 60%の不透明度（モダンデザイン対応）
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

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 96f // Android TV (4K)用に3倍に拡大（視認性向上）
        color = Color.WHITE
        letterSpacing = 0.02f // Material Design 3準拠（視認性向上）
        setShadowLayer(6f, 2f, 2f, Color.argb(180, 0, 0, 0)) // 影を追加（視認性向上）
    }

    // 時刻表示用（タイトルより小さめ）
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 64f // タイトルより小さめ
        color = Color.WHITE
        alpha = (255 * 0.8f).toInt() // 少し薄く表示
        letterSpacing = 0.02f // Material Design 3準拠（視認性向上）
        setShadowLayer(5f, 2f, 2f, Color.argb(180, 0, 0, 0)) // 影を追加（視認性向上）
    }

    /**
     * ニュースを更新
     * 初回表示時はフェードインアニメーションで表示されます。
     * @param news 表示するニュース（nullの場合は非表示）
     */
    fun updateNews(news: News?) {
        val shouldAnimate = this.currentNews == null && news != null
        this.currentNews = news
        this.errorMessage = null

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
            invalidate()
        }
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
     * Material Design 3 + Neumorphism背景を描画
     * - 外側の影（エレベーション）
     * - 半透明背景
     * - 内側のハイライト（Neumorphism）
     * - 境界線
     */
    private fun drawGlassmorphismBackground(canvas: Canvas) {
        val padding = 24f
        val cornerRadius = 48f
        val shadowOffset = 8f

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
     * エラーメッセージを描画（画面左下 - Material Design 3 - 8dpグリッド準拠）
     */
    private fun drawError(canvas: Canvas) {
        textPaint.color = Color.RED
        val text = "Error: $errorMessage"
        val textWidth = textPaint.measureText(text)

        // 8dpグリッド準拠のパディング（48dp = 48f）
        val paddingHorizontal = 48f
        val availableWidth = width - (paddingHorizontal * 2) // 左右パディング

        if (textWidth > availableWidth) {
            val scale = availableWidth / textWidth
            canvas.save()
            canvas.scale(scale, scale, paddingHorizontal, 0f)
        }

        canvas.drawText(text, paddingHorizontal, height - paddingHorizontal, textPaint)

        if (textWidth > availableWidth) {
            canvas.restore()
        }
    }

    /**
     * ニュースタイトルと時刻を描画（画面左下 - Material Design 3 - 8dpグリッド準拠）
     */
    private fun drawNewsTitle(canvas: Canvas, news: News) {
        // 8dpグリッド準拠のパディング（48dp = 48f）
        val paddingHorizontal = 48f
        val lineSpacing = 20f // 時刻とタイトルの間隔

        // 記事の公開時刻を取得
        val timeText = formatPublishTime(news.publishedAt)

        // タイトルのY位置（画面下部）
        val titleY = height - paddingHorizontal

        // 時刻のY位置（タイトルの上）
        val timeY = titleY - textPaint.textSize - lineSpacing

        // 時刻を描画
        val timeWidth = timePaint.measureText(timeText)
        val availableWidth = width - (paddingHorizontal * 2)

        if (timeWidth > availableWidth) {
            val scale = availableWidth / timeWidth
            canvas.save()
            canvas.scale(scale, scale, paddingHorizontal, 0f)
            canvas.drawText(timeText, paddingHorizontal, timeY, timePaint)
            canvas.restore()
        } else {
            canvas.drawText(timeText, paddingHorizontal, timeY, timePaint)
        }

        // タイトルを描画
        textPaint.color = Color.WHITE
        val titleText = "📰 ${news.title}"
        val titleWidth = textPaint.measureText(titleText)

        if (titleWidth > availableWidth) {
            val scale = availableWidth / titleWidth
            canvas.save()
            canvas.scale(scale, scale, paddingHorizontal, 0f)
            canvas.drawText(titleText, paddingHorizontal, titleY, textPaint)
            canvas.restore()
        } else {
            canvas.drawText(titleText, paddingHorizontal, titleY, textPaint)
        }
    }

    /**
     * 公開時刻をフォーマット（例：「15時30分」）
     */
    private fun formatPublishTime(publishedAt: Long): String {
        val jstTimeZone = TimeZone.getTimeZone("Asia/Tokyo")
        val calendar = Calendar.getInstance(jstTimeZone)
        calendar.timeInMillis = publishedAt
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return "${hour}時${minute}分"
    }
}
