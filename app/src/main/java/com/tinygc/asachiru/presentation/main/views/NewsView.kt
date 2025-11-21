package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
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
    private var progressPercent: Float = 0f // 次の記事までのプログレス（0.0～1.0）
    private var animatedProgress: Float = 0f // アニメーション用の現在のプログレス値
    
    // QRコードキャッシュ
    private var qrCodeBitmap: Bitmap? = null
    private var qrCodeUrl: String? = null

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

    // プログレスバー背景用（モダンで控えめ）
    private val progressBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((255 * 0.25f).toInt(), 135, 206, 250) // パステルブルー 25%
        style = Paint.Style.FILL
    }

    // プログレスバー前景用（グラデーション対応）
    private val progressForegroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        // グラデーション効果は描画時に動的設定
    }

    private val backgroundRect = RectF()
    private val shadowRect = RectF()
    private val highlightRect = RectF()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24f, context.resources.displayMetrics)
        color = Color.WHITE
        letterSpacing = 0.02f // Material Design 3準拠（視認性向上）
        setShadowLayer(6f, 2f, 2f, Color.argb(180, 0, 0, 0)) // 影を追加（視認性向上）
    }

    // 時刻表示用（タイトルより小さめ）
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16f, context.resources.displayMetrics)
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
            animatedProgress = 0f // プログレスもリセット
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

    /**
     * プログレスの更新(TTS OFF時の次の記事までの進行度)
     * 滑らかなアニメーションで遷移します
     */
    fun setProgress(percent: Float) {
        val targetProgress = percent.coerceIn(0f, 1f)
        this.progressPercent = targetProgress
        
        // 大きく変化する場合(記事切り替え時)は即座に反映、小さい変化はアニメーション
        val progressDiff = Math.abs(targetProgress - animatedProgress)
        if (progressDiff > 0.5f) {
            // 記事切り替えなど大きな変化: 即座に反映
            animatedProgress = targetProgress
            invalidate()
        } else {
            // 通常の進行: 滑らかなアニメーション(300ms)
            animate()
                .setDuration(300)
                .setInterpolator(FastOutSlowInInterpolator())
                .setUpdateListener { animation ->
                    // 現在のプログレス値から目標値まで補間
                    val fraction = animation.animatedFraction
                    animatedProgress = animatedProgress + (targetProgress - animatedProgress) * fraction
                    invalidate()
                }
                .start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 詳細表示時は全画面ポップアップ
        if (showDetail) {
            drawDetailPopup(canvas)
            return
        }

        // TTS OFF時はプログレスバーを最初に描画（Viewの一番上）
        if (!enableTts && progressPercent > 0f) {
            drawProgressBar(canvas)
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
        // 新しいCalendarインスタンスを明示的に作成（キャッシュの影響を避ける）
        val jstTimeZone = TimeZone.getTimeZone("Asia/Tokyo")
        val calendar = Calendar.getInstance().apply {
            timeZone = jstTimeZone
            timeInMillis = publishedAt
        }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return "${hour}時${minute}分"
    }

    /**
     * プログレスバーを描画（TTS OFF時、画面最上部にモダンデザイン）
     */
    private fun drawProgressBar(canvas: Canvas) {
        val progressBarHeight = 8f // 8dpのバー
        
        // 画面最上部に配置
        val progressBarY = 0f

        // 背景バー（全幅）
        val backgroundRect = RectF(
            0f,
            progressBarY,
            width.toFloat(),
            progressBarY + progressBarHeight
        )
        canvas.drawRect(backgroundRect, progressBackgroundPaint)

        // 前景バー（グラデーション付き、プログレスに応じて伸びる）
        val foregroundWidth = width * animatedProgress
        if (foregroundWidth > 0) {
            // グラデーション設定（パステルブルー → シアン）
            progressForegroundPaint.shader = android.graphics.LinearGradient(
                0f, progressBarY,
                foregroundWidth, progressBarY,
                intArrayOf(
                    Color.argb((255 * 0.75f).toInt(), 100, 180, 255), // パステルブルー 75%
                    Color.argb((255 * 0.85f).toInt(), 0, 220, 255)    // シアン 85%
                ),
                null,
                android.graphics.Shader.TileMode.CLAMP
            )
            
            val foregroundRect = RectF(
                0f,
                progressBarY,
                foregroundWidth,
                progressBarY + progressBarHeight
            )
            canvas.drawRect(foregroundRect, progressForegroundPaint)
        }
    }

    /**
     * 詳細ポップアップを描画（全画面）
     */
    private fun drawDetailPopup(canvas: Canvas) {
        val news = currentNews ?: return

        // ダークな半透明背景（ぼかし風）
        val overlayPaint = Paint().apply {
            color = Color.argb(240, 10, 10, 15)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // モダンなグラデーションカード背景
        val cardPadding = 80f
        val cardCornerRadius = 32f
        val cardRect = RectF(
            cardPadding,
            cardPadding,
            width.toFloat() - cardPadding,
            height.toFloat() - cardPadding
        )
        
        // カードの影（深いエレベーション）
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(100, 0, 0, 0)
            style = Paint.Style.FILL
        }
        val shadowRect = RectF(
            cardPadding + 12f,
            cardPadding + 12f,
            width.toFloat() - cardPadding + 12f,
            height.toFloat() - cardPadding + 12f
        )
        canvas.drawRoundRect(shadowRect, cardCornerRadius, cardCornerRadius, shadowPaint)
        
        // グラデーション背景（ダークモード風）
        val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(
                cardRect.left, cardRect.top,
                cardRect.right, cardRect.bottom,
                intArrayOf(
                    Color.argb(250, 30, 35, 45),   // 濃いダークブルー
                    Color.argb(250, 20, 25, 35)    // より暗いダークブルー
                ),
                null,
                android.graphics.Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, gradientPaint)
        
        // 境界線（アクセントカラー）
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 100, 120, 255) // 薄い青紫
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, borderPaint)

        // テキストペイント（明るい白）
        val detailTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24f, context.resources.displayMetrics)
            color = Color.argb(240, 240, 245, 255)
            letterSpacing = 0.03f
            setShadowLayer(4f, 2f, 2f, Color.argb(100, 0, 0, 0)) // テキストに影
        }

        val detailTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 28f, context.resources.displayMetrics)
            color = Color.argb(255, 255, 255, 255)
            letterSpacing = 0.02f
            isFakeBoldText = true
            setShadowLayer(6f, 3f, 3f, Color.argb(150, 0, 0, 0))
        }

        // タイトル、概要を描画
        val startX = cardPadding + 50f
        var currentY = cardPadding + 140f

        // タイトル
        val titleLines = wrapText(news.title, detailTitlePaint, width - (cardPadding + 50f) * 2)
        titleLines.forEach { line ->
            canvas.drawText(line, startX, currentY, detailTitlePaint)
            currentY += 130f  // 行間を広げた（110f → 130f）
        }
        
        currentY += 30f

        // 概要（descriptionがあれば表示、なければtitleを再表示）
        val description = news.description.ifBlank { news.title }
        val maxWidth = width - (cardPadding + 50f) * 2
        val lines = wrapText(description, detailTextPaint, maxWidth)

        lines.forEach { line ->
            canvas.drawText(line, startX, currentY, detailTextPaint)
            currentY += 110f  // 行間を広げた（90f → 110f）
        }
        
        // QRコード表示（右下）
        drawQRCode(canvas, news.id, cardRect)
        
        // フッター情報（閉じるヒント）
        val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, context.resources.displayMetrics)
            color = Color.argb(150, 180, 185, 200)
            letterSpacing = 0.02f
        }
        val hintText = "🔙 戻るキーで閉じる"
        val hintY = height.toFloat() - cardPadding - 40f
        canvas.drawText(hintText, startX, hintY, hintPaint)
    }

    /**
     * QRコードを生成して描画
     */
    private fun drawQRCode(canvas: Canvas, url: String, cardRect: RectF) {
        // URLが変わった場合のみQRコードを再生成
        if (qrCodeUrl != url || qrCodeBitmap == null) {
            qrCodeUrl = url
            qrCodeBitmap = generateQRCode(url, 300)
        }
        
        val bitmap = qrCodeBitmap ?: return
        
        // QRコードを右下に配置（画面端から十分なマージン）
        val qrSize = 300f
        val qrPadding = 100f  // カード端からのマージンを広めに
        val qrLeft = cardRect.right - qrSize - qrPadding
        val qrTop = cardRect.bottom - qrSize - qrPadding
        
        // 白い背景
        val qrBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val qrBgRect = RectF(
            qrLeft - 20f,
            qrTop - 20f,
            qrLeft + qrSize + 20f,
            qrTop + qrSize + 20f
        )
        canvas.drawRoundRect(qrBgRect, 16f, 16f, qrBgPaint)
        
        // QRコード描画
        canvas.drawBitmap(
            bitmap,
            null,
            RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize),
            null
        )
        
        // QRコードの説明テキスト
        val qrLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10f, context.resources.displayMetrics)
            color = Color.argb(200, 200, 205, 220)
            letterSpacing = 0.02f
        }
        val labelText = "記事URL"
        val labelWidth = qrLabelPaint.measureText(labelText)
        val labelX = qrLeft + (qrSize - labelWidth) / 2
        val labelY = qrTop - 30f
        canvas.drawText(labelText, labelX, labelY, qrLabelPaint)
    }
    
    /**
     * QRコードBitmapを生成
     */
    private fun generateQRCode(text: String, size: Int): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * テキストを指定幅で折り返す（文字単位で折り返し）
     */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        var currentLine = ""

        text.forEach { char ->
            val testLine = currentLine + char
            val testWidth = paint.measureText(testLine)

            if (testWidth > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = char.toString()
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
