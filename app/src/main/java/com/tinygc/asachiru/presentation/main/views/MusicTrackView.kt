package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
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

    // トラック名用
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 112f // Android TV (4K)用に2倍に拡大
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        setShadowLayer(8f, 2f, 2f, Color.argb(180, 0, 0, 0)) // 影を追加（視認性向上）
    }

    // アーティスト名用
    private val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 72f // Android TV (4K)用に2倍に拡大
        color = Color.WHITE
        alpha = (255 * 0.8f).toInt() // 80%の不透明度
        textAlign = Paint.Align.CENTER
        setShadowLayer(6f, 2f, 2f, Color.argb(180, 0, 0, 0)) // 影を追加（視認性向上）
    }

    // エラーメッセージ用
    private val errorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 72f // Android TV (4K)用に2倍に拡大
        color = Color.RED
        textAlign = Paint.Align.CENTER
        setShadowLayer(6f, 2f, 2f, Color.argb(180, 0, 0, 0)) // 影を追加（視認性向上）
    }

    /**
     * 音楽情報を更新
     * @param music 表示する音楽情報（nullの場合は非表示）
     */
    fun updateMusic(music: Music?) {
        this.currentMusic = music
        this.errorMessage = null
        invalidate()
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
     * エラーメッセージを描画
     */
    private fun drawError(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f
        val text = "Error: $errorMessage"
        val textWidth = errorPaint.measureText(text)
        val availableWidth = width - 100f // 左右パディング50fずつ

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
     * トラック情報を描画（中央に配置）
     */
    private fun drawTrackInfo(canvas: Canvas, music: Music) {
        val centerX = width / 2f
        val centerY = height / 2f

        // トラック名（中央より少し上）
        val titleText = "🎵 ${music.title}"
        val titleWidth = titlePaint.measureText(titleText)
        val availableWidth = width - 100f

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
    }
}
