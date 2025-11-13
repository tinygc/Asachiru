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

    // Glassmorphism背景用
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = (255 * 0.15f).toInt() // 15%の不透明度
        style = Paint.Style.FILL
    }

    // Glassmorphism境界線用
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = (255 * 0.3f).toInt() // 30%の不透明度
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val backgroundRect = RectF()

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 96f
        color = Color.WHITE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 36f
        color = Color.WHITE
    }

    private val errorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 36f
        color = Color.RED
    }

    private val dateLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        color = Color.WHITE
        alpha = (255 * 0.8f).toInt() // 少し薄く表示
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
        canvas.drawText(
            "Error: $errorMessage",
            50f, 50f, errorPaint
        )
    }

    /**
     * 日付ラベル（今日/明日）を描画（左上）
     */
    private fun drawDateLabel(canvas: Canvas, weather: Weather) {
        canvas.drawText(
            weather.dateLabel,
            40f, 40f, dateLabelPaint
        )
    }

    /**
     * 天気アイコンを描画（左側中央）
     */
    private fun drawWeatherIcon(canvas: Canvas, weather: Weather) {
        val icon = when (weather.condition) {
            WeatherCondition.SUNNY -> "☀"
            WeatherCondition.CLOUDY -> "☁"
            WeatherCondition.RAINY -> "☂"
            WeatherCondition.SNOWY -> "⛄"
            WeatherCondition.OTHER -> "?"
        }

        // 左側の中央に配置
        canvas.drawText(icon, 40f, 120f, iconPaint)
    }

    /**
     * 気温情報を描画（右側に縦に配置）
     */
    private fun drawTemperature(canvas: Canvas, weather: Weather) {
        textPaint.color = Color.WHITE

        // 右側に縦に配置、45px間隔
        canvas.drawText(
            "現在: ${weather.currentTemperature}°C",
            180f, 50f, textPaint
        )
        canvas.drawText(
            "最高: ${weather.maxTemperature}°C",
            180f, 95f, textPaint
        )
        canvas.drawText(
            "最低: ${weather.minTemperature}°C",
            180f, 140f, textPaint
        )
    }

    /**
     * 降水確率を描画（右側下部）
     */
    private fun drawPrecipitation(canvas: Canvas, weather: Weather) {
        textPaint.color = Color.CYAN
        canvas.drawText(
            "降水確率: ${weather.precipitationProbability}%",
            180f, 185f, textPaint
        )
    }
}
