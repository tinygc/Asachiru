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
    private var showDetail: Boolean = false
    private var enableTts: Boolean = false

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

    /**
     * 詳細表示の切り替え
     */
    fun setShowDetail(show: Boolean) {
        this.showDetail = show
        invalidate()
    }

    /**
     * TTS有効状態の更新
     */
    fun setEnableTts(enable: Boolean) {
        this.enableTts = enable
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 詳細表示時は全画面ポップアップ
        if (showDetail) {
            drawDetailPopup(canvas)
            return
        }

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
     * ニュースタイトルと時刻を描画（垂直方向中央寄せ - Material Design 3 - 8dpグリッド準拠）
     */
    private fun drawNewsTitle(canvas: Canvas, news: News) {
        // 8dpグリッド準拠のパディング（48dp = 48f）
        val paddingHorizontal = 48f
        val lineSpacing = 20f // 時刻とタイトルの間隔

        // 記事の公開時刻を取得
        val timeText = formatPublishTime(news.publishedAt)

        // 全体の高さを計算
        val totalTextHeight = timePaint.textSize + lineSpacing + textPaint.textSize

        // 垂直方向の中央位置を計算
        val centerY = height / 2f

        // 時刻のY位置（中央から上にオフセット）
        val timeY = centerY - (totalTextHeight / 2f) + timePaint.textSize

        // タイトルのY位置（時刻の下）
        val titleY = timeY + lineSpacing + textPaint.textSize

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

    /**
     * 詳細ポップアップを描画（全画面）
     */
    private fun drawDetailPopup(canvas: Canvas) {
        val news = currentNews ?: return

        // 半透明黒背景
        val overlayPaint = Paint().apply {
            color = Color.argb(200, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // 白い背景カード
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val cardPadding = 100f
        val cardCornerRadius = 48f
        val cardRect = RectF(
            cardPadding,
            cardPadding,
            width.toFloat() - cardPadding,
            height.toFloat() - cardPadding
        )
        canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, cardPaint)

        // テキストペイント（黒）
        val detailTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 80f
            color = Color.BLACK
            letterSpacing = 0.02f
        }

        val detailTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 100f
            color = Color.BLACK
            letterSpacing = 0.02f
            isFakeBoldText = true
        }

        val ttsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 60f
            color = if (enableTts) Color.GREEN else Color.GRAY
            letterSpacing = 0.02f
        }

        // タイトル、概要、TTS状態を描画
        val startX = cardPadding + 60f
        var currentY = cardPadding + 150f

        // TTS状態表示
        val ttsText = "TTS: ${if (enableTts) "ON" else "OFF"}"
        canvas.drawText(ttsText, startX, currentY, ttsPaint)
        currentY += 120f

        // タイトル
        canvas.drawText(news.title, startX, currentY, detailTitlePaint)
        currentY += 150f

        // 概要（descriptionがあれば表示、なければtitleを再表示）
        val description = news.description.ifBlank { news.title }
        val maxWidth = width - (cardPadding + 60f) * 2
        val lines = wrapText(description, detailTextPaint, maxWidth)

        lines.forEach { line ->
            canvas.drawText(line, startX, currentY, detailTextPaint)
            currentY += 100f
        }
    }

    /**
     * テキストを指定幅で折り返す
     */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        var currentLine = ""

        text.split(" ", "、", "。", "！", "？").forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine$word"
            val testWidth = paint.measureText(testLine)

            if (testWidth > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }
}
