package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.tinygc.asachiru.domain.entity.Weather
import com.tinygc.asachiru.domain.entity.WeatherCondition

/**
 * 天気情報を表示するカスタムビュー
 *
 * 天気アイコン、気温、降水確率を表示します。
 * エラー時は赤色でエラーメッセージを表示します。
 * Glassmorphism効果で半透明の背景と境界線を追加しています。
 */
class WeatherView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var weather: Weather? = null
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

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 192f // Android TV (4K)用に2倍に拡大
        color = Color.WHITE
        setShadowLayer(8f, 2f, 2f, Color.argb(180, 0, 0, 0)) // 影を追加（視認性向上）
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 72f // Android TV (4K)用に2倍に拡大
        color = Color.WHITE
        setShadowLayer(6f, 2f, 2f, Color.argb(180, 0, 0, 0)) // 影を追加（視認性向上）
    }

    private val errorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 72f // Android TV (4K)用に2倍に拡大
        color = Color.RED
        setShadowLayer(6f, 2f, 2f, Color.argb(180, 0, 0, 0)) // 影を追加（視認性向上）
    }

    private val dateLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 56f // Android TV (4K)用に2倍に拡大
        color = Color.WHITE
        alpha = (255 * 0.8f).toInt() // 少し薄く表示
        setShadowLayer(5f, 2f, 2f, Color.argb(180, 0, 0, 0)) // 影を追加（視認性向上）
    }

    /**
     * 天気情報を更新
     * @param weather 表示する天気情報（nullの場合は非表示）
     */
    fun updateWeather(weather: Weather?) {
        this.weather = weather
        this.errorMessage = null
        invalidate()
    }

    /**
     * エラーメッセージを表示
     * @param message エラーメッセージ
     */
    fun showError(message: String) {
        this.errorMessage = message
        this.weather = null
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

        weather?.let {
            drawDateLabel(canvas, it)
            drawWeatherIcon(canvas, it)
            drawTemperature(canvas, it)
            drawPrecipitation(canvas, it)
        }
    }

    /**
     * Material Design 3 + Neumorphism背景を描画
     */
    private fun drawGlassmorphismBackground(canvas: Canvas) {
        val padding = 24f
        val cornerRadius = 48f
        val shadowOffset = 8f

        // 外側の影
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

        // 内側のハイライト
        val highlightInset = 4f
        highlightRect.set(
            padding + highlightInset,
            padding + highlightInset,
            width.toFloat() - padding - highlightInset * 8,
            height.toFloat() - padding - highlightInset * 8
        )
        canvas.drawRoundRect(highlightRect, cornerRadius - highlightInset, cornerRadius - highlightInset, highlightPaint)

        // 境界線
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, borderPaint)
    }

    /**
     * エラーメッセージを描画
     */
    private fun drawError(canvas: Canvas) {
        val text = "Error: $errorMessage"
        val textWidth = errorPaint.measureText(text)
        val paddingX = width * 0.1f // Viewの幅の10%位置（左パディング）
        val availableWidth = width - (paddingX * 2) // 左右パディング考慮

        if (textWidth > availableWidth) {
            val scale = availableWidth / textWidth
            canvas.save()
            canvas.scale(scale, scale, paddingX, 0f)
        }

        canvas.drawText(text, paddingX, height * 0.5f, errorPaint)

        if (textWidth > availableWidth) {
            canvas.restore()
        }
    }

    /**
     * 日付ラベル（今日/明日）を描画（左上）
     */
    private fun drawDateLabel(canvas: Canvas, weather: Weather) {
        val paddingX = width * 0.1f // Viewの幅の10%位置（左パディング）
        val y = height * 0.15f // Viewの高さの15%位置
        canvas.drawText(weather.dateLabel, paddingX, y, dateLabelPaint)
    }

    /**
     * 天気アイコンを描画（左側中央）
     * 複数アイコンで長い場合は動的にスケーリング
     */
    private fun drawWeatherIcon(canvas: Canvas, weather: Weather) {
        // iconTextをそのまま使用（例：「☀のち☁」）
        val icon = weather.iconText

        val paddingX = width * 0.1f // Viewの幅の10%位置（左パディング）
        val y = height * 0.5f // Viewの高さの50%位置（中央）

        // アイコンが使用できる最大幅（気温表示エリアとの間に余裕を持たせる）
        // 気温表示は55%位置から始まるので、10%～50%の範囲（40%）を使用可能
        val maxIconWidth = width * 0.40f
        val iconWidth = iconPaint.measureText(icon)

        // アイコンが最大幅を超える場合はスケーリング
        if (iconWidth > maxIconWidth) {
            val scale = maxIconWidth / iconWidth
            canvas.save()
            canvas.scale(scale, scale, paddingX, y)
            canvas.drawText(icon, paddingX, y, iconPaint)
            canvas.restore()
        } else {
            canvas.drawText(icon, paddingX, y, iconPaint)
        }
    }

    /**
     * 気温情報を描画（右側に縦に配置）
     */
    private fun drawTemperature(canvas: Canvas, weather: Weather) {
        textPaint.color = Color.WHITE

        val x = width * 0.55f // Viewの幅の55%位置（中央やや右）
        val baseY = height * 0.3f // Viewの高さの30%位置から開始
        val lineSpacing = height * 0.15f // 行間をViewの高さの15%

        // 気温データがnullの場合は「--」を表示
        val maxTempText = weather.maxTemperature?.let { "${it}°C" } ?: "--"
        val minTempText = weather.minTemperature?.let { "${it}°C" } ?: "--"

        canvas.drawText("現在: ${weather.currentTemperature}°C", x, baseY, textPaint)
        canvas.drawText("最高: $maxTempText", x, baseY + lineSpacing, textPaint)
        canvas.drawText("最低: $minTempText", x, baseY + lineSpacing * 2, textPaint)
    }

    /**
     * 降水確率を描画（右側下部）
     */
    private fun drawPrecipitation(canvas: Canvas, weather: Weather) {
        textPaint.color = Color.CYAN
        val x = width * 0.55f // Viewの幅の55%位置（中央やや右）
        val y = height * 0.8f // Viewの高さの80%位置
        canvas.drawText("降水確率: ${weather.precipitationProbability}%", x, y, textPaint)
    }
}
