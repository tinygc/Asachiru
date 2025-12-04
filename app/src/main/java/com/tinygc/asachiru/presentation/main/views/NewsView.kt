package com.tinygc.asachiru.presentation.main.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
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

    // TTS読み上げ中の点滅表示用
    private var blinkAlpha: Float = 0f // 点滅のアルファ値（0.0～1.0）

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

    // TTS読み上げ中の点滅表示用（薄い赤色）
    private val ttsBlinkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
        // alpha値は描画時に動的設定
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

    // TTS状態表示用（小さめ）
    private val ttsStatusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        color = Color.WHITE
        alpha = (255 * 0.9f).toInt()
        letterSpacing = 0.02f
        setShadowLayer(4f, 2f, 2f, Color.argb(180, 0, 0, 0))
    }

    // 三角アイコン用（TTS状態表示の左右）
    private val ttsArrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, context.resources.displayMetrics)
        color = Color.WHITE
        alpha = (255 * 0.6f).toInt() // 少し薄めに表示
    }

    init {
        // スマホとTVでテキストサイズを調整
        if (com.tinygc.asachiru.domain.util.DeviceUtils.isPhone(context)) {
            // スマホ: 適度なフォントサイズ（読みやすさとはみ出し防止のバランス）
            textPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18f, context.resources.displayMetrics)
            timePaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        }
        // TV用はデフォルトのまま（textPaint: 24sp, timePaint: 16sp）

        // フレーバーに応じた色設定
        if (isAsachiru()) {
            // Asachiru: パステルカラーの柔らかい半透明背景
            backgroundPaint.color = getFlavorColor(
                Color.argb((255 * 0.85f).toInt(), 255, 250, 255), // ほぼ白の半透明
                0x50202020.toInt()
            )
            highlightPaint.color = getFlavorColor(
                Color.argb((255 * 0.25f).toInt(), 255, 235, 250), // ラベンダーの薄い透明
                Color.argb((255 * 0.25f).toInt(), 135, 206, 250)
            )
            borderPaint.color = getFlavorColor(
                Color.argb(200, 200, 180, 255), // ラベンダーボーダー
                Color.argb(100, 100, 149, 237)
            )
            progressBackgroundPaint.color = getFlavorColor(
                Color.argb((255 * 0.3f).toInt(), 220, 200, 250), // ラベンダー背景
                Color.argb((255 * 0.25f).toInt(), 135, 206, 250)
            )
            progressForegroundPaint.shader = if (isAsachiru()) {
                // Asachiru: パステルグラデーション
                LinearGradient(
                    0f, 0f, width.toFloat(), 0f,
                    intArrayOf(
                        Color.argb((255 * 0.75f).toInt(), 200, 180, 255), // パステルラベンダー
                        Color.argb((255 * 0.85f).toInt(), 180, 220, 255)  // パステルスカイブルー
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            } else {
                // FeedWatch: 既存のグラデーション
                LinearGradient(
                    0f, 0f, width.toFloat(), 0f,
                    intArrayOf(
                        Color.argb((255 * 0.75f).toInt(), 100, 180, 255),
                        Color.argb((255 * 0.85f).toInt(), 0, 220, 255)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            ttsBlinkPaint.color = getFlavorColor(
                Color.argb(200, 255, 180, 180), // パステルコーラル
                Color.argb(200, 255, 100, 100)
            )
        }
    }

    /**
     * dp値をピクセル値に変換するヘルパー関数
     */
    private fun Float.dp(): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics)
    }

    /**
     * Asachiruフレーバーかどうかを判定
     */
    private fun isAsachiru(): Boolean {
        return com.tinygc.asachiru.BuildConfig.FLAVOR == "asachiru"
    }

    /**
     * フレーバーに応じたカラーを取得
     * @param asachiruColor Asachiru用のカラー値（直接指定）
     * @param feedwatchColor FeedWatch用のカラー値（直接指定）
     */
    private fun getFlavorColor(asachiruColor: Int, feedwatchColor: Int): Int {
        return if (isAsachiru()) asachiruColor else feedwatchColor
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
     * TTS機能のON/OFF設定値を保持します
     */
    fun setEnableTts(enable: Boolean) {
        this.enableTts = enable
        invalidate()
    }

    /**
     * TTS読み上げ中状態の更新
     * 読み上げ中は点滅アニメーション開始、完了時は停止
     */
    fun setIsSpeaking(speaking: Boolean) {
        // TTS読み上げ状態に応じてアニメーション制御
        if (speaking && enableTts) {
            // TTS有効かつ読み上げ中の場合のみアニメーション開始
            blinkAnimator.start()
        } else {
            // 読み上げ完了時またはTTS無効時はアニメーション停止
            blinkAnimator.cancel()
            blinkAlpha = 0f // リセット
        }

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

        // TTS ON時は読み上げ中を示す点滅表示（Viewの一番上）
        if (enableTts && blinkAlpha > 0f) {
            drawTtsBlinkIndicator(canvas)
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

        // TTS状態表示（右上）
        drawTtsStatus(canvas)
    }

    /**
     * Material Design 3 + Neumorphism背景を描画
     * - 半透明背景
     * - 内側のハイライト（Neumorphism）
     * - 境界線
     */
    private fun drawGlassmorphismBackground(canvas: Canvas) {
        val padding = 8f.dp()
        val cornerRadius = 16f.dp()

        // メイン背景
        backgroundRect.set(
            padding,
            padding,
            width.toFloat() - padding,
            height.toFloat() - padding
        )
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, backgroundPaint)

        // 内側のハイライト（左上に小さく描画）Neumorphism効果
        val highlightInset = 2f.dp()
        highlightRect.set(
            padding + highlightInset,
            padding + highlightInset,
            width.toFloat() - padding - highlightInset * 4,
            height.toFloat() - padding - highlightInset * 4
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

        // 8dpグリッド準拠のパディング（48dp）
        val paddingHorizontal = 48f.dp()
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
        // スマホとTVで異なるパディング
        val paddingHorizontal = if (com.tinygc.asachiru.domain.util.DeviceUtils.isPhone(context)) {
            16f.dp() // スマホ: 16dp（画面が小さいため）
        } else {
            48f.dp() // TV: 48dp（8dpグリッド準拠）
        }
        val lineSpacing = timePaint.textSize * 0.4f // 時刻のテキストサイズの40%を行間に

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

        // 時刻を描画（省略表示）
        val availableWidth = width - (paddingHorizontal * 2)
        val displayTime = ellipsizeText(timeText, timePaint, availableWidth)
        canvas.drawText(displayTime, paddingHorizontal, timeY, timePaint)

        // タイトルを描画（省略表示）
        textPaint.color = Color.WHITE
        val fullTitleText = "📰 ${news.title}"
        val displayTitle = ellipsizeText(fullTitleText, textPaint, availableWidth)
        canvas.drawText(displayTitle, paddingHorizontal, titleY, textPaint)
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
        val progressBarHeight = 4f.dp()  // 控えめに4dpに変更
        
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
     * TTS読み上げ中の点滅表示（画面最上部に赤く、滑らかにフェード）
     */
    private fun drawTtsBlinkIndicator(canvas: Canvas) {
        val indicatorHeight = 8f.dp() // プログレスバーと同じ高さ
        val indicatorY = 0f // 画面最上部

        // 赤色（alpha 0～70%で点滅、滑らかなフェードイン/アウト）
        val alpha = (blinkAlpha * 0.7f * 255).toInt() // 最大70%の不透明度
        ttsBlinkPaint.alpha = alpha

        val indicatorRect = RectF(
            0f,
            indicatorY,
            width.toFloat(),
            indicatorY + indicatorHeight
        )
        canvas.drawRect(indicatorRect, ttsBlinkPaint)
    }

    /**
     * 詳細ポップアップを描画（全画面）
     */
    private fun drawDetailPopup(canvas: Canvas) {
        val news = currentNews ?: return

        // フレーバー別の色定義
        val overlayColor = getFlavorColor(
            Color.argb(230, 255, 250, 255), // Asachiru: ほぼ白の半透明
            Color.argb(240, 10, 10, 15)     // FeedWatch: ダーク
        )
        val cardGradientTop = getFlavorColor(
            Color.argb(250, 255, 245, 255), // Asachiru: ペールラベンダー
            Color.argb(250, 30, 35, 45)     // FeedWatch: ダークブルー
        )
        val cardGradientBottom = getFlavorColor(
            Color.argb(250, 250, 235, 255), // Asachiru: ライトラベンダー
            Color.argb(250, 20, 25, 35)     // FeedWatch: より暗いダークブルー
        )
        val borderColor = getFlavorColor(
            Color.argb(180, 200, 180, 255), // Asachiru: パステルラベンダー
            Color.argb(180, 100, 120, 255)  // FeedWatch: 薄い青紫
        )
        val textColor = getFlavorColor(
            Color.argb(240, 107, 91, 127),  // Asachiru: 濃いめのラベンダー（#6B5B7F）
            Color.argb(240, 240, 245, 255)  // FeedWatch: 明るい白
        )
        val titleColor = getFlavorColor(
            Color.argb(255, 107, 91, 127),  // Asachiru: 濃いめのラベンダー（#6B5B7F）
            Color.argb(255, 255, 255, 255)  // FeedWatch: 白
        )
        val hintColor = getFlavorColor(
            Color.argb(150, 155, 139, 164), // Asachiru: 薄めのラベンダー
            Color.argb(150, 180, 185, 200)  // FeedWatch: グレー
        )
        val shadowColor = getFlavorColor(
            Color.argb(80, 200, 180, 230),  // Asachiru: ラベンダーシャドウ
            Color.argb(100, 0, 0, 0)        // FeedWatch: 黒シャドウ
        )

        // 半透明背景（ぼかし風）
        val overlayPaint = Paint().apply {
            color = overlayColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // モダンなグラデーションカード背景
        val cardPadding = 40f.dp()
        val cardCornerRadius = 16f.dp()
        val cardRect = RectF(
            cardPadding,
            cardPadding,
            width.toFloat() - cardPadding,
            height.toFloat() - cardPadding
        )

        // グラデーション背景
        val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(
                cardRect.left, cardRect.top,
                cardRect.right, cardRect.bottom,
                intArrayOf(cardGradientTop, cardGradientBottom),
                null,
                android.graphics.Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, gradientPaint)
        
        // 境界線（アクセントカラー）
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 3f.dp()
        }
        canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, borderPaint)

        // テキストペイント
        val detailTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, context.resources.displayMetrics)
            color = textColor
            letterSpacing = 0.03f
            setShadowLayer(4f.dp(), 2f.dp(), 2f.dp(), shadowColor)
        }

        val detailTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24f, context.resources.displayMetrics)
            color = titleColor
            letterSpacing = 0.02f
            isFakeBoldText = true
            setShadowLayer(6f.dp(), 3f.dp(), 3f.dp(), shadowColor)
        }

        // 行間をテキストサイズに対する相対値で定義（画面密度に自動対応）
        val titleLineSpacing = detailTitlePaint.textSize * 1.5f // タイトル行間: テキストサイズの150%
        val textLineSpacing = detailTextPaint.textSize * 1.3f // 本文行間: テキストサイズの130%

        // QRコードのサイズとパディングを定義（drawQRCodeと同じ値）
        val qrSize = 200f.dp()
        val qrPaddingValue = 50f.dp()
        val qrTotalWidth = qrSize + qrPaddingValue * 2  // QRコードが占有する幅
        
        // QRコードの上端Y座標を事前に計算（テキストとの余裕を持たせるため、少し上にマージンを追加）
        val qrTop = cardRect.bottom - qrSize - qrPaddingValue
        val qrSafeZone = qrTop - (textLineSpacing * 2)  // QRコードの少し上から幅を制限開始

        // タイトル、概要を描画
        val startX = cardPadding + 30f.dp()
        var currentY = cardPadding + 40f.dp()

        // 全幅テキスト幅（QRコードより上で使用）
        val fullTextMaxWidth = width - (cardPadding + 30f.dp()) * 2
        // 制限幅テキスト幅（QRコードと重なる高さで使用）
        val restrictedTextMaxWidth = width - (cardPadding + 30f.dp()) * 2 - qrTotalWidth

        // タイトル描画（行ごとに幅を動的に調整）
        val titleWords = news.title.split("")
        var currentTitleLine = ""
        titleWords.forEach { char ->
            val testLine = currentTitleLine + char
            // 次の行がQRコードのセーフゾーンに入るかチェック
            val maxWidth = if (currentY + titleLineSpacing < qrSafeZone) fullTextMaxWidth else restrictedTextMaxWidth
            val lineWidth = detailTitlePaint.measureText(testLine)
            
            if (lineWidth > maxWidth && currentTitleLine.isNotEmpty()) {
                // 現在の行を描画
                canvas.drawText(currentTitleLine, startX, currentY, detailTitlePaint)
                currentY += titleLineSpacing
                currentTitleLine = char
            } else {
                currentTitleLine = testLine
            }
        }
        // 最後の行を描画
        if (currentTitleLine.isNotEmpty()) {
            canvas.drawText(currentTitleLine, startX, currentY, detailTitlePaint)
            currentY += titleLineSpacing
        }

        currentY += detailTitlePaint.textSize * 0.5f // タイトルと概要の間隔: タイトルサイズの50%

        // 概要描画（行ごとに幅を動的に調整）
        val description = news.description.ifBlank { news.title }
        val descWords = description.split("")
        var currentDescLine = ""
        descWords.forEach { char ->
            val testLine = currentDescLine + char
            // 次の行がQRコードのセーフゾーンに入るかチェック
            val maxWidth = if (currentY + textLineSpacing < qrSafeZone) fullTextMaxWidth else restrictedTextMaxWidth
            val lineWidth = detailTextPaint.measureText(testLine)
            
            if (lineWidth > maxWidth && currentDescLine.isNotEmpty()) {
                // 現在の行を描画
                canvas.drawText(currentDescLine, startX, currentY, detailTextPaint)
                currentY += textLineSpacing
                currentDescLine = char
            } else {
                currentDescLine = testLine
            }
        }
        // 最後の行を描画
        if (currentDescLine.isNotEmpty()) {
            canvas.drawText(currentDescLine, startX, currentY, detailTextPaint)
            currentY += textLineSpacing
        }
        
        // QRコード表示（右下）
        drawQRCode(canvas, news.id, cardRect)
        
        // フッター情報（閉じるヒント）
        val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, context.resources.displayMetrics)
            color = hintColor
            letterSpacing = 0.02f
        }
        val hintText = "🔙 戻るキーで閉じる"
        val hintY = height.toFloat() - cardPadding - 20f.dp()
        canvas.drawText(hintText, startX, hintY, hintPaint)
    }

    /**
     * QRコードを生成して描画
     */
    private fun drawQRCode(canvas: Canvas, url: String, cardRect: RectF) {
        // URLが変わった場合のみQRコードを再生成
        val qrSizePixels = 200f.dp().toInt()
        if (qrCodeUrl != url || qrCodeBitmap == null) {
            qrCodeUrl = url
            qrCodeBitmap = generateQRCode(url, qrSizePixels)
        }
        
        val bitmap = qrCodeBitmap ?: return

        // QRコードを右下に配置（画面端から適度なマージン）
        val qrSize = 200f.dp()
        val qrPadding = 50f.dp()  // カード端からのマージン
        val qrLeft = cardRect.right - qrSize - qrPadding
        val qrTop = cardRect.bottom - qrSize - qrPadding

        // 白い背景
        val qrBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val qrBgMargin = 10f.dp()
        val qrBgRect = RectF(
            qrLeft - qrBgMargin,
            qrTop - qrBgMargin,
            qrLeft + qrSize + qrBgMargin,
            qrTop + qrSize + qrBgMargin
        )
        val qrBgCornerRadius = 8f.dp()
        canvas.drawRoundRect(qrBgRect, qrBgCornerRadius, qrBgCornerRadius, qrBgPaint)
        
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
        val labelY = qrTop - 15f.dp()
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

    /**
     * テキストを指定幅で省略表示（...）
     */
    private fun ellipsizeText(text: String, paint: Paint, maxWidth: Float): String {
        val textWidth = paint.measureText(text)
        if (textWidth <= maxWidth) {
            return text
        }

        // "..." の幅を計算
        val ellipsis = "..."
        val ellipsisWidth = paint.measureText(ellipsis)
        val availableWidth = maxWidth - ellipsisWidth

        // 文字を1つずつ追加して、maxWidthを超えない最大長を見つける
        var truncatedText = ""
        for (i in text.indices) {
            val testText = truncatedText + text[i]
            if (paint.measureText(testText) > availableWidth) {
                break
            }
            truncatedText = testText
        }

        return truncatedText + ellipsis
    }

    // TTS読み上げ中の点滅アニメーション（4秒周期、sin波で滑らかなフェードイン/アウト）
    private val blinkAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 4000 // 4秒周期（よりゆったり）
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        addUpdateListener { animation ->
            val fraction = animation.animatedFraction
            // sin波を使って滑らかなフェードイン/アウト（0→1→0）
            blinkAlpha = kotlin.math.sin(fraction * Math.PI).toFloat()
            invalidate() // 再描画
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // TTS ONの場合のみアニメーション開始
        if (enableTts) {
            blinkAnimator.start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        blinkAnimator.cancel()
    }

    /**
     * TTS状態表示を右上に描画（◀ TTS: ON ▶ 形式）
     */
    private fun drawTtsStatus(canvas: Canvas) {
        val statusText = if (enableTts) "TTS: ON" else "TTS: OFF"
        val leftArrow = "◀"
        val rightArrow = "▶"

        // パディング
        val paddingHorizontal = if (com.tinygc.asachiru.domain.util.DeviceUtils.isPhone(context)) {
            16f.dp()
        } else {
            48f.dp()
        }
        val paddingVertical = 24f.dp()

        // テキスト幅計算
        val statusWidth = ttsStatusPaint.measureText(statusText)
        val arrowWidth = ttsArrowPaint.measureText(leftArrow)
        val spacing = 8f.dp() // 矢印とテキストの間隔

        // 全体の幅
        val totalWidth = arrowWidth + spacing + statusWidth + spacing + arrowWidth

        // 右上に配置
        val startX = width - paddingHorizontal - totalWidth
        val y = paddingVertical

        // 左矢印
        canvas.drawText(leftArrow, startX, y, ttsArrowPaint)

        // TTS状態テキスト
        val textX = startX + arrowWidth + spacing
        canvas.drawText(statusText, textX, y, ttsStatusPaint)

        // 右矢印
        val rightArrowX = textX + statusWidth + spacing
        canvas.drawText(rightArrow, rightArrowX, y, ttsArrowPaint)
    }
}
