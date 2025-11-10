package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.tinygc.asachiru.domain.entity.Weather
import com.tinygc.asachiru.domain.entity.WeatherCondition

/**
 * 天気情報を表示するカスタムビュー
 *
 * 天気アイコン、気温、降水確率を表示します。
 * エラー時は赤色でエラーメッセージを表示します。
 */
class WeatherView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var weather: Weather? = null
    private var errorMessage: String? = null

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 64f
        color = Color.WHITE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        color = Color.WHITE
    }

    private val errorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        color = Color.RED
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

        if (errorMessage != null) {
            drawError(canvas)
            return
        }

        weather?.let {
            drawWeatherIcon(canvas, it)
            drawTemperature(canvas, it)
            drawPrecipitation(canvas, it)
        }
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
     * 天気アイコンを描画
     */
    private fun drawWeatherIcon(canvas: Canvas, weather: Weather) {
        val icon = when (weather.condition) {
            WeatherCondition.SUNNY -> "☀"
            WeatherCondition.CLOUDY -> "☁"
            WeatherCondition.RAINY -> "☂"
            WeatherCondition.SNOWY -> "⛄"
            WeatherCondition.OTHER -> "?"
        }

        canvas.drawText(icon, 50f, 80f, iconPaint)
    }

    /**
     * 気温情報を描画
     */
    private fun drawTemperature(canvas: Canvas, weather: Weather) {
        textPaint.color = Color.WHITE

        canvas.drawText(
            "現在: ${weather.currentTemperature}°C",
            150f, 50f, textPaint
        )
        canvas.drawText(
            "最高: ${weather.maxTemperature}°C",
            150f, 80f, textPaint
        )
        canvas.drawText(
            "最低: ${weather.minTemperature}°C",
            150f, 110f, textPaint
        )
    }

    /**
     * 降水確率を描画
     */
    private fun drawPrecipitation(canvas: Canvas, weather: Weather) {
        textPaint.color = Color.CYAN
        canvas.drawText(
            "降水確率: ${weather.precipitationProbability}%",
            150f, 140f, textPaint
        )
    }
}
