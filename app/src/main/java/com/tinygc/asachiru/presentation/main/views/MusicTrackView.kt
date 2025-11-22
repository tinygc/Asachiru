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

    // トラック名用（控えめに調整）
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics) // 18sp → 14sp
        color = Color.WHITE
        alpha = (255 * 0.7f).toInt() // 70%の不透明度（控えめに）
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) // Boldを通常に
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.02f
        setShadowLayer(4f, 1f, 1f, Color.argb(120, 0, 0, 0)) // 影を控えめに
    }

    // 音符マーク用（クッキリ見せる）
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16f, context.resources.displayMetrics) // 少し大きめ
        color = Color.WHITE
        alpha = 255 // 100%の不透明度
        textAlign = Paint.Align.CENTER
        setShadowLayer(6f, 2f, 2f, Color.argb(150, 0, 0, 0)) // 影を強めに
    }

    // アーティスト名用（控えめに調整）
    private val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10f, context.resources.displayMetrics) // 12sp → 10sp
        color = Color.WHITE
        alpha = (255 * 0.6f).toInt() // 60%の不透明度（控えめに）
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.02f
        setShadowLayer(3f, 1f, 1f, Color.argb(100, 0, 0, 0)) // 影を控えめに
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
     */
    fun updateMusic(music: Music?) {
        val shouldAnimate = this.currentMusic == null && music != null && music != Music.EMPTY
        this.currentMusic = music
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

        // タイトル + アーティスト + 余白分を目安に高さ上限を決める
        val desiredHeight = titlePaint.textSize + artistPaint.textSize + 96f
        val maxHeight = desiredHeight.toInt()

        val measuredWidth = measuredWidth
        val measuredHeight = measuredHeight.coerceAtMost(maxHeight)

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    /**
     * シンプルな細いボーダーのみのデザイン
     * - 細い白い枠線（1.5px）
     * - 内側は透明
     * - ミニマルでクリーンな印象
     */
    private fun drawGlassmorphismBackground(canvas: Canvas) {
        val padding = 24f
        val cornerRadius = 16f // 角丸を控えめに

        // 細いボーダーのみ描画
        backgroundRect.set(
            padding,
            padding,
            width.toFloat() - padding,
            height.toFloat() - padding
        )

        // 細い白い枠線（1.5px）
        val borderPaintThin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.WHITE
            alpha = (255 * 0.5f).toInt() // 50%の不透明度で控えめに
        }

        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, borderPaintThin)
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

        // 音符マーク（クッキリ見せる - タイトルの左側に配置）
        val icon = "🎵"
        val iconWidth = iconPaint.measureText(icon)

        // タイトル（音符の右側）
        val titleText = music.title
        val titleWidth = titlePaint.measureText(titleText)

        // 音符とタイトルの合計幅（間隔8dp込み）
        val spacing = 8f
        val totalWidth = iconWidth + spacing + titleWidth

        if (totalWidth > availableWidth) {
            val scale = availableWidth / totalWidth
            canvas.save()
            canvas.scale(scale, scale, centerX, centerY - 20f)
        }

        // 音符を描画（左側）
        val iconX = centerX - totalWidth / 2f + iconWidth / 2f
        canvas.drawText(icon, iconX, centerY - 20f, iconPaint)

        // タイトルを描画（右側）
        val titleX = iconX + iconWidth / 2f + spacing + titleWidth / 2f
        canvas.drawText(titleText, titleX, centerY - 20f, titlePaint)

        if (totalWidth > availableWidth) {
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
    }
}
