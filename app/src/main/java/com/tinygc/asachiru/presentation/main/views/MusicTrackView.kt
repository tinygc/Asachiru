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
import com.tinygc.asachiru.domain.entity.Music

/**
 * 音楽トラック名を表示するカスタムビュー
 *
 * 再生中のトラック名とアーティスト名を画面中央に表示します。
 * Glassmorphism効果で半透明の背景と境界線を追加しています。
 */
class MusicTrackView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentMusic: Music? = null
    private var currentPosition: Long = 0L // 現在の再生位置（ミリ秒）
    private var errorMessage: String? = null

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

    // トラック名用
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18f, context.resources.displayMetrics)
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.02f // Material Design 3準拠（視認性向上）
        setShadowLayer(8f, 2f, 2f, Color.argb(180, 0, 0, 0)) // 影を追加（視認性向上）
    }

    // アーティスト名用
    private val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, context.resources.displayMetrics)
        color = Color.WHITE
        alpha = (255 * 0.8f).toInt() // 80%の不透明度
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.02f // Material Design 3準拠（視認性向上）
        setShadowLayer(6f, 2f, 2f, Color.argb(180, 0, 0, 0)) // 影を追加（視認性向上）
    }

    // 再生時間用
    private val durationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, context.resources.displayMetrics)
        color = Color.WHITE
        alpha = (255 * 0.7f).toInt() // 70%の不透明度
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.02f // Material Design 3準拠（視認性向上）
        setShadowLayer(4f, 2f, 2f, Color.argb(180, 0, 0, 0)) // 影を追加（視認性向上）
    }

    // エラーメッセージ用
    private val errorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18f, context.resources.displayMetrics)
        color = Color.RED
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.02f // Material Design 3準拠（視認性向上）
        setShadowLayer(6f, 2f, 2f, Color.argb(180, 0, 0, 0)) // 影を追加（視認性向上）
    }

    /**
     * 音楽情報を更新
     * 初回表示時はフェードインアニメーションで表示されます。
     * @param music 表示する音楽情報（nullの場合は非表示）
     * @param position 現在の再生位置（ミリ秒）
     */
    fun updateMusic(music: Music?, position: Long = 0L) {
        val shouldAnimate = this.currentMusic == null && music != null && music != Music.EMPTY
        this.currentMusic = music
        this.currentPosition = position
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
        this.currentMusic = null
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

        currentMusic?.let {
            if (it != Music.EMPTY) {
                drawTrackInfo(canvas, it)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // タイトル + アーティスト + 再生時間 + 余白分を目安に高さ上限を決める
        val desiredHeight = titlePaint.textSize + artistPaint.textSize + durationPaint.textSize + 96f
        val maxHeight = desiredHeight.toInt()

        val measuredWidth = measuredWidth
        val measuredHeight = measuredHeight.coerceAtMost(maxHeight)

        setMeasuredDimension(measuredWidth, measuredHeight)
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
     * エラーメッセージを描画（Material Design 3 - 8dpグリッド準拠）
     */
    private fun drawError(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f
        val text = "Error: $errorMessage"
        val textWidth = errorPaint.measureText(text)

        // 8dpグリッド準拠のパディング（48dp = 48f）
        val paddingHorizontal = 48f
        val availableWidth = width - (paddingHorizontal * 2) // 左右パディング

        if (textWidth > availableWidth) {
            val scale = availableWidth / textWidth
            canvas.save()
            canvas.scale(scale, scale, centerX, centerY)
        }

        canvas.drawText(text, centerX, centerY, errorPaint)

        if (textWidth > availableWidth) {
            canvas.restore()
        }
    }

    /**
     * トラック情報を描画（中央に配置 - Material Design 3 - 8dpグリッド準拠）
     */
    private fun drawTrackInfo(canvas: Canvas, music: Music) {
        val centerX = width / 2f
        val centerY = height / 2f

        // 8dpグリッド準拠のパディング（48dp = 48f）
        val paddingHorizontal = 48f
        val availableWidth = width - (paddingHorizontal * 2) // 左右パディング

        // トラック名（中央より少し上）
        val titleText = "🎵 ${music.title}"
        val titleWidth = titlePaint.measureText(titleText)

        if (titleWidth > availableWidth) {
            val scale = availableWidth / titleWidth
            canvas.save()
            canvas.scale(scale, scale, centerX, centerY - 20f)
        }

        canvas.drawText(titleText, centerX, centerY - 20f, titlePaint)

        if (titleWidth > availableWidth) {
            canvas.restore()
        }

        // アーティスト名（中央より少し下）
        val artistText = music.artist
        val artistWidth = artistPaint.measureText(artistText)

        if (artistWidth > availableWidth) {
            val scale = availableWidth / artistWidth
            canvas.save()
            canvas.scale(scale, scale, centerX, centerY + 40f)
        }

        canvas.drawText(artistText, centerX, centerY + 40f, artistPaint)

        if (artistWidth > availableWidth) {
            canvas.restore()
        }

        // 再生時間（アーティスト名のさらに下）
        val durationText = formatDuration(currentPosition) + " / " + formatDuration(music.durationMs)
        val durationWidth = durationPaint.measureText(durationText)

        if (durationWidth > availableWidth) {
            val scale = availableWidth / durationWidth
            canvas.save()
            canvas.scale(scale, scale, centerX, centerY + 80f)
        }

        canvas.drawText(durationText, centerX, centerY + 80f, durationPaint)

        if (durationWidth > availableWidth) {
            canvas.restore()
        }
    }

    /**
     * ミリ秒を "分:秒" フォーマットに変換
     * @param ms ミリ秒
     * @return "分:秒" 形式の文字列（例: "1:23"）
     */
    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}
